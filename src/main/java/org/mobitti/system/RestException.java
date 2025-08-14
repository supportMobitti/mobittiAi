package org.mobitti.system;

public class RestException extends Exception
{
    public Status statusDoc;

    public Status getStatusDoc() {
        return statusDoc;
    }

    public void setStatusDoc(Status statusDoc) {
        this.statusDoc = statusDoc;
    }

    @Override
    public String getMessage() {
        return statusDoc.getDeveloperErrorMessage();
    }
}