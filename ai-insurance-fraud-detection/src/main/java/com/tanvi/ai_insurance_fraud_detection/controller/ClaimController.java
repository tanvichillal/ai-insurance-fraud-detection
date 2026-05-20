package com.tanvi.ai_insurance_fraud_detection.controller;

import com.tanvi.ai_insurance_fraud_detection.entity.Claim;
import com.tanvi.ai_insurance_fraud_detection.repository.ClaimRepository;
import com.tanvi.ai_insurance_fraud_detection.service.GeminiService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.io.File;
import java.io.IOException;


import java.util.List;

@RestController
@RequestMapping("/claims")
public class ClaimController {

    private final ClaimRepository claimRepository;
    private final GeminiService geminiService;
    public ClaimController(ClaimRepository claimRepository,
                           GeminiService geminiService) {

        this.claimRepository = claimRepository;
        this.geminiService = geminiService;
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

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file)
        throws IOException{


            String uploadDir = System.getProperty("user.dir") + "/uploads/";
            File directory = new File(uploadDir);

            if(!directory.exists()){
                directory.mkdirs();
            }

            String filePath = uploadDir + file.getOriginalFilename();

            file.transferTo(new File(filePath));

            return "File uploaded successfully : " + file.getOriginalFilename();
        }


    @PostMapping("/extract")
    public String extractPdfText(@RequestParam("file") MultipartFile file)
            throws IOException{
        PDDocument document = PDDocument.load(file.getInputStream());

        PDFTextStripper pdfStripper = new PDFTextStripper();

        String text = pdfStripper.getText(document);

        if(text == null || text.trim().isEmpty() || text.length() < 20){

            return geminiService.analyzeDocument("Analyze scanned insurance document");

        }
        else{

            return text;
        }
        }

    }
