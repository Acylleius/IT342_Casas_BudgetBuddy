package edu.casas.budgetbuddy.service.impl;

import edu.casas.budgetbuddy.entity.User;
import edu.casas.budgetbuddy.repository.UserRepository;
import edu.casas.budgetbuddy.service.UserService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User register(User user) {
        return userRepository.save(user);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
