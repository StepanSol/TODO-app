package com.example.todo_service.service;


import com.example.todo_service.model.Todo;
import com.example.todo_service.model.TodoResponseDTO;
import com.example.todo_service.model.User;
import com.example.todo_service.repository.TodoRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TodoService {
    private final UserService userService;
    private final TodoRepository todoRepository;

    @Transactional
    public TodoResponseDTO createTodo(String title) {

        String keycloakId = userService.getCurrentUserId();
        User user = userService.getByKeycloakId(keycloakId);

        List<Todo> todos = Optional
                .ofNullable(todoRepository.findByUserIdOrderByPositionAsc(user.getId()))
                .orElseGet(ArrayList::new);

        int nextPosition = todos.size();

        Todo newTodo = Todo.builder()
                .title(title)
                .completed(false)
                .position(nextPosition)
                .user(user)
                .build();


        Todo saved = todoRepository.save(newTodo);

        return mapToDto(saved);
    }

    public List<TodoResponseDTO> getMyTodos(){
        String keycloakId = userService.getCurrentUserId();
        User user = userService.getByKeycloakId(keycloakId);
        return todoRepository.findByUserIdOrderByPositionAsc(user.getId())
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public TodoResponseDTO updateTodo(Long id, String title, boolean completed) {

        String keycloakId = userService.getCurrentUserId();
        User user = userService.getByKeycloakId(keycloakId);

        Todo todo = todoRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Todo not found"));

        todo.setTitle(title);
        todo.setCompleted(completed);

        todoRepository.save(todo);

        return mapToDto(todo);
    }

    public void deleteTodo(Long id) {

        String keycloakId = userService.getCurrentUserId();
        User user = userService.getByKeycloakId(keycloakId);

        List<Todo> todos = todoRepository.findByUserIdOrderByPositionAsc(user.getId());

        Todo target = todos.stream()
                .filter(t -> t.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Todo not found"));

        todos.remove(target);

        for (int i = 0; i < todos.size(); i++) {
            todos.get(i).setPosition(i);
        }

        todoRepository.delete(target);

        todoRepository.saveAll(todos);
    }

    public List<TodoResponseDTO> reorderTodo(Long todoId, int newPosition) {

        String keycloakId = userService.getCurrentUserId();
        User user = userService.getByKeycloakId(keycloakId);

        List<Todo> todos = todoRepository.findByUserIdOrderByPositionAsc(user.getId());

        if (newPosition >= todos.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid position");
        }

        Todo target = todos.stream()
                .filter(t -> t.getId().equals(todoId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Todo not found"));

        todos.remove(target);

        todos.add(newPosition, target);

        for (int i = 0; i < todos.size(); i++) {
            todos.get(i).setPosition(i);
        }

        return todoRepository.saveAll(todos)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    private TodoResponseDTO mapToDto(Todo todo) {
        return new TodoResponseDTO(
                todo.getId(),
                todo.getTitle(),
                todo.isCompleted(),
                todo.getPosition()
        );
    }

}
