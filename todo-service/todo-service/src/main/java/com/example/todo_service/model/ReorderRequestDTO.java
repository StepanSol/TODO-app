package com.example.todo_service.model;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReorderRequestDTO {
    @Min(0)
    private Integer newPosition;

}
