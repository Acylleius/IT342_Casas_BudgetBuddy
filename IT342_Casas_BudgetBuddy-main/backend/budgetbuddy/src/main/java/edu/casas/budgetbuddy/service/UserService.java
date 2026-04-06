package edu.casas.budgetbuddy.service;

import edu.casas.budgetbuddy.entity.User;

import java.util.Optional;

public interface UserService {

    User register(User user);

    Optional<User> findByEmail(String email);
}
