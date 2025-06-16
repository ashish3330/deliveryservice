package com.railswad.deliveryservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.railswad.deliveryservice.dto.AddItemRequest;
import com.railswad.deliveryservice.dto.CartDTO;
import com.railswad.deliveryservice.dto.CartItemDTO;
import com.railswad.deliveryservice.dto.CartSummaryDTO;
import com.railswad.deliveryservice.entity.MenuItem;
import com.railswad.deliveryservice.entity.Vendor;
import com.railswad.deliveryservice.exception.ResourceNotFoundException;
import com.railswad.deliveryservice.repository.MenuItemRepository;
import com.railswad.deliveryservice.repository.VendorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartService.class);
    private static final String CART_KEY_PREFIX = "cart:";
    private static final String CART_MAPPING_PREFIX = "cart_mapping:user:";
    private static final Duration CART_TTL = Duration.ofHours(1);
    private static final double DEFAULT_DELIVERY_CHARGE = 50.0;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ObjectMapper objectMapper;

    public CartSummaryDTO addItemToCart(Long customerId, AddItemRequest request) {
        long startTime = System.currentTimeMillis();
        logger.info("Adding item {} for customer ID: {}, vendor ID: {}", request.getItemId(), customerId, request.getVendorId());

        // Validate vendor
        Vendor vendor = vendorRepository.findById(request.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + request.getVendorId()));

        // Get or create cart
        String cartId = getOrCreateCartId(customerId, request.getVendorId(), request);
        CartDTO cart = getCartInternal(cartId);

        // Validate item belongs to the vendor
        MenuItem menuItem = menuItemRepository.findById(request.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with id: " + request.getItemId()));

        // Add or update item
        CartItemDTO cartItemDTO = new CartItemDTO();
        cartItemDTO.setItemId(request.getItemId());
        cartItemDTO.setQuantity(request.getQuantity());
        cartItemDTO.setUnitPrice(menuItem.getBasePrice().doubleValue());
        cartItemDTO.setSpecialInstructions(request.getSpecialInstructions());

        List<CartItemDTO> items = cart.getItems();
        Optional<CartItemDTO> existingItem = items.stream()
                .filter(item -> item.getItemId().equals(request.getItemId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItemDTO item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            item.setSpecialInstructions(request.getSpecialInstructions());
            logger.debug("Updated quantity for item {} in cart {}", request.getItemId(), cartId);
        } else {
            items.add(cartItemDTO);
            logger.debug("Added new item {} to cart {}", request.getItemId(), cartId);
        }

        saveCart(cart);
        CartSummaryDTO summary = getCartSummary(customerId, request.getVendorId());
        logger.info("Item added to cart ID: {}, took {}ms", cartId, System.currentTimeMillis() - startTime);
        return summary;
    }

    public CartSummaryDTO removeItemFromCart(Long customerId, Long vendorId, Long itemId) {
        long startTime = System.currentTimeMillis();
        logger.info("Removing item {} for customer ID: {}, vendor ID: {}", itemId, customerId, vendorId);

        String cartId = getCartId(customerId, vendorId);
        if (cartId == null) {
            throw new ResourceNotFoundException("No cart found for customer ID: " + customerId + " and vendor ID: " + vendorId);
        }

        CartDTO cart = getCartInternal(cartId);
        cart.getItems().removeIf(item -> item.getItemId().equals(itemId));
        saveCart(cart);
        CartSummaryDTO summary = getCartSummary(customerId, vendorId);
        logger.info("Item removed from cart ID: {}, took {}ms", cartId, System.currentTimeMillis() - startTime);
        return summary;
    }

    public CartSummaryDTO getCartSummary(Long customerId, Long vendorId) {
        long startTime = System.currentTimeMillis();
        logger.debug("Generating summary for customer ID: {}, vendor ID: {}", customerId, vendorId);

        String cartId = getCartId(customerId, vendorId);
        if (cartId == null) {
            throw new ResourceNotFoundException("No cart found for customer ID: " + customerId + " and vendor ID: " + vendorId);
        }

        CartDTO cart = getCartInternal(cartId);
        CartSummaryDTO summary = new CartSummaryDTO();
        summary.setCartId(cartId);
        summary.setCustomerId(customerId);
        summary.setVendorId(cart.getVendorId());
        summary.setTrainId(cart.getTrainId());
        summary.setPnrNumber(cart.getPnrNumber());
        summary.setCoachNumber(cart.getCoachNumber());
        summary.setSeatNumber(cart.getSeatNumber());
        summary.setItems(cart.getItems());

        double subtotal = cart.getItems().stream()
                .mapToDouble(item -> item.getUnitPrice() * item.getQuantity())
                .sum();
        double taxAmount = subtotal * paymentService.getGstRate();
        double deliveryCharges = calculateDeliveryCharges(vendorId);
        double finalAmount = subtotal + taxAmount + deliveryCharges;

        summary.setSubtotal(subtotal);
        summary.setTaxAmount(taxAmount);
        summary.setDeliveryCharges(deliveryCharges);
        summary.setFinalAmount(finalAmount);

        logger.info("Cart summary generated for ID: {}, took {}ms", cartId, System.currentTimeMillis() - startTime);
        return summary;
    }

    public CartDTO getCart(Long customerId, Long vendorId) {
        long startTime = System.currentTimeMillis();
        String cartId = getCartId(customerId, vendorId);
        if (cartId == null) {
            throw new ResourceNotFoundException("No cart found for customer ID: " + customerId + " and vendor ID: " + vendorId);
        }
        CartDTO cart = getCartInternal(cartId);
        logger.info("Retrieved cart ID: {}, took {}ms", cartId, System.currentTimeMillis() - startTime);
        return cart;
    }

    public void clearCart(Long customerId, Long vendorId) {
        long startTime = System.currentTimeMillis();
        logger.info("Clearing cart for customer ID: {}, vendor ID: {}", customerId, vendorId);
        String cartId = getCartId(customerId, vendorId);
        if (cartId == null) {
            throw new ResourceNotFoundException("No cart found for customer ID: " + customerId + " and vendor ID: " + vendorId);
        }
        redisTemplate.delete(CART_KEY_PREFIX + cartId);
        redisTemplate.delete(CART_MAPPING_PREFIX + customerId + ":" + vendorId);
        logger.info("Cleared cart ID: {}, took {}ms", cartId, System.currentTimeMillis() - startTime);
    }

    public String getOrCreateCartId(Long customerId, Long vendorId, AddItemRequest request) {
        String mappingKey = CART_MAPPING_PREFIX + customerId + ":" + vendorId;
        String cartId = redisTemplate.opsForValue().get(mappingKey);
        if (cartId != null) {
            return cartId;
        }

        CartDTO cartDTO = new CartDTO();
        cartDTO.setCartId(UUID.randomUUID().toString());
        cartDTO.setCustomerId(customerId);
        cartDTO.setVendorId(vendorId);
        cartDTO.setTrainId(request.getTrainId());
        cartDTO.setPnrNumber(request.getPnrNumber());
        cartDTO.setCoachNumber(request.getCoachNumber());
        cartDTO.setSeatNumber(request.getSeatNumber());
        cartDTO.setDeliveryStationId(request.getDeliveryStationId());
        cartDTO.setDeliveryInstructions(request.getDeliveryInstructions());
        cartDTO.setItems(new ArrayList<>());

        saveCart(cartDTO);
        redisTemplate.opsForValue().set(mappingKey, cartDTO.getCartId(), CART_TTL);
        logger.info("Created new cart ID: {} for customer ID: {}, vendor ID: {}", cartDTO.getCartId(), customerId, vendorId);
        return cartDTO.getCartId();
    }

    public String getCartId(Long customerId, Long vendorId) {
        return redisTemplate.opsForValue().get(CART_MAPPING_PREFIX + customerId + ":" + vendorId);
    }

    CartDTO getCartInternal(String cartId) {
        String cartJson = redisTemplate.opsForValue().get(CART_KEY_PREFIX + cartId);
        if (cartJson == null) {
            logger.warn("Cart not found in Redis with ID: {}", cartId);
            throw new ResourceNotFoundException("Cart not found with ID: " + cartId);
        }
        try {
            return objectMapper.readValue(cartJson, CartDTO.class);
        } catch (Exception e) {
            logger.error("Failed to deserialize cart ID: {}, error: {}", cartId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve cart", e);
        }
    }

    private void saveCart(CartDTO cart) {
        try {
            String cartJson = objectMapper.writeValueAsString(cart);
            redisTemplate.opsForValue().set(CART_KEY_PREFIX + cart.getCartId(), cartJson, CART_TTL);
            logger.debug("Saved cart ID: {} to Redis", cart.getCartId());
        } catch (Exception e) {
            logger.error("Failed to save cart ID: {}, error: {}", cart.getCartId(), e.getMessage(), e);
            throw new RuntimeException("Failed to save cart", e);
        }
    }

    private double calculateDeliveryCharges(Long vendorId) {
        return 0.0;
    }
}