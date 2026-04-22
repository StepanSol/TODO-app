package com.example.todo_service.it;

import com.example.todo_service.config.TestSecurityConfig;
import com.example.todo_service.controller.TodoController;
import com.example.todo_service.model.*;
import com.example.todo_service.service.TodoService;
import com.example.todo_service.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TodoController.class)
@Import(TestSecurityConfig.class)
@DisplayName("TodoController Integration Tests")
class TodoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TodoService todoService;

    @MockitoBean
    private UserService userService;

    private TodoResponseDTO testTodoResponse;
    private CreateTodoRequestDTO createRequest;
    private UpdateTodoRequestDTO updateRequest;
    private ReorderRequestDTO reorderRequest;

    @BeforeEach
    void setUp() {
        testTodoResponse = new TodoResponseDTO(1L, "Test Todo", false, 0);

        createRequest = new CreateTodoRequestDTO();
        createRequest.setTitle("New Todo");

        updateRequest = new UpdateTodoRequestDTO();
        updateRequest.setTitle("Updated Todo");
        updateRequest.setCompleted(true);

        reorderRequest = new ReorderRequestDTO();
        reorderRequest.setNewPosition(1);
    }

    @Nested
    @DisplayName("POST /api/todos - Create Todo")
    class CreateTodoTests {

        @Test
        @DisplayName("Should create todo successfully")
        void shouldCreateTodoSuccessfully() throws Exception {
            when(todoService.createTodo(anyString())).thenReturn(testTodoResponse);

            mockMvc.perform(post("/api/todos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.title").value("Test Todo"))
                    .andExpect(jsonPath("$.completed").value(false))
                    .andExpect(jsonPath("$.position").value(0));

            verify(todoService).createTodo("New Todo");
        }

        @Test
        @DisplayName("Should return 400 when title is blank")
        void shouldReturnBadRequestWhenTitleIsBlank() throws Exception {
            createRequest.setTitle("");

            mockMvc.perform(post("/api/todos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isBadRequest());

            verify(todoService, never()).createTodo(any());
        }

        @Test
        @DisplayName("Should return 400 when title exceeds max length")
        void shouldReturnBadRequestWhenTitleTooLong() throws Exception {
            createRequest.setTitle("a".repeat(256));

            mockMvc.perform(post("/api/todos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isBadRequest());

            verify(todoService, never()).createTodo(any());
        }

        @Test
        @DisplayName("Should return 400 when title is null")
        void shouldReturnBadRequestWhenTitleIsNull() throws Exception {
            createRequest.setTitle(null);

            mockMvc.perform(post("/api/todos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isBadRequest());

            verify(todoService, never()).createTodo(any());
        }
    }

    @Nested
    @DisplayName("GET /api/todos - Get My Todos")
    class GetMyTodosTests {

        @Test
        @DisplayName("Should return list of todos")
        void shouldReturnListOfTodos() throws Exception {
            List<TodoResponseDTO> todos = List.of(
                    new TodoResponseDTO(1L, "Todo 1", false, 0),
                    new TodoResponseDTO(2L, "Todo 2", true, 1)
            );
            when(todoService.getMyTodos()).thenReturn(todos);

            mockMvc.perform(get("/api/todos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id").value(1L))
                    .andExpect(jsonPath("$[0].title").value("Todo 1"))
                    .andExpect(jsonPath("$[1].id").value(2L))
                    .andExpect(jsonPath("$[1].title").value("Todo 2"));

            verify(todoService).getMyTodos();
        }

        @Test
        @DisplayName("Should return empty list when no todos")
        void shouldReturnEmptyListWhenNoTodos() throws Exception {
            when(todoService.getMyTodos()).thenReturn(List.of());

            mockMvc.perform(get("/api/todos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(todoService).getMyTodos();
        }
    }

    @Nested
    @DisplayName("PUT /api/todos/{id} - Update Todo")
    class UpdateTodoTests {

        @Test
        @DisplayName("Should update todo successfully")
        void shouldUpdateTodoSuccessfully() throws Exception {
            when(todoService.updateTodo(eq(1L), eq("Updated Todo"), eq(true)))
                    .thenReturn(new TodoResponseDTO(1L, "Updated Todo", true, 0));

            mockMvc.perform(put("/api/todos/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated Todo"))
                    .andExpect(jsonPath("$.completed").value(true));

            verify(todoService).updateTodo(1L, "Updated Todo", true);
        }

        @Test
        @DisplayName("Should return 400 when title is blank")
        void shouldReturnBadRequestWhenTitleBlank() throws Exception {
            updateRequest.setTitle("");

            mockMvc.perform(put("/api/todos/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isBadRequest());

            verify(todoService, never()).updateTodo(anyLong(), anyString(), anyBoolean());
        }

        @Test
        @DisplayName("Should return 400 when title is null")
        void shouldReturnBadRequestWhenTitleNull() throws Exception {
            updateRequest.setTitle(null);

            mockMvc.perform(put("/api/todos/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isBadRequest());

            verify(todoService, never()).updateTodo(anyLong(), anyString(), anyBoolean());
        }
    }

    @Nested
    @DisplayName("DELETE /api/todos/{id} - Delete Todo")
    class DeleteTodoTests {

        @Test
        @DisplayName("Should delete todo successfully")
        void shouldDeleteTodoSuccessfully() throws Exception {
            doNothing().when(todoService).deleteTodo(anyLong());

            mockMvc.perform(delete("/api/todos/1"))
                    .andExpect(status().isOk());

            verify(todoService).deleteTodo(1L);
        }
    }

    @Nested
    @DisplayName("PUT /api/todos/{id}/reorder - Reorder Todo")
    class ReorderTodoTests {

        @Test
        @DisplayName("Should reorder todo successfully")
        void shouldReorderTodoSuccessfully() throws Exception {
            List<TodoResponseDTO> reorderedTodos = List.of(
                    new TodoResponseDTO(2L, "Todo 2", false, 0),
                    new TodoResponseDTO(1L, "Todo 1", false, 1)
            );
            when(todoService.reorderTodo(eq(1L), eq(1))).thenReturn(reorderedTodos);

            mockMvc.perform(put("/api/todos/1/reorder")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reorderRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id").value(2L))
                    .andExpect(jsonPath("$[1].id").value(1L));

            verify(todoService).reorderTodo(eq(1L), eq(1));
        }

        @Test
        @DisplayName("Should return 400 when new position is negative")
        void shouldReturnBadRequestWhenNegativePosition() throws Exception {
            reorderRequest.setNewPosition(-1);

            mockMvc.perform(put("/api/todos/1/reorder")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reorderRequest)))
                    .andExpect(status().isBadRequest());

            // Не проверяем вызов сервиса, т.к. валидация должна сработать до него
            verify(todoService, never()).reorderTodo(anyLong(), anyInt());
        }

        @Test
        @DisplayName("Should return 400 when new position is null")
        void shouldReturnBadRequestWhenPositionNull() throws Exception {
            // Отправляем JSON без поля newPosition
            String jsonWithoutPosition = "{}";

            mockMvc.perform(put("/api/todos/1/reorder")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonWithoutPosition))
                    .andExpect(status().isBadRequest());

            verify(todoService, never()).reorderTodo(anyLong(), anyInt());
        }
    }
}