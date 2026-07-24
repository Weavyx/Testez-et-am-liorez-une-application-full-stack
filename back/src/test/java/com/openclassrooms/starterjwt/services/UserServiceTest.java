package com.openclassrooms.starterjwt.services;

import com.openclassrooms.starterjwt.exception.BadRequestException;
import com.openclassrooms.starterjwt.exception.NotFoundException;
import com.openclassrooms.starterjwt.exception.UnauthorizedException;
import com.openclassrooms.starterjwt.models.User;
import com.openclassrooms.starterjwt.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void should_deleteUser_when_deleteIsCalled() {
        userService.delete(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void should_deleteUser_when_deleteByIdIsCalled_and_currentUsernameMatchesUserEmail() {
        User user = User.builder().id(1L).email("owner@studio.com").firstName("Jean").lastName("Dupont")
                .password("encodedPassword").admin(false).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteById(1L, "owner@studio.com");

        verify(userRepository).deleteById(1L);
    }

    @Test
    void should_throwUnauthorizedException_when_deleteByIdIsCalled_and_currentUsernameDoesNotMatchUserEmail() {
        User user = User.builder().id(1L).email("owner@studio.com").firstName("Jean").lastName("Dupont")
                .password("encodedPassword").admin(false).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.deleteById(1L, "intruder@studio.com"))
                .isInstanceOf(UnauthorizedException.class);

        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void should_throwNotFoundException_when_deleteByIdIsCalled_and_userDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteById(99L, "owner@studio.com"))
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
    void should_returnSavedUser_when_saveIsCalled() {
        User user = User.builder().id(1L).email("yoga@studio.com").firstName("Jean").lastName("Dupont")
                .password("encodedPassword").admin(false).build();
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.save(user);

        assertThat(result).isEqualTo(user);
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
