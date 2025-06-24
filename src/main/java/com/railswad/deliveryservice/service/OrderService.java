package com.railswad.deliveryservice.service;

import com.railswad.deliveryservice.dto.*;
import com.railswad.deliveryservice.entity.*;
import com.railswad.deliveryservice.exception.ResourceNotFoundException;
import com.railswad.deliveryservice.repository.*;
import com.razorpay.RazorpayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderTrackingRepository orderTrackingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private TrainRepository trainRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private CartService cartService;

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "orders", key = "#result.orderId", condition = "#result != null")
    })
    public OrderDTO createOrderFromCart(String cartId, String paymentMethod, ZonedDateTime deliveryTime, CreateOrderRequest request) {
        long startTime = System.currentTimeMillis();
        logger.info("Starting order creation from cart ID: {}", cartId);

        try {
            // Use internal method to get cart by cartId
            CartDTO cart = cartService.getCartInternal(cartId);
            logger.debug("Retrieved cart with ID: {} for customer ID: {}", cartId, cart.getCustomerId());

            if (cart.getItems() == null || cart.getItems().isEmpty()) {
                logger.warn("Cart ID: {} is empty", cartId);
                throw new IllegalArgumentException("Cannot create order from empty cart");
            }

            User customer = userRepository.findById(cart.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + cart.getCustomerId()));
            Vendor vendor = vendorRepository.findById(cart.getVendorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + cart.getVendorId()));
            Station deliveryStation = stationRepository.findById(Math.toIntExact(cart.getDeliveryStationId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Station not found with id: " + cart.getDeliveryStationId()));

                logger.debug("Valid entities retrieved for order creation: customer ID {}, vendor ID {}, train ID {}, station ID {}",
                    cart.getCustomerId(), cart.getVendorId(), cart.getTrainId(), cart.getDeliveryStationId());

            Order order = new Order();
            order.setCustomer(customer);
            order.setVendor(vendor);


            order.setOrderStatus(OrderStatus.PLACED);
            order.setPaymentMethod(paymentMethod);
            order.setDeliveryInstructions(cart.getDeliveryInstructions());

            // Use customerId and vendorId to get cart summary
            CartSummaryDTO summary = cartService.getCartSummary(cart.getCustomerId(), cart.getVendorId());
            order.setTotalAmount(summary.getSubtotal());
            order.setTaxAmount(summary.getTaxAmount());
            order.setCoachNumber(request.getCoachNumber());
            order.setSeatNumber(request.getSeatNumber());
            order.setDeliveryStation(deliveryStation);
            order.setDeliveryTime(deliveryTime);
            order.setTrainId(request.getTrainId());
            order.setPnrNumber(request.getPnrNumber());
            order.setDeliveryCharges(summary.getDeliveryCharges());
            order.setFinalAmount(summary.getFinalAmount());
            order.setPaymentStatus("COD".equals(paymentMethod) ? PaymentStatus.PENDING : PaymentStatus.PROCESSING);

            order.setCreatedAt(ZonedDateTime.now());
            order.setUpdatedAt(ZonedDateTime.now());

            Order savedOrder = orderRepository.save(order);
            logger.debug("Order saved with ID: {}", savedOrder.getOrderId());

            List<OrderItem> orderItems = cart.getItems().stream().map(itemDTO -> {
                MenuItem menuItem = menuItemRepository.findById(itemDTO.getItemId())
                        .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with id: " + itemDTO.getItemId()));
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(savedOrder);
                orderItem.setItem(menuItem);
                orderItem.setQuantity(itemDTO.getQuantity());
                orderItem.setUnitPrice(itemDTO.getUnitPrice());
                orderItem.setSpecialInstructions(itemDTO.getSpecialInstructions());
                return orderItem;
            }).collect(Collectors.toList());

            orderItemRepository.saveAll(orderItems);
            logger.debug("Saved {} order items for order ID: {}", orderItems.size(), savedOrder.getOrderId());

            updateOrderTracking(savedOrder, OrderStatus.PLACED, "Order placed successfully", null);

            // Initiate Razorpay payment if not COD
            String razorpayOrderId = null;
            if (!"COD".equals(paymentMethod)) {
                try {
                    razorpayOrderId = paymentService.createRazorpayOrder(savedOrder.getOrderId());
                    logger.info("Created Razorpay order ID: {} for order {}", razorpayOrderId, savedOrder.getOrderId());
                } catch (RazorpayException e) {
                    logger.error("Razorpay payment initiation failed for order ID: {}, error: {}", savedOrder.getOrderId(), e.getMessage());
                    savedOrder.setPaymentStatus(PaymentStatus.FAILED);
                    orderRepository.save(savedOrder);
                    throw new RuntimeException("Payment initiation failed", e);
                }
            }

            cartService.clearCart(cart.getCustomerId(), cart.getVendorId()); // Clear cart after successful order creation
            OrderDTO orderDTO = convertToOrderDTO(savedOrder);
            orderDTO.setRazorpayOrderID(razorpayOrderId);
            logger.info("Order created successfully with ID: {}, took {}ms",
                    savedOrder.getOrderId(), System.currentTimeMillis() - startTime);
            return orderDTO;
        } catch (Exception e) {
            logger.error("Failed to create order from cart ID: {}, error: {}", cartId, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    @CacheEvict(value = "orders", key = "#orderId")
    public void markCodPaymentCompleted(Long orderId, Long updatedById, String remarks) {
        logger.info("Processing COD payment completion for order ID: {}, updated by user ID: {}", orderId, updatedById);
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
            User updatedBy = userRepository.findById(updatedById)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + updatedById));

            if (!"COD".equals(order.getPaymentMethod())) {
                logger.warn("Attempted COD payment completion for non-COD order ID: {}", orderId);
                throw new IllegalArgumentException("Order is not a COD order");
            }
            if (order.getPaymentStatus() == PaymentStatus.COMPLETED) {
                logger.warn("COD payment already completed for order ID: {}", orderId);
                throw new IllegalArgumentException("Payment is already completed");
            }

            order.setPaymentStatus(PaymentStatus.COMPLETED);
            order.setUpdatedAt(ZonedDateTime.now());
            orderRepository.save(order);
            logger.debug("Updated order ID: {} with payment status: COMPLETED", orderId);

            paymentService.generateAndSaveInvoice(order, "COD_" + orderId);
            logger.debug("Generated invoice for order ID: {}", orderId);

            updateOrderTracking(order, order.getOrderStatus(), "COD payment completed: " + remarks, updatedBy);
            logger.info("COD payment completed successfully for order ID: {}", orderId);
        } catch (Exception e) {
            logger.error("Failed to complete COD payment for order ID: {}, error: {}", orderId, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    @CacheEvict(value = "orders", key = "#orderId")
    public OrderDTO updateOrderStatus(Long orderId, OrderStatus status, String remarks, Long updatedById) {
        long startTime = System.currentTimeMillis();
        logger.info("Updating order status for order ID: {} to {}, updated by user ID: {}", orderId, status, updatedById);
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
            User updatedBy = userRepository.findById(updatedById)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + updatedById));

            order.setOrderStatus(status);
            order.setUpdatedAt(ZonedDateTime.now());
            Order updatedOrder = orderRepository.save(order);
            logger.debug("Updated order ID: {} with status: {}", orderId, status);

            updateOrderTracking(updatedOrder, status, remarks, updatedBy);

            OrderDTO orderDTO = convertToOrderDTO(updatedOrder);
            logger.info("Order status updated successfully for order ID: {}, took {}ms",
                    orderId, System.currentTimeMillis() - startTime);
            return orderDTO;
        } catch (Exception e) {
            logger.error("Failed to update order status for order ID: {}, error: {}", orderId, e.getMessage(), e);
            throw e;
        }
    }

    private void updateOrderTracking(Order order, OrderStatus status, String remarks, User updatedBy) {
        logger.debug("Updating tracking for order ID: {}, status: {}", order.getOrderId(), status);
        try {
            OrderTracking latestTracking = orderTrackingRepository.findTopByOrderOrderByCreatedAtDesc(order);
            if (latestTracking != null && status.equals(latestTracking.getStatus())) {
                latestTracking.setRemarks(remarks);
                latestTracking.setCreatedAt(ZonedDateTime.now());
                latestTracking.setUpdatedBy(updatedBy);
                orderTrackingRepository.save(latestTracking);
                logger.debug("Updated existing tracking entry for order ID: {}, status: {}", order.getOrderId(), status);
            } else {
                OrderTracking tracking = new OrderTracking();
                tracking.setOrder(order);
                tracking.setStatus(status);
                tracking.setRemarks(remarks);
                tracking.setCreatedAt(ZonedDateTime.now());
                tracking.setUpdatedBy(updatedBy);
                orderTrackingRepository.save(tracking);
                logger.debug("Created new tracking entry for order ID: {}, status: {}", order.getOrderId(), status);
            }
        } catch (Exception e) {
            logger.error("Failed to update tracking for order ID: {}, error: {}", order.getOrderId(), e.getMessage(), e);
            throw e;
        }
    }

    @Cacheable(value = "orders", key = "#orderId")
    public OrderDTO getOrderById(Long orderId) {
        logger.info("Fetching order with ID: {}", orderId);
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
            OrderDTO orderDTO = convertToOrderDTO(order);
            logger.info("Successfully fetched order with ID: {}", orderId);
            return orderDTO;
        } catch (Exception e) {
            logger.error("Failed to fetch order with ID: {}, error: {}", orderId, e.getMessage(), e);
            throw e;
        }
    }

    public Page<OrderDTO> getAllOrders(OrderFilterDTO filter, Pageable pageable) {
        logger.info("Fetching orders with filters: stationId={}, isVegetarianMenuItem={}, isVegetarianVendor={}, pageable: page={}, size={}, sort={}",
                filter.getStationId(), filter.getIsVegetarianMenuItem(), filter.getIsVegetarianVendor(),
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        try {
            Specification<Order> spec = OrderSpecification.withFilters(filter);
            Page<OrderDTO> orders = orderRepository.findAll(spec, pageable).map(this::convertToOrderDTO);
            logger.info("Successfully fetched {} orders matching filters", orders.getTotalElements());
            return orders;
        } catch (Exception e) {
            logger.error("Failed to fetch orders with filters, error: {}", e.getMessage(), e);
            throw e;
        }
    }
    public Page<OrderDTO> getActiveOrdersForUser(Long userId, Pageable pageable) {
        logger.info("Fetching active orders for user ID: {}, pageable: page={}, size={}, sort={}",
                userId, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        try {
            userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
            Specification<Order> spec = (root, query, cb) -> cb.and(
                    cb.equal(root.get("customer").get("userId"), userId),
                    cb.not(root.get("orderStatus").in(Arrays.asList(OrderStatus.DELIVERED, OrderStatus.CANCELLED)))
            );
            Page<OrderDTO> orders = orderRepository.findAll(spec, pageable).map(this::convertToOrderDTO);
            logger.info("Successfully fetched {} active orders for user ID: {}", orders.getTotalElements(), userId);
            return orders;
        } catch (Exception e) {
            logger.error("Failed to fetch active orders for user ID: {}, error: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    public Page<OrderDTO> getHistoricalOrdersForUser(Long userId, Pageable pageable) {
        logger.info("Fetching historical orders for user ID: {}, pageable: page={}, size={}, sort={}",
                userId, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        try {
            userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
            Specification<Order> spec = (root, query, cb) -> cb.and(
                    cb.equal(root.get("customer").get("userId"), userId),
                    root.get("orderStatus").in(Arrays.asList(OrderStatus.DELIVERED, OrderStatus.CANCELLED))
            );
            Page<OrderDTO> orders = orderRepository.findAll(spec, pageable).map(this::convertToOrderDTO);
            logger.info("Successfully fetched {} historical orders for user ID: {}", orders.getTotalElements(), userId);
            return orders;
        } catch (Exception e) {
            logger.error("Failed to fetch historical orders for user ID: {}, error: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    public Page<OrderDTO> getActiveOrdersForVendor(Long userId, Pageable pageable) {
        logger.info("Fetching active orders for user ID: {}, pageable: page={}, size={}, sort={}",
                userId, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        try {
            // Fetch the Vendor entity by userId
            Vendor vendor = vendorRepository.findByUserUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor not found for user ID: " + userId));
            Long vendorId = vendor.getVendorId();

            // Check if vendor exists
            vendorRepository.findById(vendorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + vendorId));

            // Specification to filter active orders for the vendor
            Specification<Order> spec = (root, query, cb) -> cb.and(
                    cb.equal(root.get("vendor").get("vendorId"), vendorId),
                    cb.not(root.get("orderStatus").in(Arrays.asList(OrderStatus.DELIVERED, OrderStatus.CANCELLED)))
            );

            // Fetch and convert orders to DTOs
            Page<OrderDTO> orders = orderRepository.findAll(spec, pageable).map(this::convertToOrderDTO);
            logger.info("Successfully fetched {} active orders for vendor ID: {}", orders.getTotalElements(), vendorId);
            return orders;
        } catch (Exception e) {
            logger.error("Failed to fetch active orders for user ID: {}, error: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    public Page<OrderDTO> getHistoricalOrdersForVendor(Long userId, Pageable pageable) {
        logger.info("Fetching historical orders for user ID: {}, pageable: page={}, size={}, sort={}",
                userId, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        try {
            // Fetch the Vendor entity by userId
            Vendor vendor = vendorRepository.findByUserUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor not found for user ID: " + userId));
            Long vendorId = vendor.getVendorId();

            // Check if vendor exists
            vendorRepository.findById(vendorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + vendorId));

            // Specification to filter historical orders for the vendor
            Specification<Order> spec = (root, query, cb) -> cb.and(
                    cb.equal(root.get("vendor").get("vendorId"), vendorId),
                    root.get("orderStatus").in(Arrays.asList(OrderStatus.DELIVERED, OrderStatus.CANCELLED))
            );

            // Fetch and convert orders to DTOs
            Page<OrderDTO> orders = orderRepository.findAll(spec, pageable).map(this::convertToOrderDTO);
            logger.info("Successfully fetched {} historical orders for vendor ID: {}", orders.getTotalElements(), vendorId);
            return orders;
        } catch (Exception e) {
            logger.error("Failed to fetch historical orders for user ID: {}, error: {}", userId, e.getMessage(), e);
            throw e;
        }
    }


    @Transactional
    @CacheEvict(value = "orders", key = "#orderId")
    public OrderDTO adminUpdateOrderStatus(Long orderId, OrderStatus status, String remarks, Long adminId) {
        long startTime = System.currentTimeMillis();
        logger.info("Admin updating order status for order ID: {} to {}, updated by admin ID: {}", orderId, status, adminId);
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
            User admin = userRepository.findById(adminId)
                    .orElseThrow(() -> new ResourceNotFoundException("Admin not found with id: " + adminId));

            // Validate admin role (assuming User entity has a role field)
//            if (!"ADMIN".equals(admin.getRole())) {
//                logger.warn("User ID: {} is not an admin, cannot update order status", adminId);
//                throw new IllegalArgumentException("Only admins can update order status");
//            }

            order.setOrderStatus(status);
            order.setUpdatedAt(ZonedDateTime.now());
            Order updatedOrder = orderRepository.save(order);
            logger.debug("Admin updated order ID: {} with status: {}", orderId, status);

            updateOrderTracking(updatedOrder, status, "Admin updated status: " + remarks, admin);

            OrderDTO orderDTO = convertToOrderDTO(updatedOrder);
            logger.info("Admin successfully updated order status for order ID: {}, took {}ms",
                    orderId, System.currentTimeMillis() - startTime);
            return orderDTO;
        } catch (Exception e) {
            logger.error("Admin failed to update order status for order ID: {}, error: {}", orderId, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    @CacheEvict(value = "orders", key = "#orderId")
    public OrderDTO adminUpdateCodPaymentStatus(Long orderId, PaymentStatus paymentStatus, String remarks, Long adminId) {
        long startTime = System.currentTimeMillis();
        logger.info("Admin updating COD payment status for order ID: {} to {}, updated by admin ID: {}", orderId, paymentStatus, adminId);
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
            User admin = userRepository.findById(adminId)
                    .orElseThrow(() -> new ResourceNotFoundException("Admin not found with id: " + adminId));

            // Validate admin role
            if (!"ADMIN".equals(admin.getUserRoles())) {
                logger.warn("User ID: {} is not an admin, cannot update payment status", adminId);
                throw new IllegalArgumentException("Only admins can update payment status");
            }

            // Validate that order is COD
            if (!"COD".equals(order.getPaymentMethod())) {
                logger.warn("Attempted COD payment status update for non-COD order ID: {}", orderId);
                throw new IllegalArgumentException("Order is not a COD order");
            }

            // Prevent redundant status update
            if (order.getPaymentStatus() == paymentStatus) {
                logger.warn("Payment status {} already set for order ID: {}", paymentStatus, orderId);
                throw new IllegalArgumentException("Payment status is already " + paymentStatus);
            }

            order.setPaymentStatus(paymentStatus);
            order.setUpdatedAt(ZonedDateTime.now());
            Order updatedOrder = orderRepository.save(order);
            logger.debug("Admin updated payment status for order ID: {} to {}", orderId, paymentStatus);

            // Generate invoice if payment is completed
            if (paymentStatus == PaymentStatus.COMPLETED) {
                paymentService.generateAndSaveInvoice(updatedOrder, "COD_" + orderId);
                logger.debug("Generated invoice for order ID: {}", orderId);
            }

            updateOrderTracking(updatedOrder, updatedOrder.getOrderStatus(), "Admin updated COD payment status: " + remarks, admin);

            OrderDTO orderDTO = convertToOrderDTO(updatedOrder);
            logger.info("Admin successfully updated COD payment status for order ID: {}, took {}ms",
                    orderId, System.currentTimeMillis() - startTime);
            return orderDTO;
        } catch (Exception e) {
            logger.error("Admin failed to update COD payment status for order ID: {}, error: {}", orderId, e.getMessage(), e);
            throw e;
        }
    }

    private OrderDTO convertToOrderDTO(Order order) {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderId(order.getOrderId());
        orderDTO.setCustomerId(order.getCustomer().getUserId());
        orderDTO.setVendorId(order.getVendor().getVendorId());
        orderDTO.setTrainId(order.getTrainId());
        orderDTO.setPnrNumber(order.getPnrNumber());
        orderDTO.setCoachNumber(order.getCoachNumber());
        orderDTO.setSeatNumber(order.getSeatNumber());
        orderDTO.setDeliveryStationId(Long.valueOf(order.getDeliveryStation().getStationId()));
        orderDTO.setDeliveryTime(order.getDeliveryTime());
        orderDTO.setOrderStatus(order.getOrderStatus());
        orderDTO.setTotalAmount(order.getTotalAmount());
        orderDTO.setDeliveryCharges(order.getDeliveryCharges());
        orderDTO.setTaxAmount(order.getTaxAmount());
        orderDTO.setDiscountAmount(order.getDiscountAmount());
        orderDTO.setFinalAmount(order.getFinalAmount());
        orderDTO.setPaymentStatus(order.getPaymentStatus());
        orderDTO.setPaymentMethod(order.getPaymentMethod());
        orderDTO.setDeliveryInstructions(order.getDeliveryInstructions());

        List<OrderItemDTO> itemDTOs = orderItemRepository.findByOrder(order).stream().map(item -> {
            OrderItemDTO itemDTO = new OrderItemDTO();
            if (item.getItem() != null) {
                itemDTO.setItemId(item.getItem().getItemId());
                itemDTO.setQuantity(item.getQuantity());
                itemDTO.setUnitPrice(item.getUnitPrice());
                itemDTO.setSpecialInstructions(item.getSpecialInstructions());
            } else {
                logger.warn("OrderItem with ID {} has no associated MenuItem for order ID: {}",
                        item.getOrderItemId(), order.getOrderId());
                throw new ResourceNotFoundException("Menu item not found for order item ID: " + item.getOrderItemId());
            }
            return itemDTO;
        }).collect(Collectors.toList());
        orderDTO.setItems(itemDTOs);

        return orderDTO;
    }

    public Page<OrderDTO> getActiveOrdersForAdmin(Long vendorId, ZonedDateTime startDate, ZonedDateTime endDate,
                                                  List<OrderStatus> statuses, Pageable pageable) {
        logger.info("Fetching active orders for admin with filters: vendorId={}, startDate={}, endDate={}, statuses={}",
                vendorId, startDate, endDate, statuses);
        try {
            Specification<Order> spec = createOrderSpecification(vendorId, startDate, endDate, statuses, true);
            Page<OrderDTO> orders = orderRepository.findAll(spec, pageable).map(this::convertToOrderDTO);
            logger.info("Successfully fetched {} active orders", orders.getTotalElements());
            return orders;
        } catch (Exception e) {
            logger.error("Failed to fetch active orders: {}", e.getMessage(), e);
            throw e;
        }
    }

    public Page<OrderDTO> getHistoricalOrdersForAdmin(Long vendorId, ZonedDateTime startDate, ZonedDateTime endDate,
                                                      List<OrderStatus> statuses, Pageable pageable) {
        logger.info("Fetching historical orders for admin with filters: vendorId={}, startDate={}, endDate={}, statuses={}",
                vendorId, startDate, endDate, statuses);
        try {
            Specification<Order> spec = createOrderSpecification(vendorId, startDate, endDate, statuses, false);
            Page<OrderDTO> orders = orderRepository.findAll(spec, pageable).map(this::convertToOrderDTO);
            logger.info("Successfully fetched {} historical orders", orders.getTotalElements());
            return orders;
        } catch (Exception e) {
            logger.error("Failed to fetch historical orders: {}", e.getMessage(), e);
            throw e;
        }
    }

    private Specification<Order> createOrderSpecification(Long vendorId, ZonedDateTime startDate, ZonedDateTime endDate,
                                                          List<OrderStatus> statuses, boolean isActive) {
        return (root, query, cb) -> {
            Specification<Order> spec = Specification.where(null);

            // Filter by vendorId if provided
            if (vendorId != null) {
                spec = spec.and((r, q, c) -> c.equal(r.get("vendor").get("vendorId"), vendorId));
            }

            // Filter by date range if provided
            if (startDate != null) {
                spec = spec.and((r, q, c) -> c.greaterThanOrEqualTo(r.get("createdAt"), startDate));
            }
            if (endDate != null) {
                spec = spec.and((r, q, c) -> c.lessThanOrEqualTo(r.get("createdAt"), endDate));
            }

            // Filter by status: active (not DELIVERED or CANCELLED) or historical (DELIVERED or CANCELLED)
            if (statuses != null && !statuses.isEmpty()) {
                spec = spec.and((r, q, c) -> r.get("orderStatus").in(statuses));
            } else {
                // Default status filter based on isActive flag
                List<OrderStatus> defaultStatuses = isActive
                        ? Arrays.asList(OrderStatus.PLACED, OrderStatus.PREPARING, OrderStatus.DISPATCHED)
                        : Arrays.asList(OrderStatus.DELIVERED, OrderStatus.CANCELLED);
                spec = spec.and((r, q, c) -> r.get("orderStatus").in(defaultStatuses));
            }

            return spec.toPredicate(root, query, cb);
        };
    }
}