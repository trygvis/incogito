package no.java.incogito.domain;

public class ContentType {
    private final String contentType;

    public ContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ContentType that = (ContentType) o;

        return contentType.equals(that.contentType);
    }

    @Override
    public int hashCode() {
        return contentType.hashCode();
    }

    public String toString() {
        return contentType;
    }
}
