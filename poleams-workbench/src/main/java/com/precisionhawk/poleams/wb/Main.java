package com.precisionhawk.poleams.wb;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.gaffer.GafferConfigurator;
import ch.qos.logback.core.util.StatusPrinter;
import com.precisionhawk.poleams.wb.process.*;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mail:pchapman@pcsw.us">Philip A. Chapman</a>
 */
public final class Main {

    private static final String DEFAULT_LOGGING_CONFIG = "com/precisionhawk/poleams/wb/workbench.logback.groovy";
    public static final String USER_COFIG_DIR = ".poleams";
    private static final String USER_LOGGING_COFIG = "workbench.logback.groovy";
    private final CommandProcess[] COMMANDS = new CommandProcess[] {
        new ChangeResourceStatusProcess(),
        new DeletePoleProcess(),
        new FeederDataImportProcess(),
        new PopulateEncroachmentGoogleEarthURL(),
        new PopulateMasterSurveyProcess(),
        new ResourceUploadProcess()
    };
    
    private Main() {}

    public static void main(String[] argsArray) {
        new Main().execute(argsArray);
    }
    
    private void execute(String[] argsArray) {
        configureLogging();
        Queue<String> args = new LinkedList<>();
        Collections.addAll(args, argsArray);
        boolean success = false;

        String command = args.poll();
        for (CommandProcess p : COMMANDS) {
            if (p.canProcess(command)) {
                // Process the request
                success = p.process(args);
                break;
            }
        }

        if (!success) {
            printError();
        }
        System.exit(success ? 0 : 1);
    }
    
    private void printError() {
        System.out.println("java -jar workbench.jar command args...");
        for (CommandProcess p : COMMANDS) {
            p.printHelp(System.out);
        }
        System.exit(1);
    }

    private void configureLogging() {
        // If there is a file in $home/.windams/workbench.logback.groovy, use that
        // Otherwise, use classpath://com.windams.wb.workbench.logback.groovy
        URL config = null;
        File f = new File(new File(new File(System.getProperty("user.home")), USER_COFIG_DIR), USER_LOGGING_COFIG);
        if (f.canRead()) {
            try {
                config = f.toURI().toURL();
            } catch (MalformedURLException ex) {
                
            }
        }
        if (config == null) {
            config = getClass().getClassLoader().getResource(DEFAULT_LOGGING_CONFIG);
        }
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        GafferConfigurator configurator = new GafferConfigurator(context);
        configurator.run(config);
        StatusPrinter.printInCaseOfErrorsOrWarnings(context);
        
        LoggerFactory.getLogger(getClass()).info("LogBack has been configured from {}", config);
    }
}
