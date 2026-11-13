package com.demo.springaidemo.repository;

import com.demo.springaidemo.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoRepository extends JpaRepository<Todo, Long> {
}
