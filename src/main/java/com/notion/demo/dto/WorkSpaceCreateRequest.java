package com.notion.demo.dto;

import com.notion.demo.enums.WorkSpaceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkSpaceCreateRequest {
    @NotNull(message = "Name cannot be null")
    @Size(min = 3, max = 50, message = "Name must be between 3 and 50 characters")
    private String name;
    
    @NotNull(message = "WorkSpace type cannot be null")
    private WorkSpaceType workSpaceType;
}
