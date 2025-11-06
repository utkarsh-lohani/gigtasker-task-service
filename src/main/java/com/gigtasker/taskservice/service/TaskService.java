package com.gigtasker.taskservice.service;

import com.gigtasker.taskservice.dto.TaskDTO;
import com.gigtasker.taskservice.entity.Task;
import com.gigtasker.taskservice.enums.TaskStatus;
import com.gigtasker.taskservice.exception.ResourceNotFoundException;
import com.gigtasker.taskservice.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    // For Rabbit MQ
    private final RabbitTemplate rabbitTemplate;

    public static final String EXCHANGE_NAME = "task-exchange";
    public static final String ROUTING_KEY = "task.created";

    @Transactional
    public TaskDTO createTask(TaskDTO taskDTO) {
        Task task = Task.builder()
                .id(taskDTO.getId())
                .title(taskDTO.getTitle())
                .description(taskDTO.getDescription())
                .posterUserId(taskDTO.getPosterUserId()).build();

        Task savedTask = taskRepository.save(task);

        TaskDTO savedDTO = TaskDTO.fronEntity(savedTask);

        log.info("Publishing event to RabbitMQ : " + ROUTING_KEY);
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, ROUTING_KEY, savedDTO);

        return savedDTO;
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> findTasksByUserId(Long userId) {
        return taskRepository.findTaskByPosterUserId(userId)
                .stream()
                .map(TaskDTO::fronEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> findAllTasks() {
        return taskRepository.findAll() // 1. Get all Task entities
                .stream()             // 2. Stream them
                .map(TaskDTO::fronEntity)
                .toList(); // 4. Collect as a List
    }

    @Transactional(readOnly = true)
    public TaskDTO findTaskById(Long taskId) {
        return taskRepository.findById(taskId)
                .map(TaskDTO::fronEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
    }

    @Transactional
    public TaskDTO assignTask(Long taskId) {
        // 1. Find the task
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        // 2. Check if it's already assigned
        if (task.getStatus() != TaskStatus.OPEN) {
            // You could create a custom exception here
            throw new IllegalStateException("Task is not OPEN and cannot be assigned.");
        }

        // 3. Update the status
        task.setStatus(TaskStatus.ASSIGNED);
        Task savedTask = taskRepository.save(task);

        // 4. We can also publish a "task.assigned" event here!
        return TaskDTO.fronEntity(savedTask);
    }
}
