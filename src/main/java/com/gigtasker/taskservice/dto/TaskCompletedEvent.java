package com.gigtasker.taskservice.dto;

import java.util.UUID;

public record TaskCompletedEvent(TaskDTO task, UUID posterId, UUID workerId) {}
