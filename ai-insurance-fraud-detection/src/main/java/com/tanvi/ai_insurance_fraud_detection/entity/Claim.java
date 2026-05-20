package com.tanvi.ai_insurance_fraud_detection.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long id;
    private String claimantName;
    private String claimType;
    private double amount;
    private String status;





}
