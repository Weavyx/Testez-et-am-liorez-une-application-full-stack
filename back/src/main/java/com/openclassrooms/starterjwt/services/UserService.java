package com.openclassrooms.starterjwt.services;

import com.openclassrooms.starterjwt.exception.BadRequestException;
import com.openclassrooms.starterjwt.exception.ForbiddenException;
import com.openclassrooms.starterjwt.exception.NotFoundException;
import com.openclassrooms.starterjwt.exception.UnauthorizedException;
import com.openclassrooms.starterjwt.models.User;
import com.openclassrooms.starterjwt.repository.UserRepository;
import com.openclassrooms.starterjwt.security.services.UserDetailsImpl;
import org.springframework.security.core.context.SecurityContextHolder;
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

    /**
     * Lecture de fiche utilisateur exposée par l'API : contrairement à findById(),
     * qui reste une lecture technique sans contrôle (elle est notamment appelée par
     * SessionMapper pour résoudre les participants d'une session), celle-ci vérifie
     * que l'appelant demande bien SON propre compte.
     *
     * L'existence est vérifiée avant la propriété, comme dans deleteById() et dans
     * SessionService#participate : un id inexistant reste un 404.
     */
    public User findOwnProfile(Long id) {
        User user = findById(id);
        assertRequestingUserIsSelf(id);
        return user;
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

    /**
     * Même contrôle de propriété que SessionService#assertRequestingUserIsSelf :
     * l'identité de l'appelant est lue en couche service via le SecurityContext,
     * et comparée sur l'ID (pas sur l'email).
     */
    private void assertRequestingUserIsSelf(Long userId) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!userDetails.getId().equals(userId)) {
            throw new ForbiddenException();
        }
    }
}
