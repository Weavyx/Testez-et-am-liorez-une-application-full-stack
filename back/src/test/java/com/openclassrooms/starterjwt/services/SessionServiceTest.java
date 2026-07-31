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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SessionService sessionService;

    /**
     * participate()/noLongerParticipate() lisent l'utilisateur authentifié via
     * SecurityContextHolder (fix de contrôle de propriété) ; les tests existants
     * appellent tous ces méthodes avec userId=10L, donc on authentifie par
     * défaut ce même id ici pour ne pas avoir à modifier chaque test individuel.
     */
    @BeforeEach
    void authenticateAsUser10() {
        authenticateAs(10L);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Long userId) {
        UserDetailsImpl userDetails = UserDetailsImpl.builder().id(userId).username("user" + userId + "@studio.com").build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void should_saveSession_when_createIsCalled() {
        Session session = Session.builder().id(1L).name("Yoga").build();
        when(sessionRepository.save(session)).thenReturn(session);

        Session result = sessionService.create(session);

        assertThat(result).isEqualTo(session);
        verify(sessionRepository).save(session);
    }

    @Test
    void should_deleteSession_when_deleteIsCalled_and_sessionExists() {
        when(sessionRepository.existsById(1L)).thenReturn(true);

        sessionService.delete(1L);

        verify(sessionRepository).deleteById(1L);
    }

    @Test
    void should_throwNotFoundException_when_deleteIsCalled_and_sessionDoesNotExist() {
        when(sessionRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> sessionService.delete(99L))
                .isInstanceOf(NotFoundException.class);
        verify(sessionRepository, never()).deleteById(any());
    }

    @Test
    void should_returnAllSessions_when_findAllIsCalled() {
        Session session1 = Session.builder().id(1L).name("Yoga").build();
        Session session2 = Session.builder().id(2L).name("Meditation").build();
        when(sessionRepository.findAll()).thenReturn(Arrays.asList(session1, session2));

        List<Session> result = sessionService.findAll();

        assertThat(result).hasSize(2).containsExactly(session1, session2);
    }

    @Test
    void should_returnEmptyList_when_findAllIsCalled_and_noSessionExists() {
        when(sessionRepository.findAll()).thenReturn(Collections.emptyList());

        List<Session> result = sessionService.findAll();

        assertThat(result).isEmpty();
    }

    @Test
    void should_returnSession_when_getByIdIsCalled_and_sessionExists() {
        Session session = Session.builder().id(1L).name("Yoga").build();
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        Session result = sessionService.getById(1L);

        assertThat(result).isEqualTo(session);
    }

    @Test
    void should_throwNotFoundException_when_getByIdIsCalled_and_sessionDoesNotExist() {
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.getById(99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_setIdFromParameter_when_updateIsCalled() {
        Session session = Session.builder().id(999L).name("Yoga").build();
        ArgumentCaptor<Session> captor = ArgumentCaptor.forClass(Session.class);
        when(sessionRepository.existsById(1L)).thenReturn(true);
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Session result = sessionService.update(1L, session);

        verify(sessionRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1L);
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void should_throwNotFoundException_when_updateIsCalled_and_sessionDoesNotExist() {
        Session session = Session.builder().id(99L).name("Yoga").build();
        when(sessionRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> sessionService.update(99L, session))
                .isInstanceOf(NotFoundException.class);
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void should_addUserToSession_when_participateIsCalled_and_notAlreadyParticipating() {
        User user = User.builder().id(10L).email("user@studio.com").lastName("Doe").firstName("John").password("pw").build();
        Session session = Session.builder().id(1L).name("Yoga").users(new ArrayList<>()).build();
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        sessionService.participate(1L, 10L);

        assertThat(session.getUsers()).containsExactly(user);
        verify(sessionRepository).save(session);
    }

    @Test
    void should_throwNotFoundException_when_participateIsCalled_and_sessionDoesNotExist() {
        User user = User.builder().id(10L).email("user@studio.com").lastName("Doe").firstName("John").password("pw").build();
        when(sessionRepository.findById(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> sessionService.participate(1L, 10L))
                .isInstanceOf(NotFoundException.class);
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void should_throwNotFoundException_when_participateIsCalled_and_userDoesNotExist() {
        Session session = Session.builder().id(1L).name("Yoga").users(new ArrayList<>()).build();
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.participate(1L, 10L))
                .isInstanceOf(NotFoundException.class);
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void should_throwBadRequestException_when_participateIsCalled_and_userAlreadyParticipating() {
        User user = User.builder().id(10L).email("user@studio.com").lastName("Doe").firstName("John").password("pw").build();
        List<User> users = new ArrayList<>();
        users.add(user);
        Session session = Session.builder().id(1L).name("Yoga").users(users).build();
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> sessionService.participate(1L, 10L))
                .isInstanceOf(BadRequestException.class);
        verify(sessionRepository, never()).save(any());
    }

    // Problème 2 : participate() ne doit pouvoir inscrire que l'utilisateur authentifié lui-même.
    @Test
    void should_throwForbiddenException_when_participateIsCalled_and_userIdDoesNotMatchAuthenticatedPrincipal() {
        authenticateAs(999L);
        User user = User.builder().id(10L).email("user@studio.com").lastName("Doe").firstName("John").password("pw").build();
        Session session = Session.builder().id(1L).name("Yoga").users(new ArrayList<>()).build();
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> sessionService.participate(1L, 10L))
                .isInstanceOf(ForbiddenException.class);
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void should_removeUserFromSession_when_noLongerParticipateIsCalled_and_userIsParticipating() {
        User userToRemove = User.builder().id(10L).email("user@studio.com").lastName("Doe").firstName("John").password("pw").build();
        User userToKeep = User.builder().id(20L).email("other@studio.com").lastName("Smith").firstName("Jane").password("pw").build();
        List<User> users = new ArrayList<>();
        users.add(userToRemove);
        users.add(userToKeep);
        Session session = Session.builder().id(1L).name("Yoga").users(users).build();
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(userRepository.findById(10L)).thenReturn(Optional.of(userToRemove));

        sessionService.noLongerParticipate(1L, 10L);

        assertThat(session.getUsers()).containsExactly(userToKeep);
        verify(sessionRepository).save(session);
    }

    @Test
    void should_throwNotFoundException_when_noLongerParticipateIsCalled_and_sessionDoesNotExist() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.noLongerParticipate(1L, 10L))
                .isInstanceOf(NotFoundException.class);
        verify(sessionRepository, never()).save(any());
    }

    // Problème 3 : noLongerParticipate() doit désormais vérifier l'existence de
    // l'utilisateur avant d'agir, comme le fait déjà participate().
    @Test
    void should_throwNotFoundException_when_noLongerParticipateIsCalled_and_userDoesNotExist() {
        Session session = Session.builder().id(1L).name("Yoga").users(new ArrayList<>()).build();
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.noLongerParticipate(1L, 10L))
                .isInstanceOf(NotFoundException.class);
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void should_throwBadRequestException_when_noLongerParticipateIsCalled_and_userIsNotParticipating() {
        User user = User.builder().id(10L).email("user@studio.com").lastName("Doe").firstName("John").password("pw").build();
        Session session = Session.builder().id(1L).name("Yoga").users(new ArrayList<>()).build();
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> sessionService.noLongerParticipate(1L, 10L))
                .isInstanceOf(BadRequestException.class);
        verify(sessionRepository, never()).save(any());
    }

    // Problème 2 : noLongerParticipate() ne doit pouvoir désinscrire que l'utilisateur authentifié lui-même.
    @Test
    void should_throwForbiddenException_when_noLongerParticipateIsCalled_and_userIdDoesNotMatchAuthenticatedPrincipal() {
        authenticateAs(999L);
        User userToRemove = User.builder().id(10L).email("user@studio.com").lastName("Doe").firstName("John").password("pw").build();
        List<User> users = new ArrayList<>();
        users.add(userToRemove);
        Session session = Session.builder().id(1L).name("Yoga").users(users).build();
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(userRepository.findById(10L)).thenReturn(Optional.of(userToRemove));

        assertThatThrownBy(() -> sessionService.noLongerParticipate(1L, 10L))
                .isInstanceOf(ForbiddenException.class);
        verify(sessionRepository, never()).save(any());
    }
}
