package org.mobitti.system;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;


public class Status implements Serializable {
    private static final long serialVersionUID = 1854022049975318284L;

    private String errorCode;
    private String errorMessage;
    private String developerErrorMessage = "";

    public Status(String errorCode, String errorMssage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMssage;
        this.developerErrorMessage = "";
    }

    public Status(String errorCode, String errorMessage, String developerErrorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.developerErrorMessage = developerErrorMessage == null ? "" : developerErrorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getDeveloperErrorMessage() {
        return developerErrorMessage;
    }

    public void setDeveloperErrorMessage(String developerErrorMessage) {
        this.developerErrorMessage = developerErrorMessage;
    }
}
