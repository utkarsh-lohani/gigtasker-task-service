package com.gigtasker.taskservice.dto;

import com.gigtasker.taskservice.entity.Task;
import com.gigtasker.taskservice.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO implements Serializable {

    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private Long assignedUserId;
    private Long posterUserId;
    private LocalDateTime deadline;
    private BigDecimal minPay;
    private BigDecimal maxPay;
    private Integer maxBidsPerUser;

    public static TaskDTO fromEntity(Task task) {
        return TaskDTO.builder() // Map to DTOs
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .posterUserId(task.getPosterUserId())
                .assignedUserId(task.getAssignedUserId())
                .status(task.getStatus())
                .deadline(task.getDeadline())
                .minPay(task.getMinPay())
                .maxPay(task.getMaxPay())
                .maxBidsPerUser(task.getMaxBidsPerUser())
                .build();
    }
}
