package com.tanvi.ai_insurance_fraud_detection.controller;

import com.tanvi.ai_insurance_fraud_detection.entity.Claim;
import com.tanvi.ai_insurance_fraud_detection.repository.ClaimRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/claims")
public class ClaimController {

    private final ClaimRepository claimRepository;

    public ClaimController(ClaimRepository claimRepository) {
        this.claimRepository = claimRepository;
    }

    @PostMapping
    public Claim createClaim(@RequestBody Claim claim){

        return claimRepository.save(claim);
    }

    @GetMapping
    public List<Claim> getAllClaim(){
        return claimRepository.findAll();
    }

    @DeleteMapping("/{id}")
    public String deleteClaim(@PathVariable Long id){

        Claim claim = claimRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Claim Not Found")
        );
        claimRepository.delete(claim);
        return "Deleted all the details of id " + id;
    }

    @PutMapping("/{id}")
    public Claim updateClaim(@PathVariable Long id ,@RequestBody Claim updatedClaim){

        Claim existingClaim = claimRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Claim not found")
        );

        existingClaim.setClaimantName(updatedClaim.getClaimantName());
        existingClaim.setClaimType(updatedClaim.getClaimType());
        existingClaim.setAmount(updatedClaim.getAmount());
        existingClaim.setStatus(updatedClaim.getStatus());

        return claimRepository.save(existingClaim);
    }


}
