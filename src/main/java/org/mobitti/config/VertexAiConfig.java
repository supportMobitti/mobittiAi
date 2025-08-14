package org.mobitti.config;


import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vertexai.VertexAI;
import org.mobitti.helpers.SystemPrompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableConfigurationProperties(VertexAiRagProperties.class)
public class VertexAiConfig {


    @Autowired
    private VertexAiRagProperties properties;

    @Value("${google.cloud.credentials.location}")
    private Resource credentialsResource;


    @Bean
    public VertexAI vertexAI() throws IOException {
        return new VertexAI(properties.getProjectId(), properties.getLocation(),googleCredentials());
    }



    @Bean
    public GoogleCredentials googleCredentials() throws IOException {
        System.setProperty("GOOGLE_GENAI_USE_VERTEXAI", "true");
        System.setProperty("GOOGLE_CLOUD_PROJECT", properties.getProjectId());
        System.setProperty("GOOGLE_CLOUD_LOCATION", properties.getLocation());
        if (credentialsResource != null && credentialsResource.exists()) {
            System.out.println("Loading credentials from: " + credentialsResource.getFilename());

            return GoogleCredentials
                    .fromStream(credentialsResource.getInputStream())
                    .createScoped("https://www.googleapis.com/auth/cloud-platform");

        } else {
            System.out.println("Using default credentials");
            return GoogleCredentials.getApplicationDefault()
                    .createScoped("https://www.googleapis.com/auth/cloud-platform");
        }
    }


    @Bean
    public SystemPrompt systemPrompt() {
        SystemPrompt systemPrompt = new SystemPrompt();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("system-prompt.txt")) {
            if (is == null) {
                throw new RuntimeException("system-prompt.txt not found in resources");
            }
            systemPrompt.setSystemPrompt(new String(is.readAllBytes(), StandardCharsets.UTF_8));
            return systemPrompt;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load system prompt", e);
        }
    }

}
