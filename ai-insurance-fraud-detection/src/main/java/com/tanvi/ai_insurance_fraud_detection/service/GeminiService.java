package com.tanvi.ai_insurance_fraud_detection.service;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private static final String GEMINI_TEXT_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    // ── for digital PDFs — send raw text ──────────────────────────────────
    public String analyzeTextDocument(String extractedText) {
        String prompt = """
                You are a vehicle insurance fraud analyst. Analyze this document and return:
                
                1. Claimant name
                2. Vehicle details (make, model, registration number)
                3. Claim type (accident / theft / damage)
                4. Claimed repair amount
                5. Incident date and location
                6. Fraud indicators:
                   - Repair cost higher than vehicle market value
                   - Duplicate or reused accident photos
                   - Mismatched vehicle registration vs policy
                   - Suspicious repair shop (inflated bills)
                   - Previous fraud history keywords
                   - Location inconsistencies
                7. Fraud risk: LOW / MEDIUM / HIGH
                8. Fraud score: 0-100
                
                Document text:
                """ + extractedText;

        return callGemini(prompt);
    }

    // ── for scanned PDFs or direct image uploads ───────────────────────────
    public String analyzeImageDocument(String base64Image, String mimeType) {
        String prompt = """
                You are a vehicle insurance fraud analyst. Analyze this document image and return:
                
                1. Claimant name
                2. Vehicle details (make, model, registration number)
                3. Claim type (accident / theft / damage)
                4. Claimed repair amount
                5. Incident date and location
                6. Fraud indicators:
                   - Repair cost higher than vehicle market value
                   - Duplicate or reused accident photos
                   - Mismatched vehicle registration vs policy
                   - Suspicious repair shop (inflated bills)
                   - Previous fraud history keywords
                   - Location inconsistencies
                7. Fraud risk: LOW / MEDIUM / HIGH
                8. Fraud score: 0-100
                """;

        return callGeminiVision(base64Image, mimeType, prompt);
    }

    // ── internal: text-only Gemini call ───────────────────────────────────
    private String callGemini(String prompt) {
        JSONObject textPart = new JSONObject()
                .put("text", prompt);

        JSONObject content = new JSONObject()
                .put("parts", new JSONArray().put(textPart));

        JSONObject body = new JSONObject()
                .put("contents", new JSONArray().put(content));

        return executeRequest(body);
    }

    // ── internal: vision Gemini call ──────────────────────────────────────
    private String callGeminiVision(String base64Image, String mimeType, String prompt) {
        JSONObject imageData = new JSONObject()
                .put("mime_type", mimeType)
                .put("data", base64Image);

        JSONObject imagePart = new JSONObject()
                .put("inline_data", imageData);

        JSONObject textPart = new JSONObject()
                .put("text", prompt);

        JSONObject content = new JSONObject()
                .put("parts", new JSONArray().put(imagePart).put(textPart));

        JSONObject body = new JSONObject()
                .put("contents", new JSONArray().put(content));

        return executeRequest(body);
    }

    // ── shared HTTP execution ──────────────────────────────────────────────
    private String executeRequest(JSONObject body) {
        String url = GEMINI_TEXT_URL + "?key=" + apiKey;

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                RequestBody requestBody = RequestBody.create(
                        body.toString(),
                        MediaType.parse("application/json")
                );

                Request request = new Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.code() == 429) {
                        Thread.sleep(3000L * attempt);
                        continue;
                    }
                    if (!response.isSuccessful()) {
                        return "Gemini API error: " + response.code() + " " + response.message();
                    }
                    String responseBody = response.body().string();
                    JSONObject json = new JSONObject(responseBody);
                    return json
                            .getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");
                }
            } catch (Exception e) {
                return "Request failed: " + e.getMessage();
            }
        }
        return "Gemini API rate limit exceeded. Please try again in a minute.";
    }
}