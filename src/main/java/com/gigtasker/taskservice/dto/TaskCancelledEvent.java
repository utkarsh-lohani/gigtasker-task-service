package com.gigtasker.taskservice.dto;

import java.util.UUID;

public record TaskCancelledEvent(
        Long taskId,
        UUID posterId
) {}
