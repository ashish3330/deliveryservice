package com.railswad.deliveryservice.repository;


import com.railswad.deliveryservice.entity.User;
import com.railswad.deliveryservice.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    List<UserRole> findByUser(User user);
}