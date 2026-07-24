package com.openclassrooms.starterjwt.services;

import com.openclassrooms.starterjwt.exception.BadRequestException;
import com.openclassrooms.starterjwt.exception.ForbiddenException;
import com.openclassrooms.starterjwt.exception.NotFoundException;
import com.openclassrooms.starterjwt.exception.UnauthorizedException;
import com.openclassrooms.starterjwt.models.Session;
import com.openclassrooms.starterjwt.models.User;
import com.openclassrooms.starterjwt.repository.SessionRepository;
import com.openclassrooms.starterjwt.repository.UserRepository;
import com.openclassrooms.starterjwt.security.services.UserDetailsImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       SessionRepository sessionRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
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
        removeFromAllSessions(id);
        this.userRepository.deleteById(id);
    }

    /**
     * Desinscrit l'utilisateur de toutes les sessions auxquelles il participe,
     * avant la suppression de son compte.
     *
     * Necessaire : la table de jointure PARTICIPATE est portee par Session
     * (@ManyToMany cote Session uniquement), User ne declare aucune relation
     * inverse, donc aucun cascade ne nettoie les participations. Sans ce
     * nettoyage, userRepository.deleteById() viole la contrainte de cle
     * etrangere participate.user_id -> users.id et remonte une
     * DataIntegrityViolationException non geree, soit un 500 sur le cas normal
     * (P2 de AUDIT_PHASE1_ZONE_AUTH_USER.md).
     *
     * La reconstruction de la liste par filtrage reprend exactement le pattern
     * de SessionService#noLongerParticipate.
     */
    private void removeFromAllSessions(Long userId) {
        List<Session> sessions = this.sessionRepository.findAll();
        for (Session session : sessions) {
            List<User> participants = session.getUsers();
            if (participants == null || participants.stream().noneMatch(u -> u.getId().equals(userId))) {
                continue;
            }
            session.setUsers(participants.stream()
                    .filter(u -> !u.getId().equals(userId))
                    .collect(Collectors.toList()));
            this.sessionRepository.save(session);
        }
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
