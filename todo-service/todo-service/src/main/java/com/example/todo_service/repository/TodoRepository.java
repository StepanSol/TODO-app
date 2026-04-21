package com.example.todo_service.repository;

import com.example.todo_service.model.Todo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    List<Todo> findByUserId(Long userId);

    Optional<Todo> findByIdAndUserId(Long id, Long userId);

    List<Todo> findByUserIdOrderByPositionAsc(Long userId);
}
