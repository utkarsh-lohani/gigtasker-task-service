package com.gigtasker.taskservice.entity;

import com.gigtasker.taskservice.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private Long posterUserId;
    private Long assignedUserId;
    private LocalDateTime deadline;
    private BigDecimal minPay;
    private BigDecimal maxPay;

    @Builder.Default
    private Integer maxBidsPerUser = 3; // Default is 3

    @Enumerated(EnumType.STRING) @Builder.Default
    private TaskStatus status = TaskStatus.OPEN;
}
