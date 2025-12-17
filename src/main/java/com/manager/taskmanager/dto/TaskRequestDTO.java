package com.manager.taskmanager.dto;

import com.manager.taskmanager.model.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskRequestDTO {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title can be max 255 characters")
    private String title;

    @Size(max = 1000, message = "Description can be max 1000 characters")
    private String description;

    private Status status;
}
