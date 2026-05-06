package com.app.taskmanagement.repository;

import com.app.taskmanagement.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Long> {
    Optional<Otp> findTopByEmailAndTypeOrderByExpirationTimeDesc(String email, String type);
}
