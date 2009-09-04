package no.java.incogito.domain;

import fj.data.Option;
import static fj.data.Option.none;
import static fj.data.Option.fromNull;

public class Attachment {
    public final String fileName;
    public final ContentType contentType;
    public final long size;

    public Attachment(String fileName, ContentType contentType, long size) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.size = size;
    }
}
