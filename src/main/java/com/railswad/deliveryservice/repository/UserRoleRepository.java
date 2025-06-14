package com.railswad.deliveryservice.repository;


import com.railswad.deliveryservice.entity.User;
import com.railswad.deliveryservice.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRole.UserRoleId> {
    List<UserRole> findByUser(User user);
}