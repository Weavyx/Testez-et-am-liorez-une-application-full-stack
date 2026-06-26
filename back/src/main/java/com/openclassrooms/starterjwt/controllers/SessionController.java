package com.openclassrooms.starterjwt.controllers;


import com.openclassrooms.starterjwt.dto.SessionDto;
import com.openclassrooms.starterjwt.mapper.SessionMapper;
import com.openclassrooms.starterjwt.models.Session;
import com.openclassrooms.starterjwt.services.SessionService;
import jakarta.validation.Valid;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/session")
@Log4j2
public class SessionController {
    private final SessionMapper sessionMapper;
    private final SessionService sessionService;


    public SessionController(SessionService sessionService,
                             SessionMapper sessionMapper) {
        this.sessionMapper = sessionMapper;
        this.sessionService = sessionService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable("id") String id) {
        Session session = this.sessionService.getById(Long.valueOf(id));

        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok().body(this.sessionMapper.toDto(session));
    }

    @GetMapping()
    public ResponseEntity<?> findAll() {
        List<Session> sessions = this.sessionService.findAll();

        return ResponseEntity.ok().body(this.sessionMapper.toDto(sessions));
    }

    @PostMapping()
    public ResponseEntity<?> create(@Valid @RequestBody SessionDto sessionDto) {
        log.info(sessionDto);

        Session session = this.sessionService.create(this.sessionMapper.toEntity(sessionDto));

        log.info(session);
        return ResponseEntity.ok().body(this.sessionMapper.toDto(session));
    }

    @PutMapping("{id}")
    public ResponseEntity<?> update(@PathVariable("id") String id, @Valid @RequestBody SessionDto sessionDto) {
        Session session = this.sessionService.update(Long.parseLong(id), this.sessionMapper.toEntity(sessionDto));

        return ResponseEntity.ok().body(this.sessionMapper.toDto(session));
    }

    @DeleteMapping("{id}")
    public ResponseEntity<?> save(@PathVariable("id") String id) {
        Session session = this.sessionService.getById(Long.valueOf(id));

        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        this.sessionService.delete(Long.parseLong(id));
        return ResponseEntity.ok().build();
    }

    @PostMapping("{id}/participate/{userId}")
    public ResponseEntity<?> participate(@PathVariable("id") String id, @PathVariable("userId") String userId) {
        this.sessionService.participate(Long.parseLong(id), Long.parseLong(userId));

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("{id}/participate/{userId}")
    public ResponseEntity<?> noLongerParticipate(@PathVariable("id") String id, @PathVariable("userId") String userId) {
        this.sessionService.noLongerParticipate(Long.parseLong(id), Long.parseLong(userId));

        return ResponseEntity.ok().build();
    }
}
