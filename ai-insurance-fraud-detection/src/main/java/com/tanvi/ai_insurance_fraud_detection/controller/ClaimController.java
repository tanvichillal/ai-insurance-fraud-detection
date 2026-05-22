package com.tanvi.ai_insurance_fraud_detection.controller;

import com.tanvi.ai_insurance_fraud_detection.entity.Claim;
import com.tanvi.ai_insurance_fraud_detection.repository.ClaimRepository;
import com.tanvi.ai_insurance_fraud_detection.service.GeminiService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/claims")
@CrossOrigin(origins = "*")
public class ClaimController {

    private final ClaimRepository claimRepository;
    private final GeminiService geminiService;

    public ClaimController(ClaimRepository claimRepository,
                           GeminiService geminiService) {
        this.claimRepository = claimRepository;
        this.geminiService = geminiService;
    }

    @PostMapping
    public ResponseEntity<Claim> createClaim(@RequestBody Claim claim) {
        return ResponseEntity.ok(claimRepository.save(claim));
    }

    @GetMapping
    public ResponseEntity<List<Claim>> getAllClaims() {
        return ResponseEntity.ok(claimRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Claim> getClaimById(@PathVariable Long id) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Claim not found: " + id));
        return ResponseEntity.ok(claim);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteClaim(@PathVariable Long id) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Claim not found: " + id));
        claimRepository.delete(claim);
        return ResponseEntity.ok("Deleted claim with id " + id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Claim> updateClaim(@PathVariable Long id,
                                             @RequestBody Claim updatedClaim) {
        Claim existing = claimRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Claim not found: " + id));

        existing.setClaimantName(updatedClaim.getClaimantName());
        existing.setClaimType(updatedClaim.getClaimType());
        existing.setAmount(updatedClaim.getAmount());
        existing.setStatus(updatedClaim.getStatus());

        return ResponseEntity.ok(claimRepository.save(existing));
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file)
            throws IOException {

        Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads");
        Files.createDirectories(uploadDir);

        Path filePath = uploadDir.resolve(file.getOriginalFilename());
        Files.write(filePath, file.getBytes());

        return ResponseEntity.ok("File uploaded: " + file.getOriginalFilename());
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

                // try text extraction first (works for digital PDFs)
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);

                if (text != null && text.trim().length() >= 20) {
                    // digital PDF — send text to Gemini
                    return ResponseEntity.ok(geminiService.analyzeTextDocument(text));
                }

                // scanned PDF — render each page to image, send to Gemini Vision
                PDFRenderer renderer = new PDFRenderer(document);
                List<String> results = new ArrayList<>();

                for (int i = 0; i < 1; i++) {
                    BufferedImage image = renderer.renderImageWithDPI(i, 50);
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
                .body("Unsupported file type. Please upload a PDF or image (JPG/PNG).");
    }
}