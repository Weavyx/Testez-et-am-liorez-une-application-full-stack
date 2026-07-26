package com.openclassrooms.starterjwt.models;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class SessionTest {

    @Test
    void should_returnTrue_when_equalsIsCalledWithSameInstance() {
        Session session = Session.builder().id(1L).name("Yoga").build();

        assertThat(session.equals(session)).isTrue();
    }

    @Test
    void should_returnFalse_when_equalsIsCalledWithNull() {
        Session session = Session.builder().id(1L).name("Yoga").build();

        assertThat(session.equals(null)).isFalse();
    }

    @Test
    void should_returnFalse_when_equalsIsCalledWithDifferentClass() {
        Session session = Session.builder().id(1L).name("Yoga").build();

        assertThat(session.equals("not a Session")).isFalse();
    }

    @Test
    void should_returnTrue_when_equalsIsCalledWithSameId_and_differentOtherFields() {
        Session session1 = Session.builder().id(1L).name("Yoga").description("A").build();
        Session session2 = Session.builder().id(1L).name("Pilates").description("B").build();

        assertThat(session1.equals(session2)).isTrue();
        assertThat(session1.hashCode()).isEqualTo(session2.hashCode());
    }

    @Test
    void should_returnFalse_when_equalsIsCalledWithDifferentId() {
        Session session1 = Session.builder().id(1L).name("Yoga").build();
        Session session2 = Session.builder().id(2L).name("Yoga").build();

        assertThat(session1.equals(session2)).isFalse();
    }

    @Test
    void should_returnTrue_when_equalsIsCalledWithBothIdsNull() {
        Session session1 = Session.builder().name("Yoga").build();
        Session session2 = Session.builder().name("Pilates").build();

        assertThat(session1.equals(session2)).isTrue();
    }

    @Test
    void should_returnFalse_when_equalsIsCalledWithOneIdNull() {
        Session session1 = Session.builder().name("Yoga").build();
        Session session2 = Session.builder().id(1L).name("Yoga").build();

        assertThat(session1.equals(session2)).isFalse();
    }

    @Test
    void should_assignAllFields_when_settersAreCalled() {
        Session session = new Session();
        Teacher teacher = Teacher.builder().id(1L).build();
        Date date = new Date();
        LocalDateTime now = LocalDateTime.now();

        session.setName("Yoga du soir");
        session.setDate(date);
        session.setDescription("Une séance relaxante");
        session.setTeacher(teacher);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);

        assertThat(session.getName()).isEqualTo("Yoga du soir");
        assertThat(session.getDate()).isEqualTo(date);
        assertThat(session.getDescription()).isEqualTo("Une séance relaxante");
        assertThat(session.getTeacher()).isEqualTo(teacher);
        assertThat(session.getCreatedAt()).isEqualTo(now);
        assertThat(session.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void should_containFieldValues_when_builderToStringIsCalled() {
        String builderToString = Session.builder()
                .id(1L)
                .name("Yoga du soir")
                .users(Collections.emptyList())
                .toString();

        assertThat(builderToString).contains("Yoga du soir");
    }
}
