package com.tanvi.ai_insurance_fraud_detection.repository;


import com.tanvi.ai_insurance_fraud_detection.entity.Claim;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
}
