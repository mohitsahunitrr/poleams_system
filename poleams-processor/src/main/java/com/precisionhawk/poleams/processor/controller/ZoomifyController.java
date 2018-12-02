package com.precisionhawk.poleams.processor.controller;

import com.precisionhawk.poleams.bean.ResourceSummary;
import com.precisionhawk.poleams.processor.zoomify.ZoomifyJob;
import com.precisionhawk.poleams.processor.zoomify.ZoomifyProcessException;
import com.precisionhawk.poleams.webservices.ResourceWebService;
import com.precisionhawk.poleams.webservices.client.Environment;
import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author pchapman
 */
@Controller
@RequestMapping("/zoomify")
public class ZoomifyController {
    
    @Inject
    private ZoomifyJob zoomifyJob;
    
    @Inject
    @Named("environments")
    private List<Environment> environments;
    
    @RequestMapping(method = RequestMethod.POST)
    public void zoomifyResource(
            @RequestParam("env") String envName, @RequestParam("resourceId") String resourceId,
            HttpServletResponse response
        ) throws IOException
    {
        try {
            Environment env = null;
            for (Environment e : environments) {
                if (e.getName().equals(envName)) {
                    env = e;
                    break;
                }
            }
            if (env == null) {
                // Bad request
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().printf("No such environment \"%s\"", envName);
                return;
            }
            ResourceWebService svc = env.obtainWebService(ResourceWebService.class);
            ResourceSummary smry = svc.retrieveSummary(env.obtainAccessToken(), resourceId);
            if (smry == null) {
                // Bad request
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().printf("No such resource \"%s\"", resourceId);
                return;
            }
            if (zoomifyJob.zoomifyResource(env, smry)) {
                response.setStatus(HttpServletResponse.SC_OK);
                return;
            }
        } catch (IOException | ZoomifyProcessException ex) {
            LoggerFactory.getLogger(getClass()).error("Error zoomifying resource \"%s\" in environment \"%s\".", resourceId, envName, ex);
        }
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.getWriter().printf("Unable to zoomify the resource \"%s\"", resourceId);
    }
}
