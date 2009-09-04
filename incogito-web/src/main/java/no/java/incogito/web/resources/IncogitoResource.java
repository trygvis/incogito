package no.java.incogito.web.resources;

import fj.F;
import fj.F2;
import static fj.Function.curry;
import fj.P1;
import fj.P2;
import fj.control.parallel.Callables;
import fj.data.Either;
import fj.data.Java;
import fj.data.List;
import fj.data.Option;
import static fj.data.Option.fromString;
import static fj.data.Option.join;
import static fj.data.Option.some;
import no.java.incogito.Functions;
import static no.java.incogito.Functions.compose;
import no.java.incogito.IO;
import no.java.incogito.PatternMatcher;
import no.java.incogito.application.IncogitoApplication;
import no.java.incogito.application.OperationResult;
import no.java.incogito.application.OperationResult.NotFoundOperationResult;
import no.java.incogito.application.OperationResult.OkOperationResult;
import static no.java.incogito.application.OperationResult.fromOption;
import no.java.incogito.domain.ContentType;
import no.java.incogito.domain.Event;
import no.java.incogito.domain.IncogitoUri;
import no.java.incogito.domain.IncogitoUri.IncogitoEventsUri.IncogitoEventUri;
import no.java.incogito.domain.IncogitoUri.IncogitoRestEventsUri.IncogitoRestEventUri;
import no.java.incogito.domain.Label;
import no.java.incogito.domain.Level;
import no.java.incogito.domain.Level.LevelId;
import no.java.incogito.domain.Room;
import no.java.incogito.domain.Session;
import no.java.incogito.domain.SessionId;
import no.java.incogito.domain.User;
import no.java.incogito.domain.UserSessionAssociation.InterestLevel;
import static no.java.incogito.dto.EventListXml.eventListXml;
import no.java.incogito.dto.EventXml;
import no.java.incogito.dto.IncogitoXml;
import no.java.incogito.dto.SessionListXml;
import no.java.incogito.dto.SessionXml;
import no.java.incogito.web.WebFunctions;
import static no.java.incogito.web.resources.XmlFunctions.eventToXml;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.status;
import javax.ws.rs.core.SecurityContext;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

