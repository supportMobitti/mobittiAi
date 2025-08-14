package org.mobitti.services;

import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Component
public class GeminiRagService {



    private static final String ENDPOINT = "https://us-central1-aiplatform.googleapis.com/v1/projects/mobittirag/locations/us-central1/publishers/google/models/gemini-2.5-flash-lite:streamGenerateContent";

    @Autowired
    private GoogleCredentials googleCredentials;

    public String generate(String query,String storageId) throws IOException, InterruptedException {
        String token = getAccessToken(googleCredentials);

        String requestBody = buildRequestJson(query,storageId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    public String buildRequestJson(String prompt,String storageId){
        return """
{ 
  "contents": [ 
    { 
      "role": "user", 
      "parts": [{ "text": "%s" }] 
    } 
  ], 
  "tools": [ 
    { 
      "retrieval": { 
        "vertex_rag_store": { 
          "rag_resources": [ 
            { 
              "rag_corpus": "projects/mobittirag/locations/us-central1/ragCorpora/%s" 
            } 
          ], 
          "rag_retrieval_config": { 
            "top_k": 10, 
            "ranking": { 
              "rank_service": { 
                "model_name": "semantic-ranker-default@latest" 
              } 
            } 
          } 
        } 
      } 
    } 
  ], 
  "generationConfig": { 
    "temperature": 0.2, 
    "topP": 0.8, 
    "topK": 40, 
    "maxOutputTokens": 1024 
  } 
} 
""".formatted(escapeJson(prompt), storageId);

    }


    private String getAccessToken(GoogleCredentials credentials) throws IOException {
        if (credentials.createScopedRequired()) {
            credentials = credentials.createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));
        }
        credentials.refreshIfExpired();
        return credentials.getAccessToken().getTokenValue();
    }

    private String escapeJson(String text) {
        return text.replace("\"", "\\\"");
    }
}
