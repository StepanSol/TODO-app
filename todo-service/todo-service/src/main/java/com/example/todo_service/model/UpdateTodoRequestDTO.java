package com.example.todo_service.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTodoRequestDTO {

    @NotBlank
    @Size(max = 255)
    private String title;
    private boolean completed;
}
