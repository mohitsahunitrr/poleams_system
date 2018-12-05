package com.precisionhawk.poleamsv0dot0.bean;

import io.swagger.oas.annotations.media.Schema;

/**
 *
 * @author pchapman
 */
@Schema(description="A system user.")
public class User {
    
    @Schema(description="The user's system login.")
    private String login;
    @Schema(description="The user's first name.")
    private String firstName;
    @Schema(description="The user's last name.")
    private String lastName;

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public User() {}
    
    public User(User original) {
        this.firstName = original.getFirstName();
        this.lastName = original.getLastName();
        this.login = original.getLogin();
    }
}
