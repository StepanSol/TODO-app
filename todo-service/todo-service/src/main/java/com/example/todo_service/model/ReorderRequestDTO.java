package com.example.todo_service.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReorderRequestDTO {
    @Min(0)
    @NotNull(message = "newPosition cannot be null")
    private Integer newPosition;

}
