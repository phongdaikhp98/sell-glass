package com.sellglass.branch;

import com.sellglass.branch.dto.BranchRequest;
import com.sellglass.branch.dto.BranchResponse;

import java.util.List;
import java.util.UUID;

public interface BranchService {

    List<BranchResponse> findAll();

    BranchResponse findById(UUID id);

    BranchResponse create(BranchRequest request);

    BranchResponse update(UUID id, BranchRequest request);

    void delete(UUID id);
}
