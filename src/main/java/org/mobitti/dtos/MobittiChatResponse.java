package org.mobitti.dtos;

import org.mobitti.system.Status;

public class MobittiChatResponse {
    private Status status = new Status("", "", "");
    private String data;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
