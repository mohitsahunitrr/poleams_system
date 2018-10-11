package com.precisionhawk.poleams.webservices.impl;

import com.esotericsoftware.yamlbeans.YamlReader;
import com.precisionhawk.bean.AuthenticatedUser;
import com.precisionhawk.poleams.bean.User;
import com.precisionhawk.poleams.config.ServicesConfig;
import com.precisionhawk.poleams.webservices.UserWebService;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;

/**
 *
 * @author pchapman
 */
@Named
public class UserWebServiceImpl extends AbstractWebService implements UserWebService {

    @Inject private ServicesConfig config;

    @Override
    public User authenticate(String login, String passhash) {
        List<AuthenticatedUser> users;
        Reader reader = null;
        try {
            reader = new FileReader(config.getUsersListFile());
            YamlReader yamlreader = new YamlReader(reader);
            users = yamlreader.read(List.class, AuthenticatedUser.class);
        } catch (IOException ex) {
            LOGGER.error("Error loading users", ex);
            throw new InternalServerErrorException("Error loading users.");
        }
        AuthenticatedUser user = null;
        for (AuthenticatedUser u : users) {
            if (u.getLogin().equals(login)) {
                user = u;
                break;
            }
        }        
        if (user == null) {
            throw new NotAuthorizedException("Unknown user");
        } if (user.getPasswordHash().equals(passhash)) {
            // Do not return the AuthenticatedUser and expose the password hash.
            return new User(user);
        } else {
            throw new NotAuthorizedException("Credentials not accepted");
        }
    }
    
}
