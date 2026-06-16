package com.programming.techie.service;

import com.programming.techie.entity.OtpEntity;
import com.programming.techie.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpRepository otpRepository;

    public String generateOtp(String mobileNumber) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        OtpEntity otpEntity = new OtpEntity();
        otpEntity.setMobileNumber(mobileNumber);
        otpEntity.setOtp(otp);
        otpEntity.setExpiryTime(LocalDateTime.now().plusMinutes(5));
        otpRepository.save(otpEntity);
        return otp;
    }

    public boolean verifyOtp(String mobileNumber, String otp) {
        return otpRepository.findTopByMobileNumberOrderByExpiryTimeDesc(mobileNumber)
                .map(otpEntity -> {
                    if (otpEntity.getOtp().equals(otp) && otpEntity.getExpiryTime().isAfter(LocalDateTime.now()) && !otpEntity.isVerified()) {
                        otpEntity.setVerified(true);
                        otpRepository.save(otpEntity);
                        return true;
                    }
                    return false;
                }).orElse(false);
    }
}
