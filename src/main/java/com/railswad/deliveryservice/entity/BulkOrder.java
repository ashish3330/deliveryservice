package com.railswad.deliveryservice.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bulk_orders")
public class BulkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone is required")
    private String phone;

    @NotBlank(message = "Delivery station is required")
    private String deliveryStation;

    @NotBlank(message = "Order details are required")
    @Column(columnDefinition = "TEXT")
    private String orderDetails;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getDeliveryStation() { return deliveryStation; }
    public void setDeliveryStation(String deliveryStation) { this.deliveryStation = deliveryStation; }
    public String getOrderDetails() { return orderDetails; }
    public void setOrderDetails(String orderDetails) { this.orderDetails = orderDetails; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}