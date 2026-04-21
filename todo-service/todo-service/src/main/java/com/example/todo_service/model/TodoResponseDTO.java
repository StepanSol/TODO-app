package com.example.todo_service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class TodoResponseDTO {

    private Long id;
    private String title;
    private boolean completed;
    private Integer position;
}
