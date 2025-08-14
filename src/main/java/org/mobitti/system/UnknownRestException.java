package org.mobitti.system;

public class UnknownRestException extends RestException {
    public UnknownRestException() {
        this.statusDoc = new Status(ErrorCodes.UNKNOWN_EXCEPTION, "Unknown Error");
    }

    public UnknownRestException(String developmentErrorMessage) {
        this.statusDoc = new Status(ErrorCodes.UNKNOWN_EXCEPTION, "Unknown Error", developmentErrorMessage);
    }

    public UnknownRestException(String developmentErrorMessage, String errorCode, String errorName) {
        this.statusDoc = new Status(errorCode, errorName, developmentErrorMessage);
    }
}
