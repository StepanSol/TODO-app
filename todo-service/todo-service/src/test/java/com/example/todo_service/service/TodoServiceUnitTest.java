package com.example.todo_service.service;

import com.example.todo_service.model.*;
import com.example.todo_service.repository.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TodoService Unit Tests")
class TodoServiceUnitTest {

    @Mock
    private UserService userService;

    @Mock
    private TodoRepository todoRepository;

    @InjectMocks
    private TodoService todoService;

    private User testUser;
    private Todo testTodo;
    private final String KEYCLOAK_ID = "test-keycloak-id-123";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setKeycloakId(KEYCLOAK_ID);
        testUser.setEmail("test@example.com");

        testTodo = Todo.builder()
                .id(1L)
                .title("Test Todo")
                .completed(false)
                .position(0)
                .user(testUser)
                .build();
    }

    @Nested
    @DisplayName("createTodo() tests")
    class CreateTodoTests {

        @Test
        @DisplayName("Should create todo successfully")
        void shouldCreateTodoSuccessfully() {
            // Given
            String title = "New Todo";
            when(userService.getCurrentUserId()).thenReturn(KEYCLOAK_ID);
            when(userService.getByKeycloakId(KEYCLOAK_ID)).thenReturn(testUser);
            when(todoRepository.findByUserIdOrderByPositionAsc(testUser.getId())).thenReturn(new ArrayList<>());
            when(todoRepository.save(any(Todo.class))).thenAnswer(invocation -> {
                Todo todoToSave = invocation.getArgument(0);
                todoToSave.setId(1L);
                return todoToSave;
            });

            // When
            TodoResponseDTO result = todoService.createTodo(title);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo(title);
            assertThat(result.isCompleted()).isFalse();
            assertThat(result.getPosition()).isEqualTo(0);

            verify(userService).getCurrentUserId();
            verify(userService).getByKeycloakId(KEYCLOAK_ID);
            verify(todoRepository).findByUserIdOrderByPositionAsc(testUser.getId());
            verify(todoRepository).save(any(Todo.class));
        }

        @Test
        @DisplayName("Should create todo with correct position when existing todos present")
        void shouldCreateTodoWithCorrectPosition() {
            // Given
            String title = "New Todo";
            List<Todo> existingTodos = List.of(
                    Todo.builder().id(1L).position(0).build(),
                    Todo.builder().id(2L).position(1).build()
            );

            when(userService.getCurrentUserId()).thenReturn(KEYCLOAK_ID);
            when(userService.getByKeycloakId(KEYCLOAK_ID)).thenReturn(testUser);
            when(todoRepository.findByUserIdOrderByPositionAsc(testUser.getId())).thenReturn(existingTodos);
            when(todoRepository.save(any(Todo.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            TodoResponseDTO result = todoService.createTodo(title);

            // Then
            assertThat(result.getPosition()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("getMyTodos() tests")
    class GetMyTodosTests {

        @Test
        @DisplayName("Should return list of user todos")
        void shouldReturnUserTodos() {
            // Given
            List<Todo> todos = List.of(
                    Todo.builder().id(1L).title("Todo 1").completed(false).position(0).user(testUser).build(),
                    Todo.builder().id(2L).title("Todo 2").completed(true).position(1).user(testUser).build()
            );

            when(userService.getCurrentUserId()).thenReturn(KEYCLOAK_ID);
            when(userService.getByKeycloakId(KEYCLOAK_ID)).thenReturn(testUser);
            when(todoRepository.findByUserIdOrderByPositionAsc(testUser.getId())).thenReturn(todos);

            // When
            List<TodoResponseDTO> result = todoService.getMyTodos();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getTitle()).isEqualTo("Todo 1");
            assertThat(result.get(0).isCompleted()).isFalse();
            assertThat(result.get(1).getTitle()).isEqualTo("Todo 2");
            assertThat(result.get(1).isCompleted()).isTrue();

            verify(todoRepository).findByUserIdOrderByPositionAsc(testUser.getId());
        }

        @Test
        @DisplayName("Should return empty list when user has no todos")
        void shouldReturnEmptyListWhenNoTodos() {
            // Given
            when(userService.getCurrentUserId()).thenReturn(KEYCLOAK_ID);
            when(userService.getByKeycloakId(KEYCLOAK_ID)).thenReturn(testUser);
            when(todoRepository.findByUserIdOrderByPositionAsc(testUser.getId())).thenReturn(new ArrayList<>());

            // When
            List<TodoResponseDTO> result = todoService.getMyTodos();

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateTodo() tests")
    class UpdateTodoTests {

        @Test
        @DisplayName("Should update todo successfully")
        void shouldUpdateTodoSuccessfully() {
            // Given
            Long todoId = 1L;
            String newTitle = "Updated Title";
            boolean newCompleted = true;

            when(userService.getCurrentUserId()).thenReturn(KEYCLOAK_ID);
            when(userService.getByKeycloakId(KEYCLOAK_ID)).thenReturn(testUser);
            when(todoRepository.findByIdAndUserId(todoId, testUser.getId()))
                    .thenReturn(Optional.of(testTodo));

            ArgumentCaptor<Todo> todoCaptor = ArgumentCaptor.forClass(Todo.class);
            when(todoRepository.save(todoCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

            // When
            TodoResponseDTO result = todoService.updateTodo(todoId, newTitle, newCompleted);

            // Then
            Todo savedTodo = todoCaptor.getValue();
            assertThat(savedTodo.getTitle()).isEqualTo(newTitle);
            assertThat(savedTodo.isCompleted()).isEqualTo(newCompleted);

            assertThat(result.getTitle()).isEqualTo(newTitle);
            assertThat(result.isCompleted()).isEqualTo(newCompleted);

            verify(todoRepository).save(any(Todo.class));
        }

        @Test
        @DisplayName("Should throw NOT_FOUND when todo does not exist")
        void shouldThrowNotFoundWhenTodoNotFound() {
            // Given
            Long todoId = 999L;
            when(userService.getCurrentUserId()).thenReturn(KEYCLOAK_ID);
            when(userService.getByKeycloakId(KEYCLOAK_ID)).thenReturn(testUser);
            when(todoRepository.findByIdAndUserId(todoId, testUser.getId())).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> todoService.updateTodo(todoId, "Title", false))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException responseEx = (ResponseStatusException) ex;
                        assertThat(responseEx.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(responseEx.getReason()).isEqualTo("Todo not found");
                    });

            verify(todoRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteTodo() tests")
    class DeleteTodoTests {

        @Test
        @DisplayName("Should delete todo and reorder remaining todos")
        void shouldDeleteTodoAndReorder() throws Exception {
            // Given
            Long todoIdToDelete = 2L;
            List<Todo> todos = new ArrayList<>(List.of(
                    Todo.builder().id(1L).position(0).user(testUser).build(),
                    Todo.builder().id(2L).position(1).user(testUser).build(),
                    Todo.builder().id(3L).position(2).user(testUser).build()
            ));

            when(userService.getCurrentUserId()).thenReturn(KEYCLOAK_ID);
            when(userService.getByKeycloakId(KEYCLOAK_ID)).thenReturn(testUser);
            when(todoRepository.findByUserIdOrderByPositionAsc(testUser.getId())).thenReturn(todos);
            doNothing().when(todoRepository).delete(any(Todo.class));
            when(todoRepository.saveAll(anyList())).thenReturn(todos);

            // When
            todoService.deleteTodo(todoIdToDelete);

            // Then
            verify(todoRepository).delete(any(Todo.class));
            verify(todoRepository).saveAll(anyList());

            // Verify positions were updated
            assertThat(todos.get(0).getPosition()).isEqualTo(0);
            assertThat(todos.get(1).getPosition()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should throw NOT_FOUND when todo to delete does not exist")
        void shouldThrowNotFoundWhenTodoNotFound() {
            // Given
            Long todoId = 999L;
            List<Todo> todos = List.of(
                    Todo.builder().id(1L).position(0).build(),
                    Todo.builder().id(2L).position(1).build()
            );

            when(userService.getCurrentUserId()).thenReturn(KEYCLOAK_ID);
            when(userService.getByKeycloakId(KEYCLOAK_ID)).thenReturn(testUser);
            when(todoRepository.findByUserIdOrderByPositionAsc(testUser.getId())).thenReturn(todos);

            // When & Then
            assertThatThrownBy(() -> todoService.deleteTodo(todoId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException responseEx = (ResponseStatusException) ex;
                        assertThat(responseEx.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(responseEx.getReason()).isEqualTo("Todo not found");
                    });

            verify(todoRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("reorderTodo() tests")
    class ReorderTodoTests {

        @Test
        @DisplayName("Should reorder todo successfully - move from position 1 to 0")
        void shouldReorderTodoSuccessfully() {
            // Given
            Long todoIdToMove = 2L;
            int newPosition = 0;

            List<Todo> originalTodos = new ArrayList<>(List.of(
                    Todo.builder().id(1L).position(0).title("Todo 1").user(testUser).build(),
                    Todo.builder().id(2L).position(1).title("Todo 2").user(testUser).build(),
                    Todo.builder().id(3L).position(2).title("Todo 3").user(testUser).build()
            ));

            when(userService.getCurrentUserId()).thenReturn(KEYCLOAK_ID);
            when(userService.getByKeycloakId(KEYCLOAK_ID)).thenReturn(testUser);
            when(todoRepository.findByUserIdOrderByPositionAsc(testUser.getId()))
                    .thenReturn(originalTodos);

            ArgumentCaptor<List<Todo>> saveAllCaptor = ArgumentCaptor.forClass(List.class);
            when(todoRepository.saveAll(saveAllCaptor.capture())).thenReturn(originalTodos);

            // When
            List<TodoResponseDTO> result = todoService.reorderTodo(todoIdToMove, newPosition);

            // Then
            assertThat(result).hasSize(3);

            List<Todo> savedTodos = saveAllCaptor.getValue();
            assertThat(savedTodos.get(0).getId()).isEqualTo(2L);
            assertThat(savedTodos.get(0).getPosition()).isEqualTo(0);

            assertThat(savedTodos.get(1).getId()).isEqualTo(1L);
            assertThat(savedTodos.get(1).getPosition()).isEqualTo(1);

            assertThat(savedTodos.get(2).getId()).isEqualTo(3L);
            assertThat(savedTodos.get(2).getPosition()).isEqualTo(2);

            assertThat(result.get(0).getId()).isEqualTo(2L);
            assertThat(result.get(0).getPosition()).isEqualTo(0);
            assertThat(result.get(1).getId()).isEqualTo(1L);
            assertThat(result.get(1).getPosition()).isEqualTo(1);
            assertThat(result.get(2).getId()).isEqualTo(3L);
            assertThat(result.get(2).getPosition()).isEqualTo(2);

            verify(todoRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Should throw BAD_REQUEST when new position is invalid")
        void shouldThrowBadRequestWhenInvalidPosition() {
            // Given
            Long todoId = 1L;
            int invalidPosition = 10;
            List<Todo> todos = List.of(
                    Todo.builder().id(1L).position(0).build(),
                    Todo.builder().id(2L).position(1).build()
            );

            when(userService.getCurrentUserId()).thenReturn(KEYCLOAK_ID);
            when(userService.getByKeycloakId(KEYCLOAK_ID)).thenReturn(testUser);
            when(todoRepository.findByUserIdOrderByPositionAsc(testUser.getId())).thenReturn(todos);

            // When & Then
            assertThatThrownBy(() -> todoService.reorderTodo(todoId, invalidPosition))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException responseEx = (ResponseStatusException) ex;
                        assertThat(responseEx.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(responseEx.getReason()).isEqualTo("Invalid position");
                    });
        }

        @Test
        @DisplayName("Should throw NOT_FOUND when todo to reorder does not exist")
        void shouldThrowNotFoundWhenTodoNotFound() {
            // Given
            Long todoId = 999L;
            int newPosition = 0;
            List<Todo> todos = List.of(
                    Todo.builder().id(1L).position(0).build(),
                    Todo.builder().id(2L).position(1).build()
            );

            when(userService.getCurrentUserId()).thenReturn(KEYCLOAK_ID);
            when(userService.getByKeycloakId(KEYCLOAK_ID)).thenReturn(testUser);
            when(todoRepository.findByUserIdOrderByPositionAsc(testUser.getId())).thenReturn(todos);

            // When & Then
            assertThatThrownBy(() -> todoService.reorderTodo(todoId, newPosition))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException responseEx = (ResponseStatusException) ex;
                        assertThat(responseEx.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("Should handle moving todo to the end")
        void shouldHandleMoveToEnd() {
            // Given
            Long todoIdToMove = 1L;
            int newPosition = 2;

            List<Todo> originalTodos = new ArrayList<>(List.of(
                    Todo.builder().id(1L).position(0).title("Todo 1").user(testUser).build(),
                    Todo.builder().id(2L).position(1).title("Todo 2").user(testUser).build(),
                    Todo.builder().id(3L).position(2).title("Todo 3").user(testUser).build()
            ));

            when(userService.getCurrentUserId()).thenReturn(KEYCLOAK_ID);
            when(userService.getByKeycloakId(KEYCLOAK_ID)).thenReturn(testUser);
            when(todoRepository.findByUserIdOrderByPositionAsc(testUser.getId()))
                    .thenReturn(originalTodos);

            ArgumentCaptor<List<Todo>> saveAllCaptor = ArgumentCaptor.forClass(List.class);
            when(todoRepository.saveAll(saveAllCaptor.capture())).thenReturn(originalTodos);

            // When
            List<TodoResponseDTO> result = todoService.reorderTodo(todoIdToMove, newPosition);

            // Then
            assertThat(result).hasSize(3);

            List<Todo> savedTodos = saveAllCaptor.getValue();
            assertThat(savedTodos.get(0).getId()).isEqualTo(2L);
            assertThat(savedTodos.get(0).getPosition()).isEqualTo(0);

            assertThat(savedTodos.get(1).getId()).isEqualTo(3L);
            assertThat(savedTodos.get(1).getPosition()).isEqualTo(1);

            assertThat(savedTodos.get(2).getId()).isEqualTo(1L);
            assertThat(savedTodos.get(2).getPosition()).isEqualTo(2);

            assertThat(result.get(0).getId()).isEqualTo(2L);
            assertThat(result.get(1).getId()).isEqualTo(3L);
            assertThat(result.get(2).getId()).isEqualTo(1L);

            verify(todoRepository).saveAll(anyList());
        }
    }
}
