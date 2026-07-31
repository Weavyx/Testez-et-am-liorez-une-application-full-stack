package com.openclassrooms.starterjwt.models;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TeacherTest {

    @Test
    void should_returnTrue_when_equalsIsCalledWithSameInstance() {
        Teacher teacher = Teacher.builder().id(1L).lastName("Doe").firstName("John").build();

        assertThat(teacher.equals(teacher)).isTrue();
    }

    @Test
    void should_returnFalse_when_equalsIsCalledWithNull() {
        Teacher teacher = Teacher.builder().id(1L).lastName("Doe").firstName("John").build();

        assertThat(teacher.equals(null)).isFalse();
    }

    @Test
    void should_returnFalse_when_equalsIsCalledWithDifferentClass() {
        Teacher teacher = Teacher.builder().id(1L).lastName("Doe").firstName("John").build();

        assertThat(teacher.equals("not a Teacher")).isFalse();
    }

    @Test
    void should_returnTrue_when_equalsIsCalledWithSameId_and_differentOtherFields() {
        Teacher teacher1 = Teacher.builder().id(1L).lastName("Doe").firstName("John").build();
        Teacher teacher2 = Teacher.builder().id(1L).lastName("Smith").firstName("Jane").build();

        assertThat(teacher1.equals(teacher2)).isTrue();
    }

    @Test
    void should_returnSameHashCode_when_idsAreEqual_and_differentOtherFields() {
        Teacher teacher1 = Teacher.builder().id(1L).lastName("Doe").firstName("John").build();
        Teacher teacher2 = Teacher.builder().id(1L).lastName("Smith").firstName("Jane").build();

        assertThat(teacher1.hashCode()).isEqualTo(teacher2.hashCode());
    }

    @Test
    void should_returnFalse_when_equalsIsCalledWithDifferentId() {
        Teacher teacher1 = Teacher.builder().id(1L).lastName("Doe").firstName("John").build();
        Teacher teacher2 = Teacher.builder().id(2L).lastName("Doe").firstName("John").build();

        assertThat(teacher1.equals(teacher2)).isFalse();
    }

    @Test
    void should_returnTrue_when_equalsIsCalledWithBothIdsNull() {
        Teacher teacher1 = Teacher.builder().lastName("Doe").firstName("John").build();
        Teacher teacher2 = Teacher.builder().lastName("Smith").firstName("Jane").build();

        assertThat(teacher1.equals(teacher2)).isTrue();
    }

    @Test
    void should_returnFalse_when_equalsIsCalledWithOneIdNull() {
        Teacher teacher1 = Teacher.builder().lastName("Doe").firstName("John").build();
        Teacher teacher2 = Teacher.builder().id(1L).lastName("Doe").firstName("John").build();

        assertThat(teacher1.equals(teacher2)).isFalse();
    }

    // Ce test vérifie le fonctionnement des setters/getters générés par Lombok
    // plus qu'une logique métier propre à Teacher : valeur faible mais non nulle,
    // il détecte une régression de configuration Lombok (ex. @Data retiré ou
    // mal configuré sur un champ).
    @Test
    void should_assignAllFields_when_settersAreCalled() {
        Teacher teacher = new Teacher();
        LocalDateTime now = LocalDateTime.now();

        teacher.setId(1L);
        teacher.setLastName("SetLastName");
        teacher.setFirstName("SetFirstName");
        teacher.setCreatedAt(now);
        teacher.setUpdatedAt(now);

        assertThat(teacher.getId()).isEqualTo(1L);
        assertThat(teacher.getLastName()).isEqualTo("SetLastName");
        assertThat(teacher.getFirstName()).isEqualTo("SetFirstName");
        assertThat(teacher.getCreatedAt()).isEqualTo(now);
        assertThat(teacher.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void should_containFieldValues_when_builderToStringIsCalled() {
        LocalDateTime now = LocalDateTime.now();
        String builderToString = Teacher.builder()
                .id(1L)
                .lastName("Doe")
                .firstName("John")
                .createdAt(now)
                .updatedAt(now)
                .toString();

        assertThat(builderToString)
                .contains("Doe")
                .contains("John")
                .contains("id=1");
    }
}
