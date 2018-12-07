package com.precisionhawk.poleams.config;

/**
 *
 * @author Philip A. Chapman
 */
public class ServicesConfigBean extends com.precisionhawk.ams.config.ServicesConfigBean implements ServicesConfig {
    
    private String usersListFile;
    @Override
    public String getUsersListFile() {
        return usersListFile;
    }
    public void setUsersListFile(String usersListFile) {
        this.usersListFile = usersListFile;
    }
    
}
