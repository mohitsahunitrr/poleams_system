/*
 * All rights reserved.
 */

package com.precisionhawk.poleams.support.security;

import com.precisionhawk.poleams.config.ServicesConfig;
import java.io.IOException;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author <a href="mailto:pchapman@pcsw.us">Philip A. Chapman</a>
 */
@Named
public class SSLEnforcementFilter implements Filter {
    
    Logger LOGGER = LoggerFactory.getLogger(getClass());
    
    private static final Pattern STATUS_REGEXP = Pattern.compile(".*/status$");

    @Inject private ServicesConfig servicesConfig;
    
    private FilterConfig filterConfig = null;

    @Override
    public void init(FilterConfig fc) throws ServletException {
        this.filterConfig = fc;
        LOGGER.info("HTTPS enforcement is{} enabled", servicesConfig.getEnforceSSL() ? "" : " NOT");
    }

    @Override
    public void doFilter(ServletRequest sreq, ServletResponse sresp, FilterChain fc) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest)sreq;
        HttpServletResponse resp = (HttpServletResponse)sresp;
        String uri = req.getRequestURI();
        String scheme = req.getHeader("X-Forwarded-Proto");
        scheme = scheme == null ? req.getScheme().toLowerCase() : scheme.toLowerCase();
        String requester = req.getHeader("X-Forwarded-For");
        requester = requester == null ? req.getRemoteHost() : requester;
        LOGGER.debug("Testing access to URI {} via protocol {} from {}.", uri, scheme, requester);
        if (
                !servicesConfig.getEnforceSSL()
                ||
                "https".equals(scheme)
                ||
                STATUS_REGEXP.matcher(uri).matches()
           )
        {
            LOGGER.debug("Access allowed");
            fc.doFilter(sreq, sresp);
        } else {
            LOGGER.error("Denying access to URI {} via protocol {} from {}.", uri, scheme, requester);
            resp.sendError(403, "Only HTTPS requests are honored.");
        }
    }

    @Override
    public void destroy() {
       this.filterConfig = null;
     }
}
