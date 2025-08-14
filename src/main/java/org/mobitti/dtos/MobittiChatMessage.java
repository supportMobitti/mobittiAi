package org.mobitti.dtos;

public class MobittiChatMessage {

    public enum MessageType {
        AI,User,Assistant
    }
    private MessageType messageType;
    private String message;
    private String dateTime;

    public MobittiChatMessage(String dateTime, String message, MessageType messageType) {
        this.dateTime = dateTime;
        this.messageType = messageType;
        this.message = message;
    }


    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public String getDateTime() {
        return dateTime;
    }
    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }
}
