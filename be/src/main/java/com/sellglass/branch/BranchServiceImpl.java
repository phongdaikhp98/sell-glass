package com.sellglass.branch;

import com.sellglass.branch.dto.BranchRequest;
import com.sellglass.branch.dto.BranchResponse;
import com.sellglass.common.exception.AppException;
import com.sellglass.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;

    @Override
    public List<BranchResponse> findAll() {
        return branchRepository.findAll().stream()
                .map(BranchResponse::from)
                .toList();
    }

    @Override
    public BranchResponse findById(UUID id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Branch not found"));
        return BranchResponse.from(branch);
    }

    @Override
    @Transactional
    public BranchResponse create(BranchRequest request) {
        Branch branch = new Branch();
        branch.setName(request.getName());
        branch.setAddress(request.getAddress());
        branch.setPhone(request.getPhone());
        branch.setOpenTime(request.getOpenTime());
        branch.setCloseTime(request.getCloseTime());
        branch.setActive(request.isActive());
        return BranchResponse.from(branchRepository.save(branch));
    }

    @Override
    @Transactional
    public BranchResponse update(UUID id, BranchRequest request) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Branch not found"));
        branch.setName(request.getName());
        branch.setAddress(request.getAddress());
        branch.setPhone(request.getPhone());
        branch.setOpenTime(request.getOpenTime());
        branch.setCloseTime(request.getCloseTime());
        branch.setActive(request.isActive());
        return BranchResponse.from(branchRepository.save(branch));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!branchRepository.existsById(id)) {
            throw new AppException(ErrorCode.NOT_FOUND, "Branch not found");
        }
        branchRepository.deleteById(id);
    }
}
