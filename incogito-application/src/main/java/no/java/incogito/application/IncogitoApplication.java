package no.java.incogito.application;

import fj.P2;
import fj.Unit;
import fj.data.Either;
import fj.data.List;
import fj.data.Option;
import no.java.incogito.domain.Attachment;
import no.java.incogito.domain.ContentType;
import no.java.incogito.domain.Event;
import no.java.incogito.domain.Schedule;
import no.java.incogito.domain.Session;
import no.java.incogito.domain.SessionId;
import no.java.incogito.domain.User;
import no.java.incogito.domain.UserSessionAssociation.InterestLevel;

import java.net.URI;

/**
 * @author <a href="mailto:trygve.laugstol@arktekk.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public interface IncogitoApplication {
    IncogitoConfiguration getConfiguration();

    void reloadConfiguration() throws Exception;

    OperationResult<List<Event>> getEvents();

    OperationResult<Event> getEventByName(String eventName);

    OperationResult<List<Session>> getSessions(String eventName);

    OperationResult<Session> getSession(String eventName, SessionId sessionId);

    OperationResult<Session> getSessionByTitle(String eventName, String sessionTitle);

    OperationResult<User> createUser(User user);

    OperationResult<Unit> removeUser(no.java.incogito.domain.User.UserId userId);

    OperationResult<User> getUser(no.java.incogito.domain.User.UserId userId);

    OperationResult<Schedule> getSchedule(String eventName, String userId);

    OperationResult<Schedule> getSchedule(String eventName, Option<String> userId);

    OperationResult<User> setInterestLevel(String userName, String eventName, SessionId sessionId, InterestLevel interestLevel);

    OperationResult<byte[]> getSpeakerPhotoForSession(String sessionId, int index);

    Either<String, P2<ContentType, URI>> getAttachmentForSession(String sessionId, String filename);

//    OperationResult<Unit> addAttachment(String sessionId, Attachment attachment);
//
//    OperationResult<Unit> updateAttachment(String sessionId, Attachment attachment);

    void clearSessionCache(String sessionId);
}
