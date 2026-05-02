package com.example.todo_service.it;

import com.example.todo_service.config.TestSecurityConfig;
import com.example.todo_service.model.*;
import com.example.todo_service.repository.TodoRepository;
import com.example.todo_service.repository.UserRepository;
import com.example.todo_service.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@SuppressWarnings("deprecation")
public class TodoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private UserService userService;

    private User testUser;
    private String keycloakId = "keycloak-user-123";

    @BeforeEach
    void setUp() {
        todoRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setKeycloakId(keycloakId);
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setFullName("Test User");
        testUser = userRepository.save(testUser);

        when(userService.getCurrentUserId()).thenReturn(keycloakId);
        when(userService.getByKeycloakId(keycloakId)).thenReturn(testUser);
    }

    @Test
    void createTodo_ShouldReturnCreatedTodo() throws Exception {
        CreateTodoRequestDTO request = new CreateTodoRequestDTO();
        request.setTitle("New Task");

        MvcResult result = mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Task"))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.position").value(0))
                .andReturn();

        TodoResponseDTO response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                TodoResponseDTO.class
        );
        assertThat(response.getId()).isNotNull();
    }

    @Test
    void createTodo_WithBlankTitle_ShouldReturnBadRequest() throws Exception {
        CreateTodoRequestDTO request = new CreateTodoRequestDTO();
        request.setTitle("");

        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTodo_WithTitleTooLong_ShouldReturnBadRequest() throws Exception {
        CreateTodoRequestDTO request = new CreateTodoRequestDTO();
        request.setTitle("a".repeat(256));

        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMyTodos_ShouldReturnUserTodos() throws Exception {
        createTodoEntity("Task 1", 0);
        createTodoEntity("Task 2", 1);

        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Task 1"))
                .andExpect(jsonPath("$[1].title").value("Task 2"));
    }

    @Test
    void getMyTodos_WhenNoTodos_ShouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void updateTodo_ShouldUpdateTitleAndCompleted() throws Exception {
        Todo todo = createTodoEntity("Old Title", 0);

        UpdateTodoRequestDTO request = new UpdateTodoRequestDTO();
        request.setTitle("New Title");
        request.setCompleted(true);

        mockMvc.perform(put("/api/todos/{id}", todo.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"))
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.position").value(0));
    }

    @Test
    void updateTodo_WithInvalidId_ShouldReturnNotFound() throws Exception {
        UpdateTodoRequestDTO request = new UpdateTodoRequestDTO();
        request.setTitle("New Title");
        request.setCompleted(true);

        mockMvc.perform(put("/api/todos/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteTodo_ShouldRemoveTodoAndReorderRemaining() throws Exception {
        Todo todo1 = createTodoEntity("Task 1", 0);
        Todo todo2 = createTodoEntity("Task 2", 1);
        Todo todo3 = createTodoEntity("Task 3", 2);

        mockMvc.perform(delete("/api/todos/{id}", todo2.getId()))
                .andExpect(status().isOk());

        List<Todo> remaining = todoRepository.findByUserIdOrderByPositionAsc(testUser.getId());
        assertThat(remaining).hasSize(2);
        assertThat(remaining.get(0).getTitle()).isEqualTo("Task 1");
        assertThat(remaining.get(0).getPosition()).isEqualTo(0);
        assertThat(remaining.get(1).getTitle()).isEqualTo("Task 3");
        assertThat(remaining.get(1).getPosition()).isEqualTo(1);
    }

    @Test
    void deleteTodo_WithInvalidId_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(delete("/api/todos/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void reorderTodo_ShouldChangePosition() throws Exception {
        Todo todo1 = createTodoEntity("Task 1", 0);
        Todo todo2 = createTodoEntity("Task 2", 1);
        Todo todo3 = createTodoEntity("Task 3", 2);

        ReorderRequestDTO request = new ReorderRequestDTO();
        request.setNewPosition(2);

        mockMvc.perform(put("/api/todos/{id}/reorder", todo1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(todo2.getId()))
                .andExpect(jsonPath("$[1].id").value(todo3.getId()))
                .andExpect(jsonPath("$[2].id").value(todo1.getId()));

        List<Todo> todos = todoRepository.findByUserIdOrderByPositionAsc(testUser.getId());
        assertThat(todos.get(0).getPosition()).isEqualTo(0);
        assertThat(todos.get(1).getPosition()).isEqualTo(1);
        assertThat(todos.get(2).getPosition()).isEqualTo(2);
    }

    @Test
    void reorderTodo_WithInvalidPosition_ShouldReturnBadRequest() throws Exception {
        Todo todo = createTodoEntity("Task", 0);

        ReorderRequestDTO request = new ReorderRequestDTO();
        request.setNewPosition(5);

        mockMvc.perform(put("/api/todos/{id}/reorder", todo.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reorderTodo_WithNegativePosition_ShouldReturnBadRequest() throws Exception {
        Todo todo = createTodoEntity("Task", 0);

        ReorderRequestDTO request = new ReorderRequestDTO();
        request.setNewPosition(-1);

        mockMvc.perform(put("/api/todos/{id}/reorder", todo.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reorderTodo_WithNullPosition_ShouldReturnBadRequest() throws Exception {
        ReorderRequestDTO request = new ReorderRequestDTO();
        request.setNewPosition(null);

        mockMvc.perform(put("/api/todos/{id}/reorder", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    private Todo createTodoEntity(String title, int position) {
        Todo todo = Todo.builder()
                .title(title)
                .completed(false)
                .position(position)
                .user(testUser)
                .build();
        return todoRepository.save(todo);
    }
}