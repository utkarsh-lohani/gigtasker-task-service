package com.gigtasker.taskservice.service;

import com.gigtasker.taskservice.dto.TaskDTO;
import com.gigtasker.taskservice.entity.Task;
import com.gigtasker.taskservice.enums.TaskStatus;
import com.gigtasker.taskservice.exception.ResourceNotFoundException;
import com.gigtasker.taskservice.exception.UnauthorizedAccessException;
import com.gigtasker.taskservice.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    // For Rabbit MQ
    private final RabbitTemplate rabbitTemplate;
    private final WebClient.Builder webClientBuilder;

    public static final String EXCHANGE_NAME = "task-exchange";
    public static final String ROUTING_KEY = "task.created";
    public static final String TASK_COMPLETED_KEY = "task.completed";

    @Transactional
    public TaskDTO createTask(TaskDTO taskDTO) {

        int limit = (taskDTO.getMaxBidsPerUser() != null) ? taskDTO.getMaxBidsPerUser() : 3;
        if (limit > 10) limit = 10; // Hard cap
        if (limit < 1) limit = 1;

        Task task = Task.builder()
                .title(taskDTO.getTitle())
                .description(taskDTO.getDescription())
                .posterUserId(taskDTO.getPosterUserId())
                .deadline(taskDTO.getDeadline())
                .minPay(taskDTO.getMinPay())
                .maxPay(taskDTO.getMaxPay())
                .maxBidsPerUser(limit)
                .build();

        Task savedTask = taskRepository.save(task);

        TaskDTO savedDTO = TaskDTO.fromEntity(savedTask);

        log.info("Publishing event to RabbitMQ : " + ROUTING_KEY);
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, ROUTING_KEY, savedDTO);

        return savedDTO;
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> findTasksByUserId(Long userId) {
        return taskRepository.findTaskByPosterUserId(userId)
                .stream()
                .map(TaskDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> findAllTasks() {
        return taskRepository.findAll() // 1. Get all Task entities
                .stream()             // 2. Stream them
                .map(TaskDTO::fromEntity)
                .toList(); // 4. Collect as a List
    }

    @Transactional(readOnly = true)
    public TaskDTO findTaskById(Long taskId) {
        return taskRepository.findById(taskId)
                .map(TaskDTO::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
    }

    @Transactional
    public TaskDTO assignTask(Long taskId, Long assigneeUserId) {
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
        task.setAssignedUserId(assigneeUserId);
        Task savedTask = taskRepository.save(task);

        // 4. We can also publish a "task.assigned" event here!
        return TaskDTO.fromEntity(savedTask);
    }

    @Transactional
    public TaskDTO completeTask(Long taskId) {
        // 1. Get the current user's token
        String token = ((JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication())
                .getToken().getTokenValue();

        // 2. Call user-service to get the REAL ID of the caller
        Long currentUserId = getCurrentUserIdFromToken(token);

        // 3. Find the task
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        // 4. SECURITY: Only the assigned user can complete it!
        if (!currentUserId.equals(task.getAssignedUserId())) {
            throw new UnauthorizedAccessException("You are not the assigned worker for this task!");
        }

        // 5. Update status
        task.setStatus(TaskStatus.COMPLETED);
        Task savedTask = taskRepository.save(task);

        TaskDTO taskDTO = TaskDTO.fromEntity(savedTask);

        rabbitTemplate.convertAndSend(EXCHANGE_NAME, TASK_COMPLETED_KEY, taskDTO);

        return taskDTO;
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> findTasksByIds(List<Long> ids) {
        return taskRepository.findByIdIn(ids)
                .stream()
                // We're re-using the TaskDTO's builder/factory method.
                .map(TaskDTO::fromEntity).toList();
    }

    private Long getCurrentUserIdFromToken(String token) {
        // We create a tiny record/class just to hold the ID response
        record UserResponse(Long id) {}

        UserResponse user = webClientBuilder.build()
                .get().uri("http://user-service/api/v1/users/me")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(UserResponse.class)
                .block(); // Blocking is fine here (transactional method)

        if (user == null) throw new UsernameNotFoundException("User not found");
        return user.id();
    }
}
