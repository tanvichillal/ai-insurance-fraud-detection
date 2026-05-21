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
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

    private final OkHttpClient client = new OkHttpClient();

    // ── for digital PDFs — send raw text ──────────────────────────────────
    public String analyzeTextDocument(String extractedText) {
        String prompt = """
                You are an insurance fraud analyst. Analyze this document and return:
                1. Claimant name
                2. Claim type
                3. Claimed amount
                4. Incident date
                5. Fraud indicators (suspicious keywords, inconsistencies)
                6. Fraud risk: LOW / MEDIUM / HIGH
                
                Document text:
                """ + extractedText;

        return callGemini(prompt);
    }

    // ── for scanned PDFs or direct image uploads ───────────────────────────
    public String analyzeImageDocument(String base64Image, String mimeType) {
        String prompt = """
                You are an insurance fraud analyst. Read this document image and extract:
                1. Claimant name
                2. Claim type
                3. Claimed amount
                4. Incident date
                5. Fraud indicators (suspicious keywords, inconsistencies)
                6. Fraud risk: LOW / MEDIUM / HIGH
                """;

        return callGeminiVision(base64Image, mimeType, prompt);
    }

    // ── internal: text-only Gemini call ───────────────────────────────────
    private String callGemini(String prompt) {
        // build request body
        JSONObject textPart = new JSONObject()
                .put("text", prompt);

        JSONObject content = new JSONObject()
                .put("parts", new JSONArray().put(textPart));

        JSONObject body = new JSONObject()
                .put("contents", new JSONArray().put(content));

        return executeRequest(body);
    }

    // ── internal: vision (image + text) Gemini call ───────────────────────
    private String callGeminiVision(String base64Image, String mimeType, String prompt) {
        // image part
        JSONObject imageData = new JSONObject()
                .put("mime_type", mimeType)
                .put("data", base64Image);

        JSONObject imagePart = new JSONObject()
                .put("inline_data", imageData);

        // text prompt part
        JSONObject textPart = new JSONObject()
                .put("text", prompt);

        // combine both parts into one content block
        JSONObject content = new JSONObject()
                .put("parts", new JSONArray().put(imagePart).put(textPart));

        JSONObject body = new JSONObject()
                .put("contents", new JSONArray().put(content));

        return executeRequest(body);
    }

    // ── shared HTTP execution ──────────────────────────────────────────────
    private String executeRequest(JSONObject body) {
        String url = GEMINI_TEXT_URL + "?key=" + apiKey;

        RequestBody requestBody = RequestBody.create(
                body.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return "Gemini API error: " + response.code() + " " + response.message();
            }

            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);

            // parse: candidates[0].content.parts[0].text
            return json
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");

        } catch (IOException e) {
            return "Request failed: " + e.getMessage();
        }
    }
}