/*
 * All rights reserved.
 */

package com.precisionhawk.poleams.support.security;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A filter which adds "Access-Control-Allow-Origin" header to responses.
 *
 * @author <a href="mail:pchapman@pcsw.us">Philip A. Chapman</a>
 */
public class CORSFilter implements Filter {
    
    @Override
    public void doFilter ( ServletRequest request, ServletResponse response, FilterChain chain ) throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse)response;
        httpResponse.setHeader("Access-Control-Allow-Origin", "*"); //TODO better security here
//        httpResponse.setHeader("Access-Control-Allow-Credentials", "true"); //TODO better security here
        if (request instanceof HttpServletRequest && ((HttpServletRequest)request).getMethod().equalsIgnoreCase("OPTIONS")) {
            // Send response headers needed to allow cross site scripting
            httpResponse.setHeader("Access-Control-Allow-Methods", "DELETE, GET, POST, PUT");
            httpResponse.setHeader("Access-Control-Allow-Headers", "origin, x-requested-with, accept, content-type, range, authorization");
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void init(FilterConfig fc) throws ServletException {
    }

    @Override
    public void destroy() {
    }
}
