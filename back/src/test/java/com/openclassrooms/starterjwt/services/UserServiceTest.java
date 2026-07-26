package com.openclassrooms.starterjwt.services;

import com.openclassrooms.starterjwt.exception.BadRequestException;
import com.openclassrooms.starterjwt.exception.ForbiddenException;
import com.openclassrooms.starterjwt.exception.NotFoundException;
import com.openclassrooms.starterjwt.models.Session;
import com.openclassrooms.starterjwt.models.User;
import com.openclassrooms.starterjwt.repository.SessionRepository;
import com.openclassrooms.starterjwt.repository.UserRepository;
import com.openclassrooms.starterjwt.security.services.UserDetailsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    /**
     * findOwnProfile() lit l'utilisateur authentifié via SecurityContextHolder
     * (contrôle de propriété en couche service, même pattern que SessionService).
     * Le contexte est nettoyé après chaque test pour ne pas fuiter d'une méthode
     * de test à l'autre — SecurityContextHolder est un ThreadLocal.
     */
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Long userId) {
        UserDetailsImpl userDetails = UserDetailsImpl.builder()
                .id(userId).username("user" + userId + "@studio.com").build();
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void should_deleteUser_when_deleteByIdIsCalled_and_requesterIsTheOwner() {
        authenticateAs(1L);
        User user = User.builder().id(1L).email("owner@studio.com").firstName("Jean").lastName("Dupont")
                .password("encodedPassword").admin(false).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteById(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void should_throwForbiddenException_when_deleteByIdIsCalled_and_requesterIsNotTheOwner() {
        // L'intrus est authentifié sous un id DIFFÉRENT (2) de la cible (1). Son
        // email diffère aussi, mais c'est bien l'id qui est comparé désormais.
        authenticateAs(2L);
        User user = User.builder().id(1L).email("owner@studio.com").firstName("Jean").lastName("Dupont")
                .password("encodedPassword").admin(false).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.deleteById(1L))
                .isInstanceOf(ForbiddenException.class);

        verify(userRepository, never()).deleteById(any());
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void should_throwForbiddenException_when_deleteByIdIsCalled_and_onlyTheEmailMatches() {
        // Garde-fou de la bascule email -> id : l'appelant porte le MÊME email que
        // la cible mais un id différent. L'ancienne comparaison par email aurait
        // autorisé la suppression ; la comparaison par id la refuse.
        UserDetailsImpl intruder = UserDetailsImpl.builder().id(2L).username("owner@studio.com").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(intruder, null, intruder.getAuthorities()));
        User target = User.builder().id(1L).email("owner@studio.com").firstName("Jean").lastName("Dupont")
                .password("encodedPassword").admin(false).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> userService.deleteById(1L))
                .isInstanceOf(ForbiddenException.class);

        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void should_removeUserFromParticipatedSessions_when_deleteByIdIsCalled() {
        authenticateAs(1L);
        User owner = User.builder().id(1L).email("owner@studio.com").firstName("Jean").lastName("Dupont")
                .password("encodedPassword").admin(false).build();
        User other = User.builder().id(2L).email("other@studio.com").firstName("Paul").lastName("Martin")
                .password("encodedPassword").admin(false).build();
        Session participated = Session.builder().id(10L).name("Hatha")
                .users(new ArrayList<>(Arrays.asList(owner, other))).build();
        Session notParticipated = Session.builder().id(11L).name("Ashtanga")
                .users(new ArrayList<>(List.of(other))).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(sessionRepository.findAll()).thenReturn(Arrays.asList(participated, notParticipated));

        userService.deleteById(1L);

        // Seule la session à laquelle il participait est réécrite, sans lui,
        // et les autres participants sont conservés.
        ArgumentCaptor<Session> sessionCaptor = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository, times(1)).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getId()).isEqualTo(10L);
        assertThat(sessionCaptor.getValue().getUsers()).extracting(User::getId).containsExactly(2L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void should_notTouchAnySession_when_deleteByIdIsCalled_and_userParticipatesInNone() {
        authenticateAs(1L);
        User owner = User.builder().id(1L).email("owner@studio.com").firstName("Jean").lastName("Dupont")
                .password("encodedPassword").admin(false).build();
        Session sessionWithNullUsers = Session.builder().id(10L).name("Hatha").users(null).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(sessionRepository.findAll()).thenReturn(List.of(sessionWithNullUsers));

        userService.deleteById(1L);

        verify(sessionRepository, never()).save(any());
        verify(userRepository).deleteById(1L);
    }

    @Test
    void should_throwNotFoundException_when_deleteByIdIsCalled_and_userDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteById(99L))
                .isInstanceOf(NotFoundException.class);

        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void should_returnUser_when_findByIdIsCalled_and_userExists() {
        User user = User.builder().id(1L).email("yoga@studio.com").firstName("Jean").lastName("Dupont")
                .password("encodedPassword").admin(true).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.findById(1L);

        assertThat(result).isEqualTo(user);
    }

    @Test
    void should_throwNotFoundException_when_findByIdIsCalled_and_userDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_returnUser_when_findOwnProfileIsCalled_and_requesterIsTheOwner() {
        authenticateAs(1L);
        User user = User.builder().id(1L).email("owner@studio.com").firstName("Jean").lastName("Dupont")
                .password("encodedPassword").admin(false).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.findOwnProfile(1L);

        assertThat(result).isEqualTo(user);
    }

    @Test
    void should_throwForbiddenException_when_findOwnProfileIsCalled_and_requesterIsNotTheOwner() {
        authenticateAs(2L);
        User user = User.builder().id(1L).email("owner@studio.com").firstName("Jean").lastName("Dupont")
                .password("encodedPassword").admin(false).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.findOwnProfile(1L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_throwNotFoundException_when_findOwnProfileIsCalled_and_userDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findOwnProfile(99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_returnUser_when_findByIdIsCalled_and_requesterIsAnotherUser() {
        // findById() reste une lecture technique SANS contrôle de propriété :
        // SessionMapper l'appelle pour résoudre les participants d'une session
        // (mapping déclenché par un admin sur POST/PUT /api/session). Y ajouter
        // le contrôle casserait ce mapping — d'où la méthode findOwnProfile()
        // distincte pour l'exposition API.
        authenticateAs(2L);
        User user = User.builder().id(1L).email("owner@studio.com").firstName("Jean").lastName("Dupont")
                .password("encodedPassword").admin(false).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThat(userService.findById(1L)).isEqualTo(user);
    }

    @Test
    void should_returnUser_when_findByEmailIsCalled_and_userExists() {
        User user = User.builder().id(1L).email("yoga@studio.com").firstName("Jean").lastName("Dupont")
                .password("encodedPassword").admin(true).build();
        when(userRepository.findByEmail("yoga@studio.com")).thenReturn(Optional.of(user));

        Optional<User> result = userService.findByEmail("yoga@studio.com");

        assertThat(result).contains(user);
    }

    @Test
    void should_returnEmptyOptional_when_findByEmailIsCalled_and_userDoesNotExist() {
        when(userRepository.findByEmail("unknown@studio.com")).thenReturn(Optional.empty());

        Optional<User> result = userService.findByEmail("unknown@studio.com");

        assertThat(result).isEmpty();
    }

    @Test
    void should_returnTrue_when_existsByEmailIsCalled_and_emailIsTaken() {
        when(userRepository.existsByEmail("yoga@studio.com")).thenReturn(true);

        boolean result = userService.existsByEmail("yoga@studio.com");

        assertThat(result).isTrue();
    }

    @Test
    void should_returnFalse_when_existsByEmailIsCalled_and_emailIsNotTaken() {
        when(userRepository.existsByEmail("unknown@studio.com")).thenReturn(false);

        boolean result = userService.existsByEmail("unknown@studio.com");

        assertThat(result).isFalse();
    }

    @Test
    void should_returnTrue_when_isAdminIsCalled_and_userIsAdmin() {
        User user = User.builder().id(1L).email("admin@studio.com").firstName("Jean").lastName("Dupont")
                .password("encodedPassword").admin(true).build();
        when(userRepository.findByEmail("admin@studio.com")).thenReturn(Optional.of(user));

        boolean result = userService.isAdmin("admin@studio.com");

        assertThat(result).isTrue();
    }

    @Test
    void should_returnFalse_when_isAdminIsCalled_and_userIsNotAdmin() {
        User user = User.builder().id(1L).email("user@studio.com").firstName("Jean").lastName("Dupont")
                .password("encodedPassword").admin(false).build();
        when(userRepository.findByEmail("user@studio.com")).thenReturn(Optional.of(user));

        boolean result = userService.isAdmin("user@studio.com");

        assertThat(result).isFalse();
    }

    @Test
    void should_returnFalse_when_isAdminIsCalled_and_userDoesNotExist() {
        when(userRepository.findByEmail("unknown@studio.com")).thenReturn(Optional.empty());

        boolean result = userService.isAdmin("unknown@studio.com");

        assertThat(result).isFalse();
    }

    @Test
    void should_saveUserWithEncodedPassword_when_registerIsCalled_and_emailIsNotTaken() {
        when(userRepository.existsByEmail("new@studio.com")).thenReturn(false);
        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");

        userService.register("new@studio.com", "Dupont", "Jean", "rawPassword");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("new@studio.com");
        assertThat(savedUser.getLastName()).isEqualTo("Dupont");
        assertThat(savedUser.getFirstName()).isEqualTo("Jean");
        assertThat(savedUser.getPassword()).isEqualTo("encodedPassword");
        assertThat(savedUser.isAdmin()).isFalse();
        verify(passwordEncoder, times(1)).encode("rawPassword");
    }

    @Test
    void should_throwBadRequestException_when_registerIsCalled_and_emailIsAlreadyTaken() {
        when(userRepository.existsByEmail("existing@studio.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register("existing@studio.com", "Dupont", "Jean", "rawPassword"))
                .isInstanceOf(BadRequestException.class);

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }
}
