package com.railswad.deliveryservice.service;

import com.railswad.deliveryservice.dto.MenuCategoryDTO;
import com.railswad.deliveryservice.dto.MenuItemDTO;
import com.railswad.deliveryservice.entity.MenuCategory;
import com.railswad.deliveryservice.entity.MenuItem;
import com.railswad.deliveryservice.entity.User;
import com.railswad.deliveryservice.entity.Vendor;
import com.railswad.deliveryservice.exception.ResourceNotFoundException;
import com.railswad.deliveryservice.repository.MenuCategoryRepository;
import com.railswad.deliveryservice.repository.MenuItemRepository;
import com.railswad.deliveryservice.repository.UserRepository;
import com.railswad.deliveryservice.repository.VendorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MenuService {

    private static final Logger logger = LoggerFactory.getLogger(MenuService.class);

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private MenuCategoryRepository menuCategoryRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private UserRepository userRepository;

    private void checkAuthorization(Long vendorId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + auth.getName()));

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            Vendor vendor = vendorRepository.findById(vendorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + vendorId));
            if (!vendor.getUser().getUserId().equals(user.getUserId())) {
                throw new SecurityException("User not authorized to modify this vendor's menu");
            }
        }
    }

    @Transactional
    public MenuCategoryDTO createMenuCategory(MenuCategoryDTO categoryDTO) {
        logger.info("Creating menu category for vendor ID: {}", categoryDTO.getVendorId());
        checkAuthorization(categoryDTO.getVendorId());

        Vendor vendor = vendorRepository.findById(categoryDTO.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + categoryDTO.getVendorId()));

        MenuCategory category = new MenuCategory();
        category.setVendor(vendor);
        category.setCategoryName(categoryDTO.getCategoryName());
        category.setDisplayOrder(categoryDTO.getDisplayOrder());

        MenuCategory savedCategory = menuCategoryRepository.save(category);
        categoryDTO.setCategoryId(savedCategory.getCategoryId());
        logger.info("Menu category created with ID: {}", savedCategory.getCategoryId());
        return categoryDTO;
    }

    @Transactional
    public MenuCategoryDTO updateMenuCategory(Long categoryId, MenuCategoryDTO categoryDTO) {
        logger.info("Updating menu category ID: {}", categoryId);
        MenuCategory category = menuCategoryRepository.findById((long) Math.toIntExact(categoryId))
                .orElseThrow(() -> new ResourceNotFoundException("Menu category not found with id: " + categoryId));

        checkAuthorization(category.getVendor().getVendorId());

        category.setCategoryName(categoryDTO.getCategoryName());
        category.setDisplayOrder(categoryDTO.getDisplayOrder());

        MenuCategory updatedCategory = menuCategoryRepository.save(category);
        categoryDTO.setCategoryId(updatedCategory.getCategoryId());
        categoryDTO.setVendorId(updatedCategory.getVendor().getVendorId());
        logger.info("Menu category updated with ID: {}", categoryId);
        return categoryDTO;
    }

    @Transactional
    public void deleteMenuCategory(Long categoryId) {
        logger.info("Deleting menu category ID: {}", categoryId);
        MenuCategory category = menuCategoryRepository.findById((long) Math.toIntExact(categoryId))
                .orElseThrow(() -> new ResourceNotFoundException("Menu category not found with id: " + categoryId));

        checkAuthorization(category.getVendor().getVendorId());

        menuCategoryRepository.delete(category);
        logger.info("Menu category deleted with ID: {}", categoryId);
    }

    @Transactional
    public MenuItemDTO createMenuItem(MenuItemDTO itemDTO) {
        logger.info("Creating menu item for category ID: {}", itemDTO.getCategoryId());
        MenuCategory category = menuCategoryRepository.findById((long) Math.toIntExact(itemDTO.getCategoryId()))
                .orElseThrow(() -> new ResourceNotFoundException("Menu category not found with id: " + itemDTO.getCategoryId()));

        checkAuthorization(category.getVendor().getVendorId());

        MenuItem item = new MenuItem();
        item.setCategory(category);
        item.setItemName(itemDTO.getItemName());
        item.setDescription(itemDTO.getDescription());
        item.setPrice(itemDTO.getPrice());
        item.setVegetarian(itemDTO.isVegetarian());
        item.setAvailable(itemDTO.isAvailable());
        item.setPreparationTimeMin(itemDTO.getPreparationTimeMin());
        item.setImageUrl(itemDTO.getImageUrl());
        item.setDisplayOrder(itemDTO.getDisplayOrder());
        item.setAvailableStartTime(itemDTO.getAvailableStartTime());
        item.setAvailableEndTime(itemDTO.getAvailableEndTime());

        MenuItem savedItem = menuItemRepository.save(item);
        itemDTO.setItemId(savedItem.getItemId());
        logger.info("Menu item created with ID: {}", savedItem.getItemId());
        return itemDTO;
    }

    @Transactional
    public MenuItemDTO updateMenuItem(Long itemId, MenuItemDTO itemDTO) {
        logger.info("Updating menu item ID: {}", itemId);
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with id: " + itemId));
        MenuCategory category = menuCategoryRepository.findById(itemDTO.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu category not found with id: " + itemDTO.getCategoryId()));

        checkAuthorization(category.getVendor().getVendorId());

        item.setCategory(category);
        item.setItemName(itemDTO.getItemName());
        item.setDescription(itemDTO.getDescription());
        item.setPrice(itemDTO.getPrice());
        item.setVegetarian(itemDTO.isVegetarian());
        item.setAvailable(itemDTO.isAvailable());
        item.setPreparationTimeMin(itemDTO.getPreparationTimeMin());
        item.setImageUrl(itemDTO.getImageUrl());
        item.setDisplayOrder(itemDTO.getDisplayOrder());
        item.setAvailableStartTime(itemDTO.getAvailableStartTime());
        item.setAvailableEndTime(itemDTO.getAvailableEndTime());

        MenuItem updatedItem = menuItemRepository.save(item);
        itemDTO.setItemId(updatedItem.getItemId());
        logger.info("Menu item updated with ID: {}", itemId);
        return itemDTO;
    }

    @Transactional
    public void deleteMenuItem(Long itemId) {
        logger.info("Deleting menu item ID: {}", itemId);
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with id: " + itemId));

        checkAuthorization(item.getCategory().getVendor().getVendorId());

        menuItemRepository.delete(item);
        logger.info("Menu item deleted with ID: {}", itemId);
    }

    public MenuItemDTO getMenuItemById(Long itemId) {
        logger.info("Fetching menu item with ID: {}", itemId);
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with id: " + itemId));
        MenuItemDTO itemDTO = new MenuItemDTO();
        itemDTO.setItemId(item.getItemId());
        itemDTO.setCategoryId(item.getCategory().getCategoryId());
        itemDTO.setItemName(item.getItemName());
        itemDTO.setDescription(item.getDescription());
        itemDTO.setPrice(item.getPrice());
        itemDTO.setVegetarian(item.isVegetarian());
        itemDTO.setAvailable(item.isAvailable());
        itemDTO.setPreparationTimeMin(item.getPreparationTimeMin());
        itemDTO.setImageUrl(item.getImageUrl());
        itemDTO.setDisplayOrder(item.getDisplayOrder());
        itemDTO.setAvailableStartTime(item.getAvailableStartTime());
        itemDTO.setAvailableEndTime(item.getAvailableEndTime());
        return itemDTO;
    }

    public List<MenuItemDTO> getAvailableMenuItemsByVendor(Long vendorId) {
        logger.info("Fetching available menu items for vendor ID: {}", vendorId);
        LocalTime currentTime = LocalTime.now();
        return menuItemRepository.findAvailableItemsByVendor(vendorId, currentTime).stream()
                .map(item -> {
                    MenuItemDTO itemDTO = new MenuItemDTO();
                    itemDTO.setItemId(item.getItemId());
                    itemDTO.setCategoryId(item.getCategory().getCategoryId());
                    itemDTO.setItemName(item.getItemName());
                    itemDTO.setDescription(item.getDescription());
                    itemDTO.setPrice(item.getPrice());
                    itemDTO.setVegetarian(item.isVegetarian());
                    itemDTO.setAvailable(item.isAvailable());
                    itemDTO.setPreparationTimeMin(item.getPreparationTimeMin());
                    itemDTO.setImageUrl(item.getImageUrl());
                    itemDTO.setDisplayOrder(item.getDisplayOrder());
                    itemDTO.setAvailableStartTime(item.getAvailableStartTime());
                    itemDTO.setAvailableEndTime(item.getAvailableEndTime());
                    return itemDTO;
                }).collect(Collectors.toList());
    }

    public Page<MenuCategoryDTO> getMenuCategoriesByVendor(Long vendorId, Pageable pageable) {
        logger.info("Fetching menu categories for vendor ID: {}", vendorId);
        return menuCategoryRepository.findByVendorVendorId(vendorId, pageable).map(category -> {
            MenuCategoryDTO categoryDTO = new MenuCategoryDTO();
            categoryDTO.setCategoryId(category.getCategoryId());
            categoryDTO.setVendorId(category.getVendor().getVendorId());
            categoryDTO.setCategoryName(category.getCategoryName());
            categoryDTO.setDisplayOrder(category.getDisplayOrder());
            return categoryDTO;
        });
    }
}
