package com.tanvi.ai_insurance_fraud_detection.service;

import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    public String analyzeDocument(String extractedText) throws IOException {

        OkHttpClient client = new OkHttpClient();

        String prompt = """
                Analyze this insurance claim text and extract:
                - hospital name
                - amount
                - suspicious patterns
                - fraud indicators

                Text:
                """ + extractedText;

        String json = """
                {
                  "contents": [{
                    "parts":[{"text": "%s"}]
                  }]
                }
                """.formatted(prompt);

        RequestBody body = RequestBody.create(
                json,
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey)
                .post(body)
                .build();

        Response response = client.newCall(request).execute();

        return response.body().string();
    }
}