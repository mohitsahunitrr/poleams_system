package com.precisionhawk.poleams.config;

import com.esotericsoftware.yamlbeans.YamlReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import javax.inject.Named;
import javax.inject.Provider;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author pchapman
 */
@Named
public class AppConfigFactory implements Provider<AppConfig> {
    
    //TODO: Load from classpath and allow property overrides.
    
    private static final Object LOCK = new Object();
    
    private static final String PARAM_USER_HOME = "user.home";
    
    private static final String[] PATHS = {
        "/etc/poleams/webservices.yaml",
        "{" + PARAM_USER_HOME + "}/.poleams/webservices.yaml"
    };
    
    private AppConfig config;

    @Override
    public AppConfig get() {
        synchronized (LOCK) {
            if (config == null) {
                File f = null;
                String path;
                String userDir = System.getProperty(PARAM_USER_HOME);
                for (int i = 0; config == null && i < PATHS.length ; i++) {
                    path = PATHS[i].replace("{" + PARAM_USER_HOME + "}", userDir);
                    f = new File(path);
                    if (f.canRead()) {
                        config = loadConfiguration(f);
                    }
                }
                if (config == null) {
                    throw new RuntimeException("Unable to locate a readable configuration file for PoleAMS Web Services.");
                }
            }
        }
        return config;
    }

    private AppConfig loadConfiguration(File f) {
        Reader reader = null;
        try {
            reader = new FileReader(f);
            YamlReader yamlreader = new YamlReader(reader);
            return yamlreader.read(AppConfig.class);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to load configuration file for PoleAMS Web Services.", ex);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }
    
}
