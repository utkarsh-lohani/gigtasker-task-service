package com.gigtasker.taskservice.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class UserDTO {

    private Long id;
    private UUID keycloakId;
}
