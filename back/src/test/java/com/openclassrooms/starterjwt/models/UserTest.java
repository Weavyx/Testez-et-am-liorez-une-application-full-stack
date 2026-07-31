package com.openclassrooms.starterjwt.models;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class UserTest {

    @Test
    void should_returnTrue_when_equalsIsCalledWithSameInstance() {
        User user = User.builder().id(1L).email("a@studio.com").lastName("A").firstName("A").password("pwd").admin(false).build();

        assertThat(user.equals(user)).isTrue();
    }

    @Test
    void should_returnFalse_when_equalsIsCalledWithNull() {
        User user = User.builder().id(1L).email("a@studio.com").lastName("A").firstName("A").password("pwd").admin(false).build();

        assertThat(user.equals(null)).isFalse();
    }

    @Test
    void should_returnFalse_when_equalsIsCalledWithDifferentClass() {
        User user = User.builder().id(1L).email("a@studio.com").lastName("A").firstName("A").password("pwd").admin(false).build();

        assertThat(user.equals("not a User")).isFalse();
    }

    @Test
    void should_returnTrue_when_equalsIsCalledWithSameId_and_differentOtherFields() {
        User user1 = User.builder().id(1L).email("a@studio.com").lastName("A").firstName("A").password("pwd").admin(false).build();
        User user2 = User.builder().id(1L).email("b@studio.com").lastName("B").firstName("B").password("other").admin(true).build();

        assertThat(user1.equals(user2)).isTrue();
    }

    @Test
    void should_returnSameHashCode_when_idsAreEqual_and_differentOtherFields() {
        User user1 = User.builder().id(1L).email("a@studio.com").lastName("A").firstName("A").password("pwd").admin(false).build();
        User user2 = User.builder().id(1L).email("b@studio.com").lastName("B").firstName("B").password("other").admin(true).build();

        assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
    }

    @Test
    void should_returnFalse_when_equalsIsCalledWithDifferentId() {
        User user1 = User.builder().id(1L).email("a@studio.com").lastName("A").firstName("A").password("pwd").admin(false).build();
        User user2 = User.builder().id(2L).email("a@studio.com").lastName("A").firstName("A").password("pwd").admin(false).build();

        assertThat(user1.equals(user2)).isFalse();
    }

    @Test
    void should_returnTrue_when_equalsIsCalledWithBothIdsNull() {
        User user1 = User.builder().email("a@studio.com").lastName("A").firstName("A").password("pwd").admin(false).build();
        User user2 = User.builder().email("b@studio.com").lastName("B").firstName("B").password("other").admin(true).build();

        assertThat(user1.equals(user2)).isTrue();
    }

    @Test
    void should_returnFalse_when_equalsIsCalledWithOneIdNull() {
        User user1 = User.builder().email("a@studio.com").lastName("A").firstName("A").password("pwd").admin(false).build();
        User user2 = User.builder().id(1L).email("a@studio.com").lastName("A").firstName("A").password("pwd").admin(false).build();

        assertThat(user1.equals(user2)).isFalse();
    }

    @Test
    void should_containFieldValues_when_toStringIsCalled() {
        User user = User.builder().id(1L).email("a@studio.com").lastName("Doe").firstName("John").password("pwd").admin(true).build();

        assertThat(user.toString())
                .contains("a@studio.com")
                .contains("Doe")
                .contains("John");
    }

    // Ce test vérifie le fonctionnement des setters/getters générés par Lombok
    // plus qu'une logique métier propre à User : valeur faible mais non nulle,
    // il détecte une régression de configuration Lombok (ex. @Data retiré ou
    // mal configuré sur un champ).
    @Test
    void should_assignAllFields_when_settersAreCalled() {
        User user = new User();
        LocalDateTime now = LocalDateTime.now();

        user.setId(1L);
        user.setEmail("set@studio.com");
        user.setLastName("SetLastName");
        user.setFirstName("SetFirstName");
        user.setPassword("setPassword");
        user.setAdmin(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getEmail()).isEqualTo("set@studio.com");
        assertThat(user.getLastName()).isEqualTo("SetLastName");
        assertThat(user.getFirstName()).isEqualTo("SetFirstName");
        assertThat(user.getPassword()).isEqualTo("setPassword");
        assertThat(user.isAdmin()).isTrue();
        assertThat(user.getCreatedAt()).isEqualTo(now);
        assertThat(user.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void should_throwNullPointerException_when_requiredArgsConstructorIsCalled_and_emailIsNull() {
        assertThatNullPointerException()
                .isThrownBy(() -> new User(null, "last", "first", "pwd", false));
    }

    @Test
    void should_throwNullPointerException_when_requiredArgsConstructorIsCalled_and_lastNameIsNull() {
        assertThatNullPointerException()
                .isThrownBy(() -> new User("a@studio.com", null, "first", "pwd", false));
    }

    @Test
    void should_throwNullPointerException_when_requiredArgsConstructorIsCalled_and_firstNameIsNull() {
        assertThatNullPointerException()
                .isThrownBy(() -> new User("a@studio.com", "last", null, "pwd", false));
    }

    @Test
    void should_throwNullPointerException_when_requiredArgsConstructorIsCalled_and_passwordIsNull() {
        assertThatNullPointerException()
                .isThrownBy(() -> new User("a@studio.com", "last", "first", null, false));
    }

    @Test
    void should_throwNullPointerException_when_allArgsConstructorIsCalled_and_emailIsNull() {
        LocalDateTime now = LocalDateTime.now();
        assertThatNullPointerException()
                .isThrownBy(() -> new User(1L, null, "last", "first", "pwd", false, now, now));
    }

    @Test
    void should_throwNullPointerException_when_allArgsConstructorIsCalled_and_lastNameIsNull() {
        LocalDateTime now = LocalDateTime.now();
        assertThatNullPointerException()
                .isThrownBy(() -> new User(1L, "a@studio.com", null, "first", "pwd", false, now, now));
    }

    @Test
    void should_throwNullPointerException_when_allArgsConstructorIsCalled_and_firstNameIsNull() {
        LocalDateTime now = LocalDateTime.now();
        assertThatNullPointerException()
                .isThrownBy(() -> new User(1L, "a@studio.com", "last", null, "pwd", false, now, now));
    }

    @Test
    void should_throwNullPointerException_when_allArgsConstructorIsCalled_and_passwordIsNull() {
        LocalDateTime now = LocalDateTime.now();
        assertThatNullPointerException()
                .isThrownBy(() -> new User(1L, "a@studio.com", "last", "first", null, false, now, now));
    }

    @Test
    void should_throwNullPointerException_when_builderFieldIsSetToNull_and_emailIsNull() {
        assertThatNullPointerException()
                .isThrownBy(() -> User.builder().email(null));
    }

    @Test
    void should_throwNullPointerException_when_builderFieldIsSetToNull_and_lastNameIsNull() {
        assertThatNullPointerException()
                .isThrownBy(() -> User.builder().lastName(null));
    }

    @Test
    void should_throwNullPointerException_when_builderFieldIsSetToNull_and_firstNameIsNull() {
        assertThatNullPointerException()
                .isThrownBy(() -> User.builder().firstName(null));
    }

    @Test
    void should_throwNullPointerException_when_builderFieldIsSetToNull_and_passwordIsNull() {
        assertThatNullPointerException()
                .isThrownBy(() -> User.builder().password(null));
    }

    @Test
    void should_containFieldValues_when_builderToStringIsCalled() {
        LocalDateTime now = LocalDateTime.now();
        String builderToString = User.builder()
                .id(1L)
                .email("a@studio.com")
                .lastName("Doe")
                .firstName("John")
                .password("pwd")
                .admin(false)
                .createdAt(now)
                .updatedAt(now)
                .toString();

        assertThat(builderToString)
                .contains("a@studio.com")
                .contains("Doe")
                .contains("John")
                .contains("admin=false");
    }
}