/**
 * REST-ful wrapper around IncogitoApplication.
 * <p/>
 * TODO: Add checks on every method that uses SecurityContext to check for nulls.
 *
 * @author <a href="mailto:trygvis@java.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
@Component
@Path("/rest")
@Produces({"application/xml", "application/json"})
@SuppressWarnings({"UnusedDeclaration"})
public class IncogitoResource {

    private final IncogitoApplication incogito;

    @Autowired
    public IncogitoResource(IncogitoApplication incogito) {
        this.incogito = incogito;
    }

    @GET
    public Response getIncogito(@Context SecurityContext securityContext) {
        return Response.ok(new IncogitoXml(incogito.getConfiguration().baseurl, getUserName.f(securityContext))).build();
    }

    @Path("/events")
    @GET
    public Response getEvents() {
        F<List<Event>, List<EventXml>> eventListToXml = XmlFunctions.eventListToXml.f(new IncogitoUri(incogito.getConfiguration().baseurl));

        return toJsr311(incogito.getEvents().
                ok().map(compose(eventListXml, compose(Java.<EventXml>List_ArrayList(), eventListToXml))));
    }

    @Path("/events/{eventName}/calendar.css")
    @GET
    @Produces("text/css")
    public Response getEventCalendarCss(@PathParam("eventName") final String eventName) {
        final F<List<Room>, List<String>> generateCss = WebFunctions.generateCalendarCss.f(incogito.getConfiguration().cssConfiguration);

        return toJsr311(incogito.getEventByName(eventName).ok().map(new F<Event, String>() {
            public String f(Event event) {
                return generateCss.f(event.presentationRooms).foldRight(Functions.String_join.f("\n"), "");
            }
        }), cacheForOneHourCacheControl);
    }

    @Path("/events/{eventName}/session.css")
    @GET
    @Produces("text/css")
    public Response getEventSessionCss(@PathParam("eventName") final String eventName) {
        final F<Event, List<String>> generateSessionCss = WebFunctions.generateSessionCss.f(incogito.getConfiguration());

        return toJsr311(incogito.getEventByName(eventName).ok().map(new F<Event, String>() {
            public String f(Event event) {
                return generateSessionCss.f(event).foldRight(Functions.String_join.f("\n"), "");
            }
        }), cacheForOneHourCacheControl);
    }

    @Path("/events/{eventName}/icons/levels/{level}.png")
    @GET
    @Produces("image/png")
    public Response getLevelIcon(@PathParam("eventName") final String eventName,
                                 @PathParam("level") final String level) {
        OperationResult<Event> eventResult = incogito.getEventByName(eventName);

        if (!eventResult.isOk()) {
            return toJsr311(eventResult);
        }

        Option<LevelId> levelOption = some(level).bind(Level.LevelId.valueOf);

        if (!levelOption.isSome()) {
            return toJsr311(OperationResult.<Object>notFound("Level '" + level + "' not known."));
        }

        Option<File> fileOption = eventResult.value().levels.get(levelOption.some()).map(Level.iconFile_);

        if (!fileOption.isSome()) {
            return toJsr311(OperationResult.<Object>notFound("No icon for level '" + level + "'."));
        }

        // TODO: How about some caching here?

        Option<byte[]> bytes = join(fileOption.
                map(IO.<byte[]>runFileInputStream_().f(IO.ByteArrays.streamToByteArray)).
                map(compose(P1.<Option<byte[]>>__1(), Callables.<byte[]>option())));

        return toJsr311(fromOption(bytes, "Unable to read level file."), cacheForOneHourCacheControl);
    }

    @Path("/events/{eventName}/icons/labels/{label}.png")
    @GET
    @Produces("image/png")
    public Response getLabelIcon(@PathParam("eventName") final String eventName,
                                 @PathParam("label") final String label) {
        OperationResult<Event> eventResult = incogito.getEventByName(eventName);

        if (!eventResult.isOk()) {
            return toJsr311(eventResult);
        }

        Option<File> fileOption = eventResult.value().labelMap.get(label).map(Label.iconFile_);

        if (!fileOption.isSome()) {
            return toJsr311(OperationResult.<Object>notFound("No icon for label '" + label + "'."));
        }

        // TODO: How about some caching here?

        Option<byte[]> bytes = join(fileOption.
                map(IO.<byte[]>runFileInputStream_().f(IO.ByteArrays.streamToByteArray)).
                map(compose(P1.<Option<byte[]>>__1(), Callables.<byte[]>option())));

        return toJsr311(fromOption(bytes, "Unable to read label file."), cacheForOneHourCacheControl);
    }

    @Path("/events/{eventName}")
    @GET
    public Response getEvent(@PathParam("eventName") final String eventName) {
        return toJsr311(incogito.getEventByName(eventName).ok().map(eventToXml.f(new IncogitoUri(incogito.getConfiguration().baseurl))));
    }

    @Path("/events/{eventName}/sessions")
    @GET
    public Response getSessionsForEvent(@PathParam("eventName") final String eventName) {
        IncogitoUri incogitoUri = new IncogitoUri(incogito.getConfiguration().baseurl);
        F<List<Session>, List<SessionXml>> sessionToXmlList = List.<Session, SessionXml>map_().
                f(XmlFunctions.sessionToXml.f(incogitoUri.restEvents().eventUri(eventName)).f(incogitoUri.events().eventUri(eventName)));

        return toJsr311(incogito.getSessions(eventName).
                ok().map(compose(SessionListXml.sessionListXml, sessionToXmlList)));
    }

    @Path("/events/{eventName}/sessions/{sessionId}")
    @GET
    public Response getSessionForEvent(@PathParam("eventName") final String eventName,
                                       @PathParam("sessionId") final String sessionId) {
        IncogitoUri incogitoUri = new IncogitoUri(incogito.getConfiguration().baseurl);
        IncogitoRestEventUri restEventUri = incogitoUri.restEvents().eventUri(eventName);
        IncogitoEventUri eventUri = incogitoUri.events().eventUri(eventName);

        // TODO: Consider replacing this with the configured host name and base url
        F<Session, SessionXml> sessionToXml = XmlFunctions.sessionToXml.f(restEventUri).f(eventUri);

        return toJsr311(incogito.getSession(eventName, new SessionId(sessionId)).
                ok().map(sessionToXml));
    }

    @Path("/events/{eventName}/sessions/{sessionId}/speaker-photos/{index}")
    @GET
    public Response getPersonPhoto(@PathParam("eventName") final String eventName,
                                   @PathParam("sessionId") final String sessionId,
                                   @PathParam("index") final int index) {
        return toJsr311(incogito.getSpeakerPhotoForSession(sessionId, index), cacheForOneHourCacheControl);
    }

    @Path("/events/{eventName}/sessions/{sessionId}/attachments/{fileName}")
    @GET
    public Response getAttachment(@PathParam("eventName") final String eventName,
                                  @PathParam("sessionId") final String sessionId,
                                  @PathParam("fileName") final String filename) {
/*
        Either<String, P2<ContentType, byte[]>> result = incogito.getAttachmentForSession(sessionId, filename);

        if (result.isLeft()) {
            return toJsr311(OperationResult.<Object>notFound(result.left().value()));
        }

        P2<ContentType, byte[]> p = result.right().value();

        return toJsr311(OperationResult.ok(p._2()), compose(cacheForOneHourCacheControl, type.f(p._1())));
*/
/*
        Either<String, P2<ContentType, URI>> result = incogito.getAttachmentForSession(sessionId, filename);

        if (result.isLeft()) {
            return toJsr311(OperationResult.<Object>notFound(result.left().value()));
        }

        return temporaryRedirect(result.right().value()._2()).
                type(MediaType.valueOf(result.right().value()._1().toString())).
                build();
*/
        Either<String, P2<ContentType, URI>> result = incogito.getAttachmentForSession(sessionId, filename);

        if (result.isLeft()) {
            return toJsr311(OperationResult.<Object>notFound(result.left().value()));
        }

        try {
            String mediaType = result.right().value()._1().toString();

            if (filename.endsWith("audio.mp4")) {
                mediaType = "audio/mpeg";
            } else if (filename.endsWith(".mp4")) {
                mediaType = "video/mpeg";
            }

            return Response.ok(result.right().value()._2().toURL().openStream()).
                type(MediaType.valueOf(mediaType)).
                build();
        } catch (IOException e) {
            return status(Status.INTERNAL_SERVER_ERROR).
                    type(MediaType.TEXT_PLAIN_TYPE).
                    entity(e.getMessage()).
                    build();
        }
    }

