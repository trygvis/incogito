package no.java.incogito.dto;

public class AttachmentXml {
    public String selfUri;
    public String fileName;
    public String contentType;
    public long size;
    public String attachmentUri;

    public AttachmentXml() {
    }

    public AttachmentXml(String selfUri, String fileName, String contentType, long size, String attachmentUri) {
        this.selfUri = selfUri;
        this.fileName = fileName;
        this.contentType = contentType;
        this.size = size;
        this.attachmentUri = attachmentUri;
    }

    public String getSelfUri() {
        return selfUri;
    }

    public void setSelfUri(String selfUri) {
        this.selfUri = selfUri;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getAttachmentUri() {
        return attachmentUri;
    }

    public void setAttachmentUri(String attachmentUri) {
        this.attachmentUri = attachmentUri;
    }
}
