package edu.casas.budgetbuddy.dto;

import lombok.Data;

@Data
public class RegisterRequest {

    private String email;
    private String password;
    private String firstname;
    private String lastname;

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

}


