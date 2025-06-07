package com.railswad.deliveryservice.entity;

import com.railswad.deliveryservice.entity.MenuCategory;
import com.railswad.deliveryservice.entity.OrderItem;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "menu_items")
@Getter
@Setter
public class    MenuItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private MenuCategory category;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(name = "description")
    private String description;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "is_vegetarian", nullable = false)
    private boolean vegetarian;

    @Column(name = "is_available", nullable = false)
    private boolean available = true;

    @Column(name = "preparation_time_min")
    private Integer preparationTimeMin;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "available_start_time")
    private LocalTime availableStartTime;

    @Column(name = "available_end_time")
    private LocalTime availableEndTime;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems;

    @PrePersist
    @PreUpdate
    private void validateTimes() {
        if (availableStartTime != null && availableEndTime != null) {
            if (availableEndTime.isBefore(availableStartTime)) {
                throw new IllegalArgumentException("End time must be after start time");
            }
        }
    }
}
