package com.railswad.deliveryservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.railswad.deliveryservice.dto.CartDTO;
import com.railswad.deliveryservice.dto.CartItemDTO;
import com.railswad.deliveryservice.dto.CartSummaryDTO;
import com.railswad.deliveryservice.entity.MenuItem;
import com.railswad.deliveryservice.exception.ResourceNotFoundException;
import com.railswad.deliveryservice.repository.MenuItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
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
    private static final Duration CART_TTL = Duration.ofHours(1);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ObjectMapper objectMapper;

    private Long getCustomerIdFromJwt() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Jwt) {
            Jwt jwt = (Jwt) principal;
            return Long.valueOf(jwt.getClaimAsString("userId")); // Adjust claim name as per your JWT
        }
        throw new IllegalStateException("Invalid JWT token or user not authenticated");
    }

    public CartDTO createCart(CartDTO cartDTO) {
        long startTime = System.currentTimeMillis();
        String cartId = UUID.randomUUID().toString();
        Long customerId = getCustomerIdFromJwt();

        cartDTO.setCartId(cartId);
        cartDTO.setCustomerId(customerId);
        if (cartDTO.getItems() == null) {
            cartDTO.setItems(new ArrayList<>());
        }

        try {
            String cartJson = objectMapper.writeValueAsString(cartDTO);
            redisTemplate.opsForValue().set(CART_KEY_PREFIX + cartId, cartJson, CART_TTL);
            logger.info("Created cart with ID: {} for customer ID: {}, took {}ms",
                    cartId, customerId, System.currentTimeMillis() - startTime);
            return cartDTO;
        } catch (Exception e) {
            logger.error("Failed to create cart for customer ID: {}, error: {}", customerId, e.getMessage(), e);
            throw new RuntimeException("Failed to create cart", e);
        }
    }

    public CartSummaryDTO addItemToCart(String cartId, CartItemDTO cartItemDTO) {
        long startTime = System.currentTimeMillis();
        logger.info("Adding item {} to cart ID: {}", cartItemDTO.getItemId(), cartId);

        CartDTO cart = getCartInternal(cartId);
        Long authenticatedCustomerId = getCustomerIdFromJwt();
        if (!cart.getCustomerId().equals(authenticatedCustomerId)) {
            logger.warn("Unauthorized attempt to modify cart ID: {} by customer ID: {}", cartId, authenticatedCustomerId);
            throw new IllegalAccessError("Unauthorized cart access");
        }

        MenuItem menuItem = menuItemRepository.findById(cartItemDTO.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with id: " + cartItemDTO.getItemId()));

        List<CartItemDTO> items = cart.getItems();
        Optional<CartItemDTO> existingItem = items.stream()
                .filter(item -> item.getItemId().equals(cartItemDTO.getItemId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItemDTO item = existingItem.get();
            item.setQuantity(item.getQuantity() + cartItemDTO.getQuantity());
            item.setSpecialInstructions(cartItemDTO.getSpecialInstructions());
            logger.debug("Updated quantity for item {} in cart ID: {}", cartItemDTO.getItemId(), cartId);
        } else {
            cartItemDTO.setUnitPrice(menuItem.getBasePrice());
            items.add(cartItemDTO);
            logger.debug("Added new item {} to cart ID: {}", cartItemDTO.getItemId(), cartId);
        }

        saveCart(cart);
        CartSummaryDTO summary = getCartSummary(cartId);
        logger.info("Item added to cart ID: {}, took {}ms", cartId, System.currentTimeMillis() - startTime);
        return summary;
    }

    public CartSummaryDTO removeItemFromCart(String cartId, Long itemId) {
        long startTime = System.currentTimeMillis();
        logger.info("Removing item {} from cart ID: {}", itemId, cartId);

        CartDTO cart = getCartInternal(cartId);
        Long authenticatedCustomerId = getCustomerIdFromJwt();
        if (!cart.getCustomerId().equals(authenticatedCustomerId)) {
            logger.warn("Unauthorized attempt to modify cart ID: {} by customer ID: {}", cartId, authenticatedCustomerId);
            throw new IllegalAccessError("Unauthorized cart access");
        }

        cart.getItems().removeIf(item -> item.getItemId().equals(itemId));
        saveCart(cart);
        CartSummaryDTO summary = getCartSummary(cartId);
        logger.info("Item removed from cart ID: {}, took {}ms", cartId, System.currentTimeMillis() - startTime);
        return summary;
    }

    public CartSummaryDTO getCartSummary(String cartId) {
        long startTime = System.currentTimeMillis();
        logger.info("Generating summary for cart ID: {}", cartId);

        CartDTO cart = getCartInternal(cartId);
        Long authenticatedCustomerId = getCustomerIdFromJwt();
        if (!cart.getCustomerId().equals(authenticatedCustomerId)) {
            logger.warn("Unauthorized attempt to access cart ID: {} by customer ID: {}", cartId, authenticatedCustomerId);
            throw new IllegalAccessError("Unauthorized cart access");
        }

        CartSummaryDTO summary = new CartSummaryDTO();
        summary.setCartId(cartId);
        summary.setCustomerId(cart.getCustomerId());
        summary.setItems(cart.getItems());

        double subtotal = cart.getItems().stream()
                .mapToDouble(item -> item.getUnitPrice().doubleValue() * item.getQuantity())
                .sum();
        double taxAmount = subtotal * paymentService.getGstRate();
        double deliveryCharges = 0; // Adjust as needed
        double finalAmount = subtotal + taxAmount + deliveryCharges;

        summary.setSubtotal(subtotal);
        summary.setTaxAmount(taxAmount);
        summary.setDeliveryCharges(deliveryCharges);
        summary.setFinalAmount(finalAmount);

        logger.info("Cart summary generated for ID: {}, took {}ms", cartId, System.currentTimeMillis() - startTime);
        return summary;
    }

    public CartDTO getCart(String cartId) {
        long startTime = System.currentTimeMillis();
        CartDTO cart = getCartInternal(cartId);
        Long authenticatedCustomerId = getCustomerIdFromJwt();
        if (!cart.getCustomerId().equals(authenticatedCustomerId)) {
            logger.warn("Unauthorized attempt to access cart ID: {} by customer ID: {}", cartId, authenticatedCustomerId);
            throw new IllegalAccessError("Unauthorized cart access");
        }
        logger.info("Retrieved cart ID: {}, took {}ms", cartId, System.currentTimeMillis() - startTime);
        return cart;
    }

    public void clearCart(String cartId) {
        long startTime = System.currentTimeMillis();
        logger.info("Clearing cart ID: {}", cartId);
        redisTemplate.delete(CART_KEY_PREFIX + cartId);
        logger.info("Cleared cart ID: {}, took {}ms", cartId, System.currentTimeMillis() - startTime);
    }

    private CartDTO getCartInternal(String cartId) {
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
}