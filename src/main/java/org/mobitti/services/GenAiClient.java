package org.mobitti.services;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.genai.types.HttpOptions;
import org.mobitti.config.VertexAiRagProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.google.genai.Client;

import java.util.HashMap;
import java.util.Map;

@Component
public class GenAiClient {

    private Map<Integer,Client> clients = new HashMap<>();

    @Autowired
    private GoogleCredentials googleCredentials;

    @Autowired
    private VertexAiRagProperties properties;


    public Client getClient(int superClubId) {
        return clients.computeIfAbsent(superClubId, id -> {;
            Client client = new Client.Builder()
                    .credentials(googleCredentials)
                    .location(properties.getLocation())
                    .project(properties.getProjectId())
                    .vertexAI(true)
                    .httpOptions(HttpOptions.builder().apiVersion("v1").build())
                    .build();
            return client;
        });
    }




    private String getProjectId(int superClubId) {
        switch (superClubId){
            case 63:
                return "hmc";
        }
        throw new IllegalArgumentException("Unknown super club ID: " + superClubId);
    }
}
