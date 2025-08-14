package org.mobitti.helpers;

public class ContextItem {

   private  String title;
   private  String uri;
   private  String page;
   private  String text;

   public  ContextItem(String title, String uri, String page, String text) {
        this.title = title == null ? "" : title;
        this.uri = uri == null ? "" : uri;
        this.page = page == null ? "" : page;
        this.text = text == null ? "" : text;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
