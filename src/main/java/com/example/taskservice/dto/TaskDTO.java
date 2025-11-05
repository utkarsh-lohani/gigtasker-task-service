package com.example.taskservice.dto;

import com.example.taskservice.entity.Task;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO implements Serializable {

    private Long id;
    private String title;
    private String description;
    private String status;
    private Long posterUserId;

    public static TaskDTO fronEntity(Task task) {
        return TaskDTO.builder() // Map to DTOs
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .posterUserId(task.getPosterUserId())
                .status(task.getStatus())
                .build();
    }
}
