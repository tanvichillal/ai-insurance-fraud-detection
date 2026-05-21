package com.tanvi.ai_insurance_fraud_detection.controller;

import com.tanvi.ai_insurance_fraud_detection.entity.Claim;
import com.tanvi.ai_insurance_fraud_detection.repository.ClaimRepository;
import com.tanvi.ai_insurance_fraud_detection.service.GeminiService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.apache.pdfbox.rendering.PDFRenderer;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.ArrayList;

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
    public ResponseEntity<String> extractDocument(@RequestParam("file") MultipartFile file)
            throws IOException {

        String contentType = file.getContentType();

        // CASE 1: direct image upload (jpg, png)
        if (contentType != null && contentType.startsWith("image/")) {
            String base64Image = Base64.getEncoder()
                    .encodeToString(file.getBytes());
            String result = geminiService.analyzeImageDocument(base64Image, contentType);
            return ResponseEntity.ok(result);
        }

        // CASE 2: PDF upload
        if (contentType != null && contentType.equals("application/pdf")) {
            try (PDDocument document = PDDocument.load(file.getInputStream())) {

                // try text extraction first
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);

                if (text != null && text.trim().length() >= 20) {
                    // digital PDF — use text
                    return ResponseEntity.ok(geminiService.analyzeTextDocument(text));
                }

                // scanned PDF — render to image and send to Gemini Vision
                PDFRenderer renderer = new PDFRenderer(document);
                List<String> results = new ArrayList<>();

                for (int i = 0; i < document.getNumberOfPages(); i++) {
                    BufferedImage image = renderer.renderImageWithDPI(i, 150);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(image, "PNG", baos);
                    String base64 = Base64.getEncoder()
                            .encodeToString(baos.toByteArray());
                    results.add(geminiService.analyzeImageDocument(base64, "image/png"));
                }

                return ResponseEntity.ok(String.join("\n\n", results));
            }
        }

        return ResponseEntity.badRequest()
                .body("Unsupported file type. Upload a PDF or image (JPG/PNG).");
    }
