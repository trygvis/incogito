package no.java.incogito.ems.client;

import no.java.ems.client.RestEmsService;
import no.java.ems.service.EmsService;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Required;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * @author <a href="mailto:trygve.laugstol@arktekk.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class RestEmsServiceFactoryBean implements FactoryBean {

    private static final Logger log = LoggerFactory.getLogger(RestEmsServiceFactoryBean.class);

    private String baseurl;

    private boolean cache;

    private String username;

    private String password;

    @Required
    public void setBaseurl(String baseurl) {
        this.baseurl = baseurl;
    }

    public void setCache(boolean cache) {
        this.cache = cache;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Object getObject() throws Exception {
        log.info("Creating EMS client. Cache=" + cache + ", URL=" + baseurl);

        RestEmsService service = new RestEmsService(baseurl, cache);
        service.setCredentials(username, password);
        return service;
    }

    public Class getObjectType() {
        return EmsService.class;
    }

    public boolean isSingleton() {
        return true;
    }
}
