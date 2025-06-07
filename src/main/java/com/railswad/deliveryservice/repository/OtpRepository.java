package com.railswad.deliveryservice.repository;

import com.railswad.deliveryservice.entity.Otp;
import com.railswad.deliveryservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.Optional;

import java.util.Optional;


@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {
    Optional<Otp> findByUserIdAndOtpCodeAndOtpType(Long userId, String otpCode, String otpType);

    Optional<Otp> findByUserIdAndOtpType(Long userId, String otpType);

    @Modifying
    @Query("DELETE FROM Otp o WHERE o.expirationTime < :currentTime")
    int deleteByExpirationTimeBefore(ZonedDateTime currentTime);

    @Modifying
    @Query("DELETE FROM Otp o WHERE o.userId = :userId AND o.otpType = :otpType")
    void deleteByUserIdAndOtpType(Long userId, String otpType);
}