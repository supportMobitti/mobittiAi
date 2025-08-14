package org.mobitti.helpers;

public class SystemPrompt {
    private String systemPrompt;


    // Default constructor
    public SystemPrompt() {
    }

    // Constructor with system prompt
    public SystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    // Getter for system prompt
    public String getSystemPrompt() {
        return systemPrompt;
    }

    // Setter for system prompt
    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }
}
