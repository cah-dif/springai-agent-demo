package com.demo.springaidemo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "todo")
@Data
public class Todo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content; //待办内容

    private Boolean finished; //是否完成

    private LocalDateTime createTime; //创建时间

    @PrePersist
    public void preSave() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
        if (finished == null) {
            finished = false;
        }
    }
}
