package no.java.incogito.web.resources;

import fj.F;
import fj.F2;
import fj.F3;
import static fj.Function.curry;
import fj.P2;
import fj.data.List;
import no.java.incogito.Enums;
import static no.java.incogito.Functions.compose;
import no.java.incogito.domain.Event;
import no.java.incogito.domain.IncogitoUri;
import no.java.incogito.domain.IncogitoUri.IncogitoEventsUri.IncogitoEventUri;
import no.java.incogito.domain.IncogitoUri.IncogitoRestEventsUri.IncogitoRestEventUri;
import no.java.incogito.domain.IncogitoUri.IncogitoRestEventsUri.IncogitoRestEventUri.IncogitoLabelsIconUri;
import no.java.incogito.domain.IncogitoUri.IncogitoRestEventsUri.IncogitoRestEventUri.IncogitoLevelsIconUri;
import no.java.incogito.domain.IncogitoUri.IncogitoRestEventsUri.IncogitoRestEventUri.IncogitoRestSessionUri;
import no.java.incogito.domain.Label;
import no.java.incogito.domain.Level;
import no.java.incogito.domain.Schedule;
import no.java.incogito.domain.Session;
import no.java.incogito.domain.SessionRating;
import no.java.incogito.domain.Speaker;
import no.java.incogito.domain.UserSessionAssociation;
import no.java.incogito.domain.WikiString;
import no.java.incogito.dto.EventXml;
import no.java.incogito.dto.InterestLevelXml;
import no.java.incogito.dto.LabelXml;
import no.java.incogito.dto.LevelXml;
import no.java.incogito.dto.ScheduleXml;
import no.java.incogito.dto.SessionRatingXml;
import no.java.incogito.dto.SessionXml;
import no.java.incogito.dto.SpeakerXml;
import no.java.incogito.dto.UserSessionAssociationXml;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * @author <a href="mailto:trygvis@java.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class XmlFunctions {
    private static final DatatypeFactory datatypeFactory;

    static {
        try {
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static F<Interval, DateTime> Interval_start = new F<Interval, DateTime>() {
        public DateTime f(Interval interval) {
            return interval.getStart();
        }
    };

    public static F<Interval, DateTime> Interval_end = new F<Interval, DateTime>() {
        public DateTime f(Interval interval) {
            return interval.getEnd();
        }
    };

    private static F<DateTime, XMLGregorianCalendar> toXmlGregorianCalendar = new F<DateTime, XMLGregorianCalendar>() {
        public XMLGregorianCalendar f(DateTime dateTime) {
            XMLGregorianCalendar gregorianCalendar = datatypeFactory.newXMLGregorianCalendar();
            gregorianCalendar.setTime(dateTime.getHourOfDay(), dateTime.getMinuteOfHour(), dateTime.getSecondOfMinute());
            gregorianCalendar.setYear(dateTime.getYear());
            gregorianCalendar.setMonth(dateTime.getMonthOfYear());
            gregorianCalendar.setDay(dateTime.getDayOfMonth());
            return gregorianCalendar;
        }
    };

    private static F<IncogitoRestSessionUri, F<P2<Speaker, Integer>, SpeakerXml>> speakerToXml = curry(new F2<IncogitoRestSessionUri, P2<Speaker, Integer>, SpeakerXml>() {
        public SpeakerXml f(IncogitoRestSessionUri incogitoRestSessionUri, P2<Speaker, Integer> item) {
            Speaker speaker = item._1();
            return new SpeakerXml(speaker.name,
                    speaker.bio.map(WikiString.toHtml),
                    incogitoRestSessionUri.speakerPhoto(item._2()));
        }
    });

    private static F<IncogitoLabelsIconUri, F<Label, LabelXml>> labelToXml = curry(new F2<IncogitoLabelsIconUri, Label, LabelXml>() {
        public LabelXml f(IncogitoLabelsIconUri incogitoLabelsIconUri, Label label) {
            return new LabelXml(label.id, label.displayName, incogitoLabelsIconUri.png(label));
        }
    });

    private static F<IncogitoLevelsIconUri, F<Level, LevelXml>> levelToXml = curry(new F2<IncogitoLevelsIconUri, Level, LevelXml>() {
        public LevelXml f(IncogitoLevelsIconUri levelsIconUri, Level level) {
            return new LevelXml(level.id.name(), level.displayName, levelsIconUri.png(level));
        }
    });

    public static final F<IncogitoRestEventUri, F<IncogitoEventUri, F<Session, SessionXml>>> sessionToXml = curry(new F3<IncogitoRestEventUri, IncogitoEventUri, Session, SessionXml>() {
        public SessionXml f(IncogitoRestEventUri restEventUri, IncogitoEventUri eventUri, Session session) {
            IncogitoRestSessionUri restSessionUri = restEventUri.session(session);
            System.out.println("session.audioAttachment.map(restSessionUri.attachmentUrl) = " + session.audioAttachment.map(restSessionUri.attachmentUrl));
            System.out.println("session.videoAttachment.map(restSessionUri.attachmentUrl) = " + session.videoAttachment.map(restSessionUri.attachmentUrl));
            return new SessionXml(restSessionUri.toString(),
                    eventUri.session(session),
                    SessionXml.FormatXml.valueOf(session.format.name()),
                    session.id.value,
                    session.title,
                    session.body.map(WikiString.toHtml),
                    session.level.map(levelToXml.f(restEventUri.levelsIcon())),
                    session.room,
                    session.timeslot.map(compose(toXmlGregorianCalendar, Interval_start)),
                    session.timeslot.map(compose(toXmlGregorianCalendar, Interval_end)),
                    session.speakers.zipIndex().map(speakerToXml.f(restSessionUri)),
                    session.labels.map(labelToXml.f(restEventUri.labelsIcon())),
                    restSessionUri.addAttachmentUrl(),
                    session.pdfAttachment.map(restSessionUri.attachmentUrl),
                    session.audioAttachment.map(restSessionUri.attachmentUrl),
                    session.videoAttachment.map(restSessionUri.attachmentUrl));
        }
    });

    public static final F<UserSessionAssociation, UserSessionAssociationXml> sessionAssociationToXml = new F<UserSessionAssociation, UserSessionAssociationXml>() {
        public UserSessionAssociationXml f(UserSessionAssociation userSessionAssociation) {
            return new UserSessionAssociationXml(userSessionAssociation.sessionId.value,
                    userSessionAssociation.rating.map(Enums.<SessionRating>toString_()).bind(SessionRatingXml.valueOf_).orSome(SessionRatingXml.NOT_SET),
                    userSessionAssociation.ratingComment,
                    InterestLevelXml.valueOf(userSessionAssociation.interestLevel.name()));
        }
    };

    public static final F<IncogitoRestEventUri, F<IncogitoEventUri, F<Schedule, ScheduleXml>>> scheduleToXml = curry(new F3<IncogitoRestEventUri, IncogitoEventUri, Schedule, ScheduleXml>() {
        public ScheduleXml f(IncogitoRestEventUri restEventUri, IncogitoEventUri eventUri, Schedule schedule) {
            return new ScheduleXml(schedule.sessions.map(sessionToXml.f(restEventUri).f(eventUri)),
                    schedule.sessionAssociations.values().map(sessionAssociationToXml));
        }
    });

    public static final F<IncogitoUri, F<Event, EventXml>> eventToXml = curry(new F2<IncogitoUri, Event, EventXml>() {
        public EventXml f(IncogitoUri incogitoUri, final Event event) {
            IncogitoRestEventUri restEventUri = incogitoUri.restEvents().eventUri(event.name);
            IncogitoEventUri eventUri = incogitoUri.events().eventUri(event.name);
            return new EventXml(restEventUri.toString(),
                    event.id.toString(),
                    event.name,
                    event.blurb,
                    event.frontpageContent,
                    eventUri.toString(),
                    eventUri.sessionListHtml(),
                    eventUri.calendarHtml(),
                    event.labels.map(labelToXml.f(restEventUri.labelsIcon())),
                    event.levels.values().map(levelToXml.f(restEventUri.levelsIcon())));
        }
    });

    public static final F<IncogitoUri, F<List<Event>, List<EventXml>>> eventListToXml = new F<IncogitoUri, F<List<Event>, List<EventXml>>>() {
        public F<List<Event>, List<EventXml>> f(IncogitoUri incogitoUri) {
            return List.<Event, EventXml>map_().f(XmlFunctions.eventToXml.f(incogitoUri));
        }
    };
}
