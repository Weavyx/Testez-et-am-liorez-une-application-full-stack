package com.openclassrooms.starterjwt.services;

import com.openclassrooms.starterjwt.exception.BadRequestException;
import com.openclassrooms.starterjwt.exception.NotFoundException;
import com.openclassrooms.starterjwt.exception.UnauthorizedException;
import com.openclassrooms.starterjwt.models.User;
import com.openclassrooms.starterjwt.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void delete(Long id) {
        this.userRepository.deleteById(id);
    }

    public void deleteById(Long id, String currentUsername) {
        User user = findById(id);
        if (!Objects.equals(currentUsername, user.getEmail())) {
            throw new UnauthorizedException();
        }
        this.userRepository.deleteById(id);
    }

    public User findById(Long id) {
        return this.userRepository.findById(id).orElseThrow(NotFoundException::new);
    }

    public Optional<User> findByEmail(String email) {
        return this.userRepository.findByEmail(email);
    }

    public boolean existsByEmail(String email) {
        return this.userRepository.existsByEmail(email);
    }

    public boolean isAdmin(String email) {
        return this.userRepository.findByEmail(email).map(User::isAdmin).orElse(false);
    }

    public User save(User user) {
        return this.userRepository.save(user);
    }

    public void register(String email, String lastName, String firstName, String rawPassword) {
        if (existsByEmail(email)) {
            throw new BadRequestException("Error: Email is already taken!");
        }
        User user = new User(email, lastName, firstName, passwordEncoder.encode(rawPassword), false);
        this.userRepository.save(user);
    }
}
