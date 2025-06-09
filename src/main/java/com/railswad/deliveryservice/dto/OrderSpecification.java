package com.railswad.deliveryservice.dto;

import com.railswad.deliveryservice.dto.OrderFilterDTO;
import com.railswad.deliveryservice.entity.Order;
import com.railswad.deliveryservice.entity.OrderItem;
import com.railswad.deliveryservice.entity.MenuItem;
import com.railswad.deliveryservice.entity.Vendor;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class OrderSpecification {
    public static Specification<Order> withFilters(OrderFilterDTO filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by station ID
            if (filter.getStationId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("deliveryStation").get("stationId"), filter.getStationId()));
            }

            // Filter by vegetarian/non-vegetarian menu items
            if (filter.getIsVegetarianMenuItem() != null) {
                Join<Order, OrderItem> orderItems = root.join("orderItems");
                Join<OrderItem, MenuItem> menuItems = orderItems.join("item");
                predicates.add(criteriaBuilder.equal(menuItems.get("isVegetarian"), filter.getIsVegetarianMenuItem()));
                query.distinct(true); // Avoid duplicate orders due to join
            }

            // Filter by vegetarian/non-vegetarian vendor
            if (filter.getIsVegetarianVendor() != null) {
                Join<Order, Vendor> vendor = root.join("vendor");
                predicates.add(criteriaBuilder.equal(vendor.get("isVegetarian"), filter.getIsVegetarianVendor()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}