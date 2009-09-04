var InterestLevel = {
    ATTEND: "ATTEND",
    INTEREST: "INTEREST",
    NO_INTEREST: "NO_INTEREST"
}

function createConsole() {
    var names = ["log", "debug", "info", "warn", "error", "assert", "dir", "dirxml", "group", "groupEnd", "time", "timeEnd", "count", "trace", "profile", "profileEnd"];
    if (window.opera && !window.console) {
        window.console = {};
        for (var i = 0; i < names.length; ++i) {
            window.console[names[i]] = function() {
            }
        }

        window.console.info = function() {
            opera.postError(arguments);
        }
    }

    if (!window.console) {
        window.console = {};
        for (i = 0; i < names.length; ++i) {
            window.console[names[i]] = function() {
            }
        }
    }
}

function getEvents(success) {
    console.log("Fetching events...")
    var s = success
    $.ajax({
        dataType: "json",
        url: baseurl + "/rest/events",
        success: function(data) {
            var events = data.events;
            console.log("Got " + events.length + " events")
            s(events)
        }
    })
}

function getSessionsByEventName(eventName, success) {
    console.log("Fetching sessions for event " + eventName + "...")
    var s = success
    $.ajax({
        dataType: "json",
        url: baseurl + "/rest/events/" + eventName + "/sessions",
        success: function(data) {
            var sessions = data.sessions;
            console.log("Got " + sessions.length + " for event " + eventName + "...")
            s(sessions)
        }
    })
}

function getSession(eventName, sessionUrl, success) {
    console.log("Fetching " + sessionUrl + " for event " + eventName + "...")
    var s = success
    $.ajax({
        dataType: "json",
        url: sessionUrl,
        success: function(data) {
            console.log("Got '" + data.title + "' for event " + eventName)
            s(data)
        }
    })
}

function getMySchedule(eventName, success) {
    console.log("Fetching schedule for event " + eventName + "...")
    var s = success
    $.ajax({
        dataType: "json",
        url: baseurl + "/rest/events/" + eventName + "/my-schedule",
        success: function(data) {
            console.log("Got schedule for event " + eventName)
            s(data)
        }
    })
}

function getSchedule(eventName, userName, success) {
    console.log("Fetching " + userName + "' schedule for event " + eventName + "...")
    var s = success
    $.ajax({
        dataType: "json",
        url: baseurl + "/rest/events/" + eventName + "/schedules/" + userName,
        success: function(data) {
            console.log("Got " + userName + "' schedule for event " + eventName)
            s(data)
        }
    })
}

function updateInterest(eventName, sessionUrl, state, success, unauthorized) {
    console.log("Setting interest level on " + sessionId + " for event " + eventName + " to " + state + "...")

    $.ajax({
        dataType: "json",
        url: sessionUrl,
        type: "POST",
        contentType: "application/json",
        data: state,
        complete: function(xhr) {
            switch (xhr.status) {
                case 201:
                    console.log("Updated interest level on " + sessionId + " for event " + eventName)
                    if (typeof success == "function")
                        success()
                    break;
                case 401:
                    console.log("Unauthorized")
                    if (typeof unauthorized == "function")
                        unauthorized()
                    break;
            }
        }
    })
}

function addAttachment(url, fileName, contentType, size, uri, success, fail) {
    console.log("Adding attachment: fileName=" + fileName + ", contentType=" + contentType + ", size=" + size + ": " + url)

    return mainpulateAttachment(url, fileName, contentType, size, uri, success, fail, "POST");
}

function updateAttachment(url, fileName, contentType, size, uri, success, fail) {
    console.log("Update attachment: fileName=" + fileName + ", contentType=" + contentType + ", size=" + size + ": " + url)

    return mainpulateAttachment(url, fileName, contentType, size, uri, success, fail, "PUT");
}

function mainpulateAttachment(url, fileName, contentType, size, uri, success, fail, method) {
    $.ajax({
        dataType: "json",
        url: url,
        type: method,
        contentType: "application/json",
        data: $.json.encode({fileName: fileName, contentType: contentType, size: size, attachmentUri: uri}),
        complete: function(xhr) {
            switch (xhr.status) {
                case 200:
                case 201:
                    console.log("Attachment created!")
                    if(typeof success == "function") {
                        success();
                    }
                    break;
                default:
                    if(typeof fail == "function") {
                        fail(xhr);
                    }
            }
        }
    })
}

Functional.install()
createConsole()
