package com.app.taskmanagement.service;

import com.app.taskmanagement.dto.NotificationEvent;
import com.app.taskmanagement.entity.Otp;
import com.app.taskmanagement.exception.BadRequestException;
import com.app.taskmanagement.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpRepository otpRepository;
    private final RabbitTemplate rabbitTemplate;
    private static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    private static final String NOTIFICATION_ROUTING_KEY = "notification.routing.key";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Transactional
    public void generateAndSendOtp(String email, String type) {
        // Generate a secure 6-digit OTP
        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));

        log.info("Generating OTP for email={} type={}", email, type);

        Otp otp = Otp.builder()
                .email(email)
                .code(code)
                .type(type)
                .expirationTime(LocalDateTime.now().plusMinutes(10))
                .verified(false)
                .build();

        otpRepository.save(otp);

        // Build the event now but publish AFTER the transaction commits
        // so the OTP row is visible to the notification-service verify path
        final NotificationEvent event = NotificationEvent.builder()
                .eventType("OTP_EMAIL")
                .recipientEmail(email)
                .subject("FlowBoard – Your OTP Code")
                .message("Your OTP code is: " + code + "\n\nIt will expire in 10 minutes.\nDo not share this code with anyone.")
                .timestamp(LocalDateTime.now())
                .build();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    rabbitTemplate.convertAndSend(NOTIFICATION_EXCHANGE, NOTIFICATION_ROUTING_KEY, event);
                    log.info("OTP event published to RabbitMQ for email={}", email);
                } catch (Exception ex) {
                    log.error("Failed to publish OTP event to RabbitMQ for email={}", email, ex);
                }
            }
        });
    }

    @Transactional
    public boolean verifyOtp(String email, String code, String type) {
        Otp otp = otpRepository.findTopByEmailAndTypeOrderByExpirationTimeDesc(email, type)
                .orElseThrow(() -> new BadRequestException("OTP not found. Please request a new one."));

        if (otp.isVerified()) {
            throw new BadRequestException("OTP has already been used");
        }

        if (otp.getExpirationTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP has expired. Please request a new one.");
        }

        if (!otp.getCode().equals(code)) {
            throw new BadRequestException("Invalid OTP code");
        }

        otp.setVerified(true);
        otpRepository.save(otp);
        log.info("OTP verified successfully for email={} type={}", email, type);
        return true;
    }
}
