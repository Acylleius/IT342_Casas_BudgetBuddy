package edu.casas.budgetbuddy.dto;

import edu.casas.budgetbuddy.entity.User;

public class UserDto {

    private final Long id;
    private final String email;
    private final String firstname;
    private final String lastname;
    private final String role;

    public UserDto(Long id, String email, String firstname, String lastname, String role) {
        this.id = id;
        this.email = email;
        this.firstname = firstname;
        this.lastname = lastname;
        this.role = role;
    }

    public static UserDto from(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getFirstname(),
                user.getLastname(),
                user.getRole()
        );
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public String getRole() {
        return role;
    }
}
