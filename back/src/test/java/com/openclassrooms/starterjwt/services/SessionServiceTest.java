package com.openclassrooms.starterjwt.services;

import com.openclassrooms.starterjwt.exception.BadRequestException;
import com.openclassrooms.starterjwt.exception.NotFoundException;
import com.openclassrooms.starterjwt.models.Session;
import com.openclassrooms.starterjwt.models.User;
import com.openclassrooms.starterjwt.repository.SessionRepository;
import com.openclassrooms.starterjwt.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    void should_returnNull_when_getByIdIsCalled_and_sessionDoesNotExist() {
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.getById(99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_setIdFromParameter_when_updateIsCalled() {
        Session session = Session.builder().id(999L).name("Yoga").build();
        ArgumentCaptor<Session> captor = ArgumentCaptor.forClass(Session.class);
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Session result = sessionService.update(1L, session);

        verify(sessionRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1L);
        assertThat(result.getId()).isEqualTo(1L);
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

    @Test
    void should_removeUserFromSession_when_noLongerParticipateIsCalled_and_userIsParticipating() {
        User userToRemove = User.builder().id(10L).email("user@studio.com").lastName("Doe").firstName("John").password("pw").build();
        User userToKeep = User.builder().id(20L).email("other@studio.com").lastName("Smith").firstName("Jane").password("pw").build();
        List<User> users = new ArrayList<>();
        users.add(userToRemove);
        users.add(userToKeep);
        Session session = Session.builder().id(1L).name("Yoga").users(users).build();
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

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

    @Test
    void should_throwBadRequestException_when_noLongerParticipateIsCalled_and_userIsNotParticipating() {
        Session session = Session.builder().id(1L).name("Yoga").users(new ArrayList<>()).build();
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> sessionService.noLongerParticipate(1L, 10L))
                .isInstanceOf(BadRequestException.class);
        verify(sessionRepository, never()).save(any());
    }
}
