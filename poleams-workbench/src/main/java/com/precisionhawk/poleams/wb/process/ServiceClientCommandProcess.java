/*
 * All rights reserved.
 */

package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.poleams.wb.Main;
import com.precisionhawk.poleams.webservices.ExportWebService;
import com.precisionhawk.poleams.webservices.PoleInspectionWebService;
import com.precisionhawk.poleams.webservices.PoleWebService;
import com.precisionhawk.poleams.webservices.ResourceWebService;
import com.precisionhawk.poleams.webservices.StatusWebService;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.ams.webservices.client.spring.EnvironmentsFactory;
import java.io.File;
import java.util.List;
import java.util.Queue;
import com.precisionhawk.poleams.webservices.FeederWebService;

/**
 *
 * @author <a href="mailto:pchapman@pcsw.us">Philip A. Chapman</a>
 */
public abstract class ServiceClientCommandProcess extends CommandProcess {

    private static final String DEFAULT_CFG_FILE = "environments.yaml";
    private static final String ARG_CONFIG = "-cfg";
    private static final String ARG_ENV = "-env";
    protected static final String ARGS_FOR_HELP = "[" + ARG_CONFIG + " environments/config/file.yaml] " + ARG_ENV + " environment";
    
    public final boolean process(Queue<String> args) {
        String configURI = "file://" + new File(new File(new File(System.getProperty("user.home")), Main.USER_COFIG_DIR), DEFAULT_CFG_FILE).getAbsolutePath();
        String envStr = null;
        
        String s;
        for (String arg = args.poll(); arg != null; arg = args.poll()) {
            switch (arg) {
                case ARG_CONFIG:
                    s = args.poll();
                    if (s == null) {
                        return false;
                    } else {
                        configURI = s;
                    }
                    break;
                case ARG_ENV:
                    s = args.poll();
                    if (s == null || envStr != null) {
                        return false;
                    } else {
                        envStr = s;
                    }
                    break;
                default:
                    if (!processArg(arg, args)) {
                        return false;
                    }
            }
        }
        
        if (configURI == null) {
            return false;
        }
        
        EnvironmentsFactory factory = new EnvironmentsFactory();
        factory.setConfigFilePath(configURI);
        List<Environment> environments;
        try {
            factory.init();
            environments = factory.getEnvironments();
        } catch (Exception ex) {
            System.err.printf("Unable to configure environments from URI %s\n", configURI);
            return false;
        }
        
        Environment env = null;
        if (envStr != null) {
            for (Environment e : environments) {
                if (envStr.equals(e.getName())) {
                    env = e;
                    break;
                }
            }
            if (env == null) {
                System.err.printf("Unable to locate configuration for environment %s\n", envStr);
                return false;
            }
        } else if (environments.size() == 1) {
            env = environments.get(0);
        } else {
            System.err.println("Unable to determine what environment to use");
            return false;
        }
        
        return execute(env);
    }
    
    protected abstract boolean processArg(String arg, Queue<String> args);
    
    protected abstract boolean execute(Environment env);
    
    protected ExportWebService exportService(Environment env) {
        return env.obtainWebService(ExportWebService.class);
    }
    
    protected PoleInspectionWebService poleInspectionService(Environment env) {
        return env.obtainWebService(PoleInspectionWebService.class);
    }
    
    protected PoleWebService poleService(Environment env) {
        return env.obtainWebService(PoleWebService.class);
    }

    protected ResourceWebService componentService(Environment env) {
        return env.obtainWebService(ResourceWebService.class);
    }

    protected StatusWebService componentInspectionService(Environment env) {
        return env.obtainWebService(StatusWebService.class);
    }

    protected FeederWebService inspectionEventService(Environment env) {
        return env.obtainWebService(FeederWebService.class);
    }
}