//    @POST
//    @Path("/events/{eventName}/sessions/{sessionId}/attachments")
//    @Produces("application/json")
//    @Consumes("application/json")
//    public Response addAttachment(@PathParam("eventName") final String eventName,
//                                  @PathParam("sessionId") final String sessionId,
//                                  @Context UriInfo uriInfo,
//                                  AttachmentXml attachmentXml) {
//        Option<Response> responseOption = isValidAttachment(attachmentXml);
//        if (responseOption.isSome()) {
//            return responseOption.some();
//        }
//
//        Attachment attachment = new Attachment(attachmentXml.getFileName(),
//                new ContentType(attachmentXml.getContentType()),
//                attachmentXml.getSize(),
//                attachmentXml.getAttachmentUri());
//        OperationResult<Unit> result = incogito.addAttachment(sessionId, attachment);
//
//        URI uri = uriInfo.getRequestUriBuilder().segment(attachmentXml.getFileName()).build();
//
//        return this.<String>defaultResponsePatternMatcher().
//                add(OkOperationResult.class, this.<OperationResult<String>>createdWithUri().f(uri)).
//                match(result.ok().map(Function.<Unit, String>constant("Updated")));
//    }

//    @PUT
//    @Path("/events/{eventName}/sessions/{sessionId}/attachments/{fileName}")
//    @Produces("application/json")
//    @Consumes("application/json")
//    public Response updateAttachment(@PathParam("eventName") final String eventName,
//                                     @PathParam("sessionId") final String sessionId,
//                                     @PathParam("fileName") final String fileName,
//                                     AttachmentXml attachmentXml) {
//        Option<Response> responseOption = isValidAttachment(attachmentXml);
//        if (responseOption.isSome()) {
//            return responseOption.some();
//        }
//
//        if(!attachmentXml.getFileName().equals(fileName)) {
//            ResponseBuilder builder = status(Status.BAD_REQUEST).
//                    header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN).
//                    entity("!attachmentXml.getFileName().equals(fileName)");
//            return builder.build();
//        }
//
//        System.out.println("attachmentXml.getAttachmentUri() = " + attachmentXml.getAttachmentUri());
//        Attachment attachment = new Attachment(attachmentXml.getFileName(),
//                new ContentType(attachmentXml.getContentType()),
//                attachmentXml.getSize(),
//                attachmentXml.getAttachmentUri());
//        OperationResult<Unit> result = incogito.updateAttachment(sessionId, attachment);
//
//        return toJsr311(result.ok().map(Function.<Unit, String>constant("Updated")));
//    }

