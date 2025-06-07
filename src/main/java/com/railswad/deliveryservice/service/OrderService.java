package com.railswad.deliveryservice.service;

import com.railswad.deliveryservice.dto.OrderDTO;
import com.railswad.deliveryservice.dto.OrderItemDTO;
import com.railswad.deliveryservice.entity.*;
import com.railswad.deliveryservice.entity.MenuItem;
import com.railswad.deliveryservice.exception.ResourceNotFoundException;
import com.railswad.deliveryservice.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
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

    @Transactional
    public OrderDTO createOrder(OrderDTO orderDTO) {
        logger.info("Creating order for customer ID: {}", orderDTO.getCustomerId());
        User customer = userRepository.findById(orderDTO.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + orderDTO.getCustomerId()));
        Vendor vendor = vendorRepository.findById(orderDTO.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + orderDTO.getVendorId()));
        Train train = trainRepository.findById(orderDTO.getTrainId())
                .orElseThrow(() -> new ResourceNotFoundException("Train not found with id: " + orderDTO.getTrainId()));
        Station deliveryStation = stationRepository.findById(orderDTO.getDeliveryStationId())
                .orElseThrow(() -> new ResourceNotFoundException("Station not found with id: " + orderDTO.getDeliveryStationId()));

        Order order = new Order();
        order.setCustomer(customer);
        order.setVendor(vendor);
        order.setTrain(train);
        order.setPnrNumber(orderDTO.getPnrNumber());
        order.setCoachNumber(orderDTO.getCoachNumber());
        order.setSeatNumber(orderDTO.getSeatNumber());
        order.setDeliveryStation(deliveryStation);
        order.setDeliveryTime(orderDTO.getDeliveryTime());
        order.setOrderStatus("PLACED");
        order.setTotalAmount(orderDTO.getTotalAmount());
        order.setDeliveryCharges(orderDTO.getDeliveryCharges());
        order.setTaxAmount(orderDTO.getTaxAmount());
        order.setDiscountAmount(orderDTO.getDiscountAmount());
        order.setFinalAmount(orderDTO.getFinalAmount());
        order.setPaymentStatus(orderDTO.getPaymentStatus());
        order.setPaymentMethod(orderDTO.getPaymentMethod());
        order.setDeliveryInstructions(orderDTO.getDeliveryInstructions());
        order.setCreatedAt(ZonedDateTime.now());
        order.setUpdatedAt(ZonedDateTime.now());

        Order savedOrder = orderRepository.save(order);

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

        OrderTracking tracking = new OrderTracking();
        tracking.setOrder(savedOrder);
        tracking.setStatus("PLACED");
        tracking.setRemarks("Order placed successfully");
        tracking.setCreatedAt(ZonedDateTime.now());
        orderTrackingRepository.save(tracking);

        orderDTO.setOrderId(savedOrder.getOrderId());
        logger.info("Order created successfully with ID: {}", savedOrder.getOrderId());
        return orderDTO;
    }

    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, String status, String remarks, Long updatedById) {
        logger.info("Updating status for order ID: {} to {}", orderId, status);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        User updatedBy = userRepository.findById(updatedById)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + updatedById));

        order.setOrderStatus(status);
        order.setUpdatedAt(ZonedDateTime.now());
        Order updatedOrder = orderRepository.save(order);

        OrderTracking tracking = new OrderTracking();
        tracking.setOrder(updatedOrder);
        tracking.setStatus(status);
        tracking.setRemarks(remarks);
        tracking.setCreatedAt(ZonedDateTime.now());
        tracking.setUpdatedBy(updatedBy);
        orderTrackingRepository.save(tracking);

        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderId(updatedOrder.getOrderId());
        orderDTO.setCustomerId(updatedOrder.getCustomer().getUserId());
        orderDTO.setVendorId(updatedOrder.getVendor().getVendorId());
        orderDTO.setTrainId(updatedOrder.getTrain().getTrainId());
        orderDTO.setPnrNumber(updatedOrder.getPnrNumber());
        orderDTO.setCoachNumber(updatedOrder.getCoachNumber());
        orderDTO.setSeatNumber(updatedOrder.getSeatNumber());
        orderDTO.setDeliveryStationId(updatedOrder.getDeliveryStation().getStationId());
        orderDTO.setDeliveryTime(updatedOrder.getDeliveryTime());
        orderDTO.setOrderStatus(updatedOrder.getOrderStatus());
        orderDTO.setTotalAmount(updatedOrder.getTotalAmount());
        orderDTO.setDeliveryCharges(updatedOrder.getDeliveryCharges());
        orderDTO.setTaxAmount(updatedOrder.getTaxAmount());
        orderDTO.setDiscountAmount(updatedOrder.getDiscountAmount());
        orderDTO.setFinalAmount(updatedOrder.getFinalAmount());
        orderDTO.setPaymentStatus(updatedOrder.getPaymentStatus());
        orderDTO.setPaymentMethod(updatedOrder.getPaymentMethod());
        orderDTO.setDeliveryInstructions(updatedOrder.getDeliveryInstructions());

        List<OrderItemDTO> itemDTOs = orderItemRepository.findByOrder(updatedOrder).stream().map(item -> {
            OrderItemDTO itemDTO = new OrderItemDTO();
            if (item.getItem() != null) {
                itemDTO.setItemId(item.getItem().getItemId());
            } else {
                logger.warn("OrderItem with ID {} has no associated MenuItem", item.getOrderItemId());
                throw new ResourceNotFoundException("Menu item not found for order item ID: " + item.getOrderItemId());
            }
            itemDTO.setQuantity(item.getQuantity());
            itemDTO.setUnitPrice(item.getUnitPrice());
            itemDTO.setSpecialInstructions(item.getSpecialInstructions());
            return itemDTO;
        }).collect(Collectors.toList());
        orderDTO.setItems(itemDTOs);

        logger.info("Order status updated successfully for order ID: {}", orderId);
        return orderDTO;
    }

    public OrderDTO getOrderById(Long orderId) {
        logger.info("Fetching order with ID: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderId(order.getOrderId());
        orderDTO.setCustomerId(order.getCustomer().getUserId());
        orderDTO.setVendorId(order.getVendor().getVendorId());
        orderDTO.setTrainId(order.getTrain().getTrainId());
        orderDTO.setPnrNumber(order.getPnrNumber());
        orderDTO.setCoachNumber(order.getCoachNumber());
        orderDTO.setSeatNumber(order.getSeatNumber());
        orderDTO.setDeliveryStationId(order.getDeliveryStation().getStationId());
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
                logger.warn("OrderItem with ID {} has no associated MenuItem", item.getOrderItemId());
                throw new ResourceNotFoundException("Menu item not found for order item ID: " + item.getOrderItemId());
            }
            itemDTO.setQuantity(item.getQuantity());
            itemDTO.setUnitPrice(item.getUnitPrice());
            itemDTO.setSpecialInstructions(item.getSpecialInstructions());
            return itemDTO;
        }).collect(Collectors.toList());
        orderDTO.setItems(itemDTOs);

        logger.info("Order fetched successfully with ID: {}", orderId);
        return orderDTO;
    }

    public Page<OrderDTO> getAllOrders(Pageable pageable) {
        logger.info("Fetching all orders with pageable: {}", pageable);
        return orderRepository.findAll(pageable).map(order -> {
            OrderDTO orderDTO = new OrderDTO();
            orderDTO.setOrderId(order.getOrderId());
            orderDTO.setCustomerId(order.getCustomer().getUserId());
            orderDTO.setVendorId(order.getVendor().getVendorId());
            orderDTO.setTrainId(order.getTrain().getTrainId());
            orderDTO.setPnrNumber(order.getPnrNumber());
            orderDTO.setCoachNumber(order.getCoachNumber());
            orderDTO.setSeatNumber(order.getSeatNumber());
            orderDTO.setDeliveryStationId(order.getDeliveryStation().getStationId());
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
                    logger.warn("OrderItem with ID {} has no associated MenuItem", item.getOrderItemId());
                    throw new ResourceNotFoundException("Menu item not found for order item ID: " + item.getOrderItemId());
                }
                itemDTO.setQuantity(item.getQuantity());
                itemDTO.setUnitPrice(item.getUnitPrice());
                itemDTO.setSpecialInstructions(item.getSpecialInstructions());
                return itemDTO;
            }).collect(Collectors.toList());
            orderDTO.setItems(itemDTOs);

            return orderDTO;
        });
    }
}