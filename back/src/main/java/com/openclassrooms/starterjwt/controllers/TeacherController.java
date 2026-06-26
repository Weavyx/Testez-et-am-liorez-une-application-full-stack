package com.openclassrooms.starterjwt.controllers;

import com.openclassrooms.starterjwt.mapper.TeacherMapper;
import com.openclassrooms.starterjwt.models.Teacher;
import com.openclassrooms.starterjwt.services.TeacherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {
    private final TeacherMapper teacherMapper;
    private final TeacherService teacherService;


    public TeacherController(TeacherService teacherService,
                             TeacherMapper teacherMapper) {
        this.teacherMapper = teacherMapper;
        this.teacherService = teacherService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable("id") String id) {
        Teacher teacher = this.teacherService.findById(Long.valueOf(id));

        if (teacher == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok().body(this.teacherMapper.toDto(teacher));
    }

    @GetMapping()
    public ResponseEntity<?> findAll() {
        List<Teacher> teachers = this.teacherService.findAll();

        return ResponseEntity.ok().body(this.teacherMapper.toDto(teachers));
    }
}
