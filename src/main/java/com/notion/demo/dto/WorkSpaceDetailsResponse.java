package com.notion.demo.dto;

import com.notion.demo.enums.WorkSpaceRole;
import com.notion.demo.enums.WorkSpaceType;
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