//    private Option<Response> isValidAttachment(AttachmentXml attachmentXml) {
//        ResponseBuilder builder = status(Status.BAD_REQUEST).
//                header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN);
//
//        if (StringUtils.isEmpty(attachmentXml.getFileName())) {
//            return some(builder.entity("filename is empty").build());
//
//        }
//        if (StringUtils.isEmpty(attachmentXml.getAttachmentUri())) {
//            return some(builder.entity("attachment uri is empty").build());
//
//        }
//        if (attachmentXml.getSize() <= 0) {
//            return some(builder.entity("size is 0").build());
//
//        }
//        if (StringUtils.isEmpty(attachmentXml.getContentType())) {
//            return some(builder.entity("content type is empty").build());
//
//        }
//
//        return Option.none();
//    }

    @Path("/events/{eventName}/{sessionId}/session-interest")
    @POST
    public Response setSessionInterest(@Context final SecurityContext securityContext,
                                       @PathParam("eventName") final String eventName,
                                       @PathParam("sessionId") final String sessionId,
                                       String payload) {

        Option<String> userName = getUserName.f(securityContext);

        if (userName.isNone()) {
            return status(Status.UNAUTHORIZED).build();
        }

        OperationResult<User> result = incogito.setInterestLevel(userName.some(),
                eventName,
                new SessionId(sessionId),
                InterestLevel.valueOf(payload));

        return this.<User>defaultResponsePatternMatcher().
                add(OkOperationResult.class, this.<OperationResult<User>>created()).
                match(result);
    }

    @Path("/events/{eventName}/my-schedule")
    @GET
    public Response getMySchedule(@Context final SecurityContext securityContext,
                                  @PathParam("eventName") final String eventName) {
        String name = securityContext.getUserPrincipal().getName();

        if (name == null) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }

        return getScheduleForUser(securityContext, eventName, name);
    }

    @Path("/events/{eventName}/schedules/{userName}")
    @GET
    public Response getScheduleForUser(@Context final SecurityContext securityContext,
                                       @PathParam("eventName") final String eventName,
                                       @PathParam("userName") final String userName) {
        IncogitoUri incogitoUri = new IncogitoUri(incogito.getConfiguration().baseurl);
        IncogitoRestEventUri restEventUri = incogitoUri.restEvents().eventUri(eventName);
        IncogitoEventUri eventUri = incogitoUri.events().eventUri(eventName);

        return toJsr311(incogito.getSchedule(eventName, userName).ok().
                map(XmlFunctions.scheduleToXml.f(restEventUri).f(eventUri)));
    }

    // -----------------------------------------------------------------------
    //
    // -----------------------------------------------------------------------

    private <T> F<T, ResponseBuilder> ok() {
        return new F<T, ResponseBuilder>() {
            public ResponseBuilder f(T operationResult) {
                Object value = ((OperationResult) operationResult).value();
                ToStringBuilder.reflectionToString(value, ToStringStyle.MULTI_LINE_STYLE);
                return Response.ok(operationResult);
            }
        };
    }

    private F<CacheControl, F<ResponseBuilder, ResponseBuilder>> cacheControl = curry(new F2<CacheControl, ResponseBuilder, ResponseBuilder>() {
        public ResponseBuilder f(CacheControl cacheControl, ResponseBuilder responseBuilder) {
            return responseBuilder.cacheControl(cacheControl);
        }
    });

    private F<ContentType, F<ResponseBuilder, ResponseBuilder>> type = curry(new F2<ContentType, ResponseBuilder, ResponseBuilder>() {
        public ResponseBuilder f(ContentType contentType, ResponseBuilder responseBuilder) {
            return responseBuilder.type(contentType.toString());
        }
    });

    private final F<ResponseBuilder, ResponseBuilder> cacheForOneHourCacheControl = cacheControl.f(createCacheControl(3600));

    private F<ResponseBuilder, Response> build = new F<ResponseBuilder, Response>() {
        public Response f(ResponseBuilder responseBuilder) {
            return responseBuilder.build();
        }
    };

    private <T> F<URI, F<T, Response>> createdWithUri() {
        return curry( new F2<URI, T, Response>() {
            public Response f(URI uri, T t) {
                return status(Status.CREATED).
                        header("Location", uri.toString()).
                        build();
            }
        });
    }

    private <T> F<T, Response> created() {
        return new F<T, Response>() {
            public Response f(T operationResult) {
                return status(Status.CREATED).build();
            }
        };
    }

    private <T> F<T, Response> notFound() {
        return new F<T, Response>() {
            public Response f(T operationResult) {
                NotFoundOperationResult o = (NotFoundOperationResult) operationResult;

                return status(NOT_FOUND).
                        type(MediaType.TEXT_PLAIN).
                        entity(o.message + "\n").
                        build();
            }
        };
    }

    private <T> PatternMatcher<OperationResult<T>, Response> defaultResponsePatternMatcher() {
        return PatternMatcher.<OperationResult<T>, Response>match().
                add(NotFoundOperationResult.class, this.<OperationResult<T>>notFound());
    }

    private <T> Response toJsr311(OperationResult<T> result) {
        return this.<T>defaultResponsePatternMatcher().
                add(OkOperationResult.class, compose(build, this.<OperationResult<T>>ok())).
                match(result);
    }

    private <T> Response toJsr311(OperationResult<T> result, F<Response.ResponseBuilder, Response.ResponseBuilder> map) {
        return this.<T>defaultResponsePatternMatcher().
                add(OkOperationResult.class, compose(build, map, this.<OperationResult<T>>ok())).
                match(result);
    }

    private F<SecurityContext, Option<String>> getUserName = new F<SecurityContext, Option<String>>() {
        public Option<String> f(SecurityContext securityContext) {
            return securityContext.getUserPrincipal() == null ? Option.<String>none() : fromString(securityContext.getUserPrincipal().getName());
        }
    };

    private CacheControl createCacheControl(int time) {
        CacheControl cacheForOneHourCacheControl = new CacheControl();
        cacheForOneHourCacheControl.setMaxAge(time);
        return cacheForOneHourCacheControl;
    }
}
