package no.java.incogito.domain;

import fj.F;
import fj.data.List;
import fj.data.Option;
import fj.pre.Equal;
import no.java.incogito.Enums;
import static no.java.incogito.domain.Session.Format.Presentation;
import org.joda.time.Interval;

/**
 * @author <a href="mailto:trygve.laugstol@arktekk.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class Session {
    public enum Format {
        Presentation,
        Quickie,
        BoF;

        public static Equal<Format> equal = Enums.<Format>equal();
        public static F<String, Option<Format>> valueOf_ = Enums.<Format>valueOf().f(Format.class);
        public static F<Format, String> toString = Enums.toString_();
    }

    public final SessionId id;
    public final Format format;
    public final String title;
    public final Option<WikiString> body;
    public final Option<Level> level;
    public final Option<Interval> timeslot;
    public final Option<String> room;
    public final List<Label> labels;
    public final List<Speaker> speakers;
    public final List<Comment> comments;
    public Option<Attachment> pdfAttachment;
    public Option<Attachment> audioAttachment;
    public Option<Attachment> videoAttachment;

    public Session(SessionId id, Format format, String title, Option<WikiString> body, Option<Level> level,
                   Option<Interval> timeslot, Option<String> room, List<Label> labels, List<Speaker> speakers,
                   List<Comment> comments, Option<Attachment> pdfAttachment, Option<Attachment> audioAttachment,
                   Option<Attachment> videoAttachment) {
        this.id = id;
        this.format = format;
        this.title = title;
        this.body = body;
        this.level = level;
        this.timeslot = timeslot;
        this.room = room;
        this.labels = labels;
        this.speakers = speakers;
        this.comments = comments;
        this.pdfAttachment = pdfAttachment;
        this.audioAttachment = audioAttachment;
        this.videoAttachment = videoAttachment;
    }

    public Session title(String title) {
        return new Session(id, format, title, body, level, timeslot, room, labels, speakers, comments, pdfAttachment, audioAttachment, videoAttachment);
    }

    public Session timeslot(Option<Interval> timeslot) {
        return new Session(id, format, title, body, level, timeslot, room, labels, speakers, comments, pdfAttachment, audioAttachment, videoAttachment);
    }

    public Session room(Option<String> room) {
        return new Session(id, format, title, body, level, timeslot, room, labels, speakers, comments, pdfAttachment, audioAttachment, videoAttachment);
    }

    public static Session emptySession(SessionId sessionId, Format format, String title) {
        return new Session(sessionId, format, title,
            Option.<WikiString>none(),
            Option.<Level>none(),
            Option.<Interval>none(),
            Option.<String>none(),
            List.<Label>nil(),
            List.<Speaker>nil(),
            List.<Comment>nil(),
            Option.<Attachment>none(),
            Option.<Attachment>none(),
            Option.<Attachment>none());
    }
}
