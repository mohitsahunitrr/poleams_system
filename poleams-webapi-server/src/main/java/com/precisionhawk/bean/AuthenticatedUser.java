package com.precisionhawk.bean;

import com.precisionhawk.poleams.bean.User;

/**
 * Extends the User class for storing the password hash.
 *
 * @author pchapman
 */
public class AuthenticatedUser extends User {
    
    private String passwordHash;
    public String getPasswordHash() {
        return passwordHash;
    }
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
