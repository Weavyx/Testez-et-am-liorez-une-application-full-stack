package com.openclassrooms.starterjwt.security.services;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

class UserDetailsImplTest {

    @Test
    void should_returnRoleAdmin_when_adminIsTrue() {
        UserDetailsImpl userDetails = UserDetailsImpl.builder()
                .id(1L)
                .username("yoga@studio.com")
                .admin(true)
                .build();

        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void should_returnRoleUser_when_adminIsFalse() {
        UserDetailsImpl userDetails = UserDetailsImpl.builder()
                .id(2L)
                .username("user@studio.com")
                .admin(false)
                .build();

        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    void should_returnRoleUser_when_adminIsNull() {
        UserDetailsImpl userDetails = UserDetailsImpl.builder()
                .id(3L)
                .username("noadmin@studio.com")
                .admin(null)
                .build();

        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    void should_returnTrue_when_isAccountNonExpiredIsCalled() {
        UserDetailsImpl userDetails = UserDetailsImpl.builder().id(1L).build();

        assertThat(userDetails.isAccountNonExpired()).isTrue();
    }

    @Test
    void should_returnTrue_when_isAccountNonLockedIsCalled() {
        UserDetailsImpl userDetails = UserDetailsImpl.builder().id(1L).build();

        assertThat(userDetails.isAccountNonLocked()).isTrue();
    }

    @Test
    void should_returnTrue_when_isCredentialsNonExpiredIsCalled() {
        UserDetailsImpl userDetails = UserDetailsImpl.builder().id(1L).build();

        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    }

    @Test
    void should_returnTrue_when_isEnabledIsCalled() {
        UserDetailsImpl userDetails = UserDetailsImpl.builder().id(1L).build();

        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    void should_returnTrue_when_equalsIsCalledWithSameInstance() {
        UserDetailsImpl userDetails = UserDetailsImpl.builder().id(1L).build();

        assertThat(userDetails.equals(userDetails)).isTrue();
    }

    @Test
    void should_returnFalse_when_equalsIsCalledWithNull() {
        UserDetailsImpl userDetails = UserDetailsImpl.builder().id(1L).build();

        assertThat(userDetails.equals(null)).isFalse();
    }

    @Test
    void should_returnFalse_when_equalsIsCalledWithDifferentClass() {
        UserDetailsImpl userDetails = UserDetailsImpl.builder().id(1L).build();

        assertThat(userDetails.equals("not a UserDetailsImpl")).isFalse();
    }

    @Test
    void should_returnTrue_when_equalsIsCalledWithSameId() {
        UserDetailsImpl userDetails1 = UserDetailsImpl.builder().id(1L).username("a@studio.com").build();
        UserDetailsImpl userDetails2 = UserDetailsImpl.builder().id(1L).username("b@studio.com").build();

        assertThat(userDetails1.equals(userDetails2)).isTrue();
    }

    @Test
    void should_returnFalse_when_equalsIsCalledWithDifferentId() {
        UserDetailsImpl userDetails1 = UserDetailsImpl.builder().id(1L).build();
        UserDetailsImpl userDetails2 = UserDetailsImpl.builder().id(2L).build();

        assertThat(userDetails1.equals(userDetails2)).isFalse();
    }
}
