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
import com.railswad.deliveryservice.util.ExcelHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
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

    @Autowired
    private ExcelHelper excelHelper;

    private void checkAuthorization(Long vendorId) {
        logger.debug("Checking authorization for vendor ID: {}", vendorId);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + auth.getName()));

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            Vendor vendor = vendorRepository.findById(vendorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + vendorId));
            if (!vendor.getUser().getUserId().equals(user.getUserId())) {
                logger.error("User {} not authorized to modify vendor ID: {}", auth.getName(), vendorId);
                throw new SecurityException("User not authorized to modify this vendor's menu");
            }
        }
        logger.debug("Authorization check passed for vendor ID: {}", vendorId);
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
        MenuCategory category = menuCategoryRepository.findById(categoryId)
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
        MenuCategory category = menuCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu category not found with id: " + categoryId));

        checkAuthorization(category.getVendor().getVendorId());

        menuCategoryRepository.delete(category);
        logger.info("Menu category deleted with ID: {}", categoryId);
    }

    @Transactional
    public MenuItemDTO createMenuItem(MenuItemDTO itemDTO) {
        logger.info("Creating menu item for category ID: {}", itemDTO.getCategoryId());
        MenuCategory category = menuCategoryRepository.findById(itemDTO.getCategoryId())
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
        itemDTO.setCategoryName(item.getCategory().getCategoryName());
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
        logger.debug("Fetched menu item ID: {}", itemId);
        return itemDTO;
    }

    public List<MenuItemDTO> getAvailableMenuItemsByVendor(Long vendorId) {
        logger.info("Fetching available menu items for vendor ID: {}", vendorId);
        LocalTime currentTime = LocalTime.now();
        List<MenuItem> items = menuItemRepository.findAvailableItemsByVendor(vendorId, currentTime);
        logger.debug("Found {} available menu items for vendor ID: {}", items.size(), vendorId);
        return items.stream().map(item -> {
            MenuItemDTO itemDTO = new MenuItemDTO();
            itemDTO.setItemId(item.getItemId());
            itemDTO.setCategoryId(item.getCategory().getCategoryId());
            itemDTO.setCategoryName(item.getCategory().getCategoryName());
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
        Page<MenuCategory> categories = menuCategoryRepository.findByVendorVendorId(vendorId, pageable);
        logger.debug("Found {} menu categories for vendor ID: {}", categories.getTotalElements(), vendorId);
        return categories.map(category -> {
            MenuCategoryDTO categoryDTO = new MenuCategoryDTO();
            categoryDTO.setCategoryId(category.getCategoryId());
            categoryDTO.setVendorId(category.getVendor().getVendorId());
            categoryDTO.setCategoryName(category.getCategoryName());
            categoryDTO.setDisplayOrder(category.getDisplayOrder());
            return categoryDTO;
        });
    }

    @Transactional
    public String uploadMenuItems(MultipartFile file, Long vendorId, boolean clearExisting) {
        logger.info("Starting menu upload for vendor ID: {}, clearExisting: {}", vendorId, clearExisting);
        try {
            if (!excelHelper.hasExcelFormat(file)) {
                logger.error("Invalid file format: {}", file.getContentType());
                return "Please upload an Excel file!";
            }

            // Validate authorization
            checkAuthorization(vendorId);

            // Validate vendor
            Vendor vendor = vendorRepository.findById(vendorId)
                    .orElseThrow(() -> {
                        logger.error("Vendor ID {} not found", vendorId);
                        return new ResourceNotFoundException("Vendor not found with id: " + vendorId);
                    });

            // Clear existing menu if requested
            if (clearExisting) {
                logger.info("Clearing existing menu for vendor ID: {}", vendorId);
                List<MenuCategory> existingCategories = menuCategoryRepository.findByVendorVendorId(vendorId);
                for (MenuCategory category : existingCategories) {
                    List<MenuItem> items = menuItemRepository.findByCategory(category);
                    logger.debug("Deleting {} items in category ID: {}", items.size(), category.getCategoryId());
                    menuItemRepository.deleteAll(items);
                }
                menuCategoryRepository.deleteAll(existingCategories);
                logger.info("Cleared {} categories for vendor ID: {}", existingCategories.size(), vendorId);
            }

            // Parse Excel file
            List<MenuItemDTO> dtos = excelHelper.excelToMenuItemDTOs(file.getInputStream());
            logger.info("Parsed {} menu items from Excel file", dtos.size());

            List<MenuItem> menuItems = new ArrayList<>();
            for (MenuItemDTO dto : dtos) {
                // Validate required fields
                if (dto.getCategoryName() == null || dto.getCategoryName().trim().isEmpty()) {
                    logger.error("Category name is required at row {}", dtos.indexOf(dto) + 2);
                    throw new IllegalArgumentException("Category name is required at row " + (dtos.indexOf(dto) + 2));
                }
                if (dto.getItemName() == null || dto.getItemName().trim().isEmpty()) {
                    logger.error("Item name is required at row {}", dtos.indexOf(dto) + 2);
                    throw new IllegalArgumentException("Item name is required at row " + (dtos.indexOf(dto) + 2));
                }
                if (dto.getPrice() == null) {
                    logger.error("Price is required at row {}", dtos.indexOf(dto) + 2);
                    throw new IllegalArgumentException("Price is required at row " + (dtos.indexOf(dto) + 2));
                }

                // Find or create category
                MenuCategory category = menuCategoryRepository.findByVendorAndCategoryName(vendor, dto.getCategoryName());
                if (category == null) {
                    logger.info("Creating new category '{}' for vendor ID: {}", dto.getCategoryName(), vendorId);
                    category = new MenuCategory();
                    category.setVendor(vendor);
                    category.setCategoryName(dto.getCategoryName());
                    category.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);
                    category = menuCategoryRepository.save(category);
                    logger.debug("Created category ID: {}", category.getCategoryId());
                }

                // Check for duplicate item
                if (menuItemRepository.findByCategoryAndItemName(category, dto.getItemName()).isPresent()) {
                    logger.error("Duplicate item '{}' in category '{}' at row {}", dto.getItemName(), dto.getCategoryName(), dtos.indexOf(dto) + 2);
                    throw new IllegalArgumentException("Item '" + dto.getItemName() + "' already exists in category '" + dto.getCategoryName() + "' at row " + (dtos.indexOf(dto) + 2));
                }

                // Create MenuItem
                MenuItem item = new MenuItem();
                item.setCategory(category);
                item.setItemName(dto.getItemName());
                item.setDescription(dto.getDescription());
                item.setPrice(dto.getPrice());
                item.setVegetarian(dto.isVegetarian());
                item.setAvailable(dto.isAvailable());
                item.setPreparationTimeMin(dto.getPreparationTimeMin());
                item.setImageUrl(dto.getImageUrl());
                item.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);
                item.setAvailableStartTime(dto.getAvailableStartTime());
                item.setAvailableEndTime(dto.getAvailableEndTime());

                menuItems.add(item);
                logger.debug("Prepared menu item: {} in category: {}", dto.getItemName(), dto.getCategoryName());
            }

            // Save all menu items
            menuItemRepository.saveAll(menuItems);
            logger.info("Successfully saved {} menu items for vendor ID: {}", menuItems.size(), vendorId);
            return "Uploaded the file successfully for vendor ID " + vendorId + ": " + file.getOriginalFilename();
        } catch (IOException e) {
            logger.error("Failed to process Excel file: {}", e.getMessage());
            throw new RuntimeException("Failed to process Excel file: " + e.getMessage());
        }
    }
}