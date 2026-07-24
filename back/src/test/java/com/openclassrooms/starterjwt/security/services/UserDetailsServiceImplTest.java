package com.openclassrooms.starterjwt.security.services;

import com.openclassrooms.starterjwt.models.User;
import com.openclassrooms.starterjwt.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void should_returnUserDetails_when_loadUserByUsernameIsCalled_and_userExists() {
        User user = User.builder()
                .id(1L)
                .email("yoga@studio.com")
                .firstName("Jean")
                .lastName("Dupont")
                .password("encodedPassword")
                .admin(true)
                .build();
        when(userRepository.findByEmail("yoga@studio.com")).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("yoga@studio.com");

        UserDetailsImpl userDetails = (UserDetailsImpl) result;
        assertThat(userDetails.getId()).isEqualTo(1L);
        assertThat(userDetails.getUsername()).isEqualTo("yoga@studio.com");
        assertThat(userDetails.getFirstName()).isEqualTo("Jean");
        assertThat(userDetails.getLastName()).isEqualTo("Dupont");
        assertThat(userDetails.getPassword()).isEqualTo("encodedPassword");
        assertThat(userDetails.getAdmin()).isTrue();
    }

    @Test
    void should_throwUsernameNotFoundException_when_loadUserByUsernameIsCalled_and_userDoesNotExist() {
        when(userRepository.findByEmail("unknown@studio.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown@studio.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User Not Found with email: unknown@studio.com");
    }
}
