package com.sellglass.branch.dto;

import com.sellglass.branch.Branch;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class BranchResponse {

    private UUID id;
    private String name;
    private String address;
    private String phone;
    private LocalTime openTime;
    private LocalTime closeTime;
    private boolean isActive;
    private LocalDateTime createdAt;

    public static BranchResponse from(Branch branch) {
        BranchResponse response = new BranchResponse();
        response.id = branch.getId();
        response.name = branch.getName();
        response.address = branch.getAddress();
        response.phone = branch.getPhone();
        response.openTime = branch.getOpenTime();
        response.closeTime = branch.getCloseTime();
        response.isActive = branch.isActive();
        response.createdAt = branch.getCreatedAt();
        return response;
    }
}
