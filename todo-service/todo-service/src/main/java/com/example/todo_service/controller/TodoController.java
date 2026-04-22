package com.example.todo_service.controller;

import com.example.todo_service.model.CreateTodoRequestDTO;
import com.example.todo_service.model.ReorderRequestDTO;
import com.example.todo_service.model.Todo;
import com.example.todo_service.model.TodoResponseDTO;
import com.example.todo_service.model.UpdateTodoRequestDTO;
import com.example.todo_service.service.TodoService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    @PostMapping
    public TodoResponseDTO createTodo(@RequestBody @Valid CreateTodoRequestDTO request) {
        return todoService.createTodo(request.getTitle());
    }

    @GetMapping
    public List<TodoResponseDTO> getMyTodos(){
        return todoService.getMyTodos();
    }

    @PutMapping("/{id}")
    public TodoResponseDTO updateTodo(@PathVariable Long id,
                           @RequestBody @Valid UpdateTodoRequestDTO request) {

        return todoService.updateTodo(
                id,
                request.getTitle(),
                request.isCompleted()
        );
    }

    @DeleteMapping("/{id}")
    public void deleteTodo(@PathVariable Long id) {
        todoService.deleteTodo(id);
    }

    @PutMapping("/{id}/reorder")
    public List<TodoResponseDTO> reorder(
            @PathVariable Long id,
            @RequestBody @Valid ReorderRequestDTO request
    ) {
        return todoService.reorderTodo(id, request.getNewPosition());
    }

}
