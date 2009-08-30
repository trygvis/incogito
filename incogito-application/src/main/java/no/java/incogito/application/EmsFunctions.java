package no.java.incogito.application;

import fj.F;
import fj.F2;
import static fj.Function.compose;
import static fj.Function.curry;
import static fj.Function.flip;
import fj.data.List;
import static fj.data.List.iterableList;
import fj.data.Option;
import static fj.data.Option.fromNull;
import static fj.data.Option.fromString;
import static fj.data.Option.none;
import static fj.data.Option.some;
import static fj.data.Option.somes;
import fj.pre.Show;
import no.java.ems.domain.Event;
import no.java.incogito.Enums;
import no.java.incogito.Functions;
import no.java.incogito.application.IncogitoConfiguration.EventConfiguration;
import no.java.incogito.domain.Comment;
import no.java.incogito.domain.Event.EventId;
import static no.java.incogito.domain.Event.EventId.eventId;
import no.java.incogito.domain.Label;
import no.java.incogito.domain.Level;
import no.java.incogito.domain.Level.LevelId;
import no.java.incogito.domain.Room;
import no.java.incogito.domain.Session;
import no.java.incogito.domain.SessionId;
import no.java.incogito.domain.Speaker;
import no.java.incogito.domain.WikiString;

/**
 * Functions from EMS domain objects to Incogito domain objects.
 *
 * @author <a href="mailto:trygve.laugstol@arktekk.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class EmsFunctions {
    public static F<Event, EventId> eventIdFromEms = new F<no.java.ems.domain.Event, EventId>() {
        public EventId f(no.java.ems.domain.Event event) {
            return eventId(event.getId());
        }
    };

    public static F<no.java.ems.domain.Room, Room> roomFromEms = new F<no.java.ems.domain.Room, Room>() {
        public Room f(no.java.ems.domain.Room room) {
            return new Room(room.getName());
        }
    };

    public static F<IncogitoConfiguration, F<no.java.ems.domain.Event, Option<no.java.incogito.domain.Event>>> eventFromEms = curry(new F2<IncogitoConfiguration, Event, Option<no.java.incogito.domain.Event>>() {
        public Option<no.java.incogito.domain.Event> f(IncogitoConfiguration configuration, final no.java.ems.domain.Event event) {
            final EventId eventId = eventIdFromEms.f(event);

            return configuration.eventConfigurations.
                find(compose(Functions.equals.f(event.getName()), EventConfiguration.name_)).
                map(new F<EventConfiguration, no.java.incogito.domain.Event>() {
                    public no.java.incogito.domain.Event f(EventConfiguration eventConfiguration) {
                        return new no.java.incogito.domain.Event(eventId,
                            event.getName(),
                            eventConfiguration.blurb,
                            eventConfiguration.frontPageText,
                            List.iterableList(event.getRooms()).map(roomFromEms),
                            eventConfiguration.levels,
                            eventConfiguration.labels);
                    }
                });
        }
    });

    public static F<no.java.ems.domain.Speaker, Speaker> speakerFromEms = new F<no.java.ems.domain.Speaker, Speaker>() {
        public Speaker f(no.java.ems.domain.Speaker speaker) {
            return new Speaker(speaker.getName(), speaker.getPersonId(), fromString(speaker.getDescription()).map(WikiString.constructor));
        }
    };

    public static F<no.java.incogito.domain.Event, F<no.java.ems.domain.Session, Option<Session>>> sessionFromEms = curry(new F2<no.java.incogito.domain.Event, no.java.ems.domain.Session, Option<Session>>() {
        public Option<Session> f(no.java.incogito.domain.Event event, no.java.ems.domain.Session session) {
            if (session.getTitle() == null) {
                return none();
            }

            // Hack for now until ';' is encoded in url properly
            if (session.getTitle().indexOf(';') > 0) {
                return none();
            }

            Option<LevelId> levelId = fromNull(session.getLevel()).
                bind(Functions.compose(LevelId.valueOf, Enums.<no.java.ems.domain.Session.Level>name_()));

            F<LevelId, Option<Level>> getLevel = flip(Functions.<LevelId, Level>TreeMap_get()).f(event.levels);
            F<String, Option<Label>> getLabel = flip(Functions.<String, Label>TreeMap_get()).f(event.emsIndexedLabels);

            return some(new Session(new SessionId(session.getId()),
                fromNull(session.getFormat()).bind(compose(Session.Format.valueOf_, Show.<no.java.ems.domain.Session.Format>anyShow().showS_())).orSome(Session.Format.Presentation),
                session.getTitle(),
                fromString(session.getLead()).map(WikiString.constructor),
                fromString(session.getBody()).map(WikiString.constructor),
                levelId.bind(getLevel),
                fromNull(session.getTimeslot()),
                fromNull(session.getRoom()).map(no.java.incogito.ems.client.EmsFunctions.roomName),
                somes(iterableList(session.getKeywords()).map(getLabel)),
                iterableList(session.getSpeakers()).map(speakerFromEms),
                List.<Comment>nil()));
        }
    });
}