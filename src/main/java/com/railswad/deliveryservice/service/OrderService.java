package com.railswad.deliveryservice.service;

import com.railswad.deliveryservice.dto.OrderDTO;
import com.railswad.deliveryservice.dto.OrderFilterDTO;
import com.railswad.deliveryservice.dto.OrderItemDTO;
import com.railswad.deliveryservice.dto.OrderSpecification;
import com.railswad.deliveryservice.entity.*;
import com.railswad.deliveryservice.exception.ResourceNotFoundException;
import com.railswad.deliveryservice.repository.*;
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

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "orders", key = "#result.orderId", condition = "#result != null")
    })
    public OrderDTO createOrder(OrderDTO orderDTO) {
        long startTime = System.currentTimeMillis();
        logger.info("Starting order creation for customer ID: {}, vendor ID: {}, train ID: {}",
                orderDTO.getCustomerId(), orderDTO.getVendorId(), orderDTO.getTrainId());

        try {
            User customer = userRepository.findById(orderDTO.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + orderDTO.getCustomerId()));
            Vendor vendor = vendorRepository.findById(orderDTO.getVendorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + orderDTO.getVendorId()));
            Train train = trainRepository.findById(Math.toIntExact(orderDTO.getTrainId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Train not found with id: " + orderDTO.getTrainId()));
            Station deliveryStation = stationRepository.findById(Math.toIntExact(orderDTO.getDeliveryStationId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Station not found with id: " + orderDTO.getDeliveryStationId()));

            logger.debug("Valid entities retrieved for order creation: customer ID {}, vendor ID {}, train ID {}, station ID {}",
                    orderDTO.getCustomerId(), orderDTO.getVendorId(), orderDTO.getTrainId(), orderDTO.getDeliveryStationId());

            Order order = new Order();
            order.setCustomer(customer);
            order.setVendor(vendor);
            order.setTrain(train);
            order.setPnrNumber(orderDTO.getPnrNumber());
            order.setCoachNumber(orderDTO.getCoachNumber());
            order.setSeatNumber(orderDTO.getSeatNumber());
            order.setDeliveryStation(deliveryStation);
            order.setDeliveryTime(orderDTO.getDeliveryTime());
            order.setOrderStatus(OrderStatus.PLACED);
            order.setPaymentMethod(orderDTO.getPaymentMethod());

            if ("COD".equals(orderDTO.getPaymentMethod())) {
                double subtotal = orderDTO.getItems().stream()
                        .mapToDouble(item -> item.getUnitPrice() * item.getQuantity())
                        .sum();
                double gstAmount = subtotal * paymentService.getGstRate();
                double deliveryCharges = orderDTO.getDeliveryCharges() != null ? orderDTO.getDeliveryCharges() : 0.0;
                double finalAmount = subtotal + gstAmount + deliveryCharges;

                order.setTotalAmount(subtotal);
                order.setTaxAmount(gstAmount);
                order.setDeliveryCharges(deliveryCharges);
                order.setFinalAmount(finalAmount);
                order.setPaymentStatus(PaymentStatus.PENDING);
                logger.debug("COD order amounts calculated: subtotal={}, gst={}, delivery={}, final={}",
                        subtotal, gstAmount, deliveryCharges, finalAmount);
            } else {
                order.setTotalAmount(orderDTO.getTotalAmount());
                order.setTaxAmount(orderDTO.getTaxAmount());
                order.setDeliveryCharges(orderDTO.getDeliveryCharges());
                order.setFinalAmount(orderDTO.getFinalAmount());
                order.setPaymentStatus(orderDTO.getPaymentStatus());
                logger.debug("Non-COD order amounts set: final amount={}", orderDTO.getFinalAmount());
            }

            order.setDeliveryInstructions(orderDTO.getDeliveryInstructions());
            order.setCreatedAt(ZonedDateTime.now());
            order.setUpdatedAt(ZonedDateTime.now());

            Order savedOrder = orderRepository.save(order);
            logger.debug("Order saved with ID: {}", savedOrder.getOrderId());

            List<OrderItem> orderItems = orderDTO.getItems().stream().map(itemDTO -> {
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

            orderDTO.setOrderId(savedOrder.getOrderId());
            logger.info("Order created successfully with ID: {}, took {}ms",
                    savedOrder.getOrderId(), System.currentTimeMillis() - startTime);
            return orderDTO;
        } catch (Exception e) {
            logger.error("Failed to create order for customer ID: {}, error: {}", orderDTO.getCustomerId(), e.getMessage(), e);
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

    private OrderDTO convertToOrderDTO(Order order) {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderId(order.getOrderId());
        orderDTO.setCustomerId(order.getCustomer().getUserId());
        orderDTO.setVendorId(order.getVendor().getVendorId());
        orderDTO.setTrainId(Long.valueOf(order.getTrain().getTrainId()));
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
            } else {
                logger.warn("OrderItem with ID {} has no associated MenuItem for order ID: {}",
                        item.getOrderItemId(), order.getOrderId());
                throw new ResourceNotFoundException("Menu item not found for order item ID: " + item.getOrderItemId());
            }
            itemDTO.setQuantity(item.getQuantity());
            itemDTO.setUnitPrice(item.getUnitPrice());
            itemDTO.setSpecialInstructions(item.getSpecialInstructions());
            return itemDTO;
        }).collect(Collectors.toList());
        orderDTO.setItems(itemDTOs);

        return orderDTO;
    }
}

