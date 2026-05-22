package com.tanvi.ai_insurance_fraud_detection.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "claims")
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String claimantName;
    private String claimType;
    private double amount;
    private String status;

    // ── Getters ──────────────────────────────
    public Long getId() { return id; }
    public String getClaimantName() { return claimantName; }
    public String getClaimType() { return claimType; }
    public double getAmount() { return amount; }
    public String getStatus() { return status; }

    // ── Setters ──────────────────────────────
    public void setId(Long id) { this.id = id; }
    public void setClaimantName(String claimantName) { this.claimantName = claimantName; }
    public void setClaimType(String claimType) { this.claimType = claimType; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setStatus(String status) { this.status = status; }

    // ── No-arg constructor (required by JPA) ─
    public Claim() {}

    // ── All-args constructor ──────────────────
    public Claim(Long id, String claimantName, String claimType,
                 double amount, String status) {
        this.id = id;
        this.claimantName = claimantName;
        this.claimType = claimType;
        this.amount = amount;
        this.status = status;
    }
}