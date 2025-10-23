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
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
@EnableConfigurationProperties(VertexAiRagProperties.class)
public class VertexAiConfig {


    @Autowired
    private VertexAiRagProperties properties;

    @Value("${google.cloud.credentials.location:}")
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
        if (credentialsResource != null && credentialsResource.exists() && credentialsResource.getFilename() != null && !credentialsResource.getFilename().isEmpty()) {
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
    public   Map<Integer, SystemPrompt> systemPrompt() throws IOException {
        Map<Integer, SystemPrompt> systemPrompt = new HashMap<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Pattern pattern = Pattern.compile("system_prompt_(\\d+)$"); // no .txt at the end

        Resource[] resources = resolver.getResources("classpath*:system_prompt_*");
        for (Resource resource : resources) {
            String filename = Objects.requireNonNull(resource.getFilename());
            Matcher matcher = pattern.matcher(filename);
            if (matcher.matches()) {
                int superClubId = Integer.parseInt(matcher.group(1));
                try (InputStream is = resource.getInputStream()) {
                    String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    SystemPrompt sp = new SystemPrompt();
                    sp.setSystemPrompt(content);
                    systemPrompt.put(superClubId, sp);
                }
            } else {
                System.out.println("Skipping non-matching file: " + filename);
            }

        }
        return systemPrompt;
    }
}
