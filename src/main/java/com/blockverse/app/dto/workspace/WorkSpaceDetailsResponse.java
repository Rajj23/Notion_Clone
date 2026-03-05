package com.blockverse.app.dto.workspace;

import com.blockverse.app.dto.user.OwnerInfo;
import com.blockverse.app.enums.WorkSpaceRole;
import com.blockverse.app.enums.WorkSpaceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkSpaceDetailsResponse {
    private int id;
    private String name;
    private WorkSpaceType workSpaceType;;
    private OwnerInfo ownerInfo; 
    private int memberCount;
    private WorkSpaceRole userRoleInWorkSpace;
}
