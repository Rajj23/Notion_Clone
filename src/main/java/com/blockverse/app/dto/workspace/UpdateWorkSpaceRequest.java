package com.blockverse.app.dto.workspace;

import com.blockverse.app.enums.WorkSpaceType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateWorkSpaceRequest {
    
    @NotNull(message = "Name cannot be null")
    private String name;
    
    @NotNull(message = "WorkSpace name cannot be null")
    private WorkSpaceType workSpaceType;
}
