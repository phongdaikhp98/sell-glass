package com.sellglass.branch;

import com.sellglass.branch.dto.BranchRequest;
import com.sellglass.branch.dto.BranchResponse;
import com.sellglass.common.exception.AppException;
import com.sellglass.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BranchServiceImplTest {

    @Mock
    private BranchRepository branchRepository;

    @InjectMocks
    private BranchServiceImpl service;

    private UUID branchId;
    private Branch branch;

    @BeforeEach
    void setUp() {
        branchId = UUID.randomUUID();
        branch = new Branch();
        branch.setId(branchId);
        branch.setName("Branch 1");
        branch.setAddress("123 Street");
        branch.setPhone("0901234567");
        branch.setOpenTime(LocalTime.of(8, 0));
        branch.setCloseTime(LocalTime.of(20, 0));
        branch.setActive(true);
    }

    private BranchRequest buildRequest() {
        BranchRequest request = new BranchRequest();
        request.setName("Branch 1");
        request.setAddress("123 Street");
        request.setPhone("0901234567");
        request.setOpenTime(LocalTime.of(8, 0));
        request.setCloseTime(LocalTime.of(20, 0));
        request.setActive(true);
        return request;
    }

    @Test
    @DisplayName("findAll should return all branches")
    void findAll_success() {
        when(branchRepository.findAll()).thenReturn(List.of(branch));

        List<BranchResponse> result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Branch 1");
    }

    @Test
    @DisplayName("findById should return branch when exists")
    void findById_success() {
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));

        BranchResponse result = service.findById(branchId);

        assertThat(result.getId()).isEqualTo(branchId);
        assertThat(result.getOpenTime()).isEqualTo(LocalTime.of(8, 0));
    }

    @Test
    @DisplayName("findById should throw NOT_FOUND when branch missing")
    void findById_notFound() {
        when(branchRepository.findById(branchId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(branchId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("create should save and return new branch")
    void create_success() {
        when(branchRepository.save(any(Branch.class))).thenAnswer(inv -> {
            Branch b = inv.getArgument(0);
            b.setId(branchId);
            return b;
        });

        BranchResponse result = service.create(buildRequest());

        assertThat(result.getName()).isEqualTo("Branch 1");
        assertThat(result.isActive()).isTrue();
        verify(branchRepository).save(any(Branch.class));
    }

    @Test
    @DisplayName("update should modify all fields")
    void update_success() {
        BranchRequest request = buildRequest();
        request.setName("Branch Updated");
        request.setActive(false);

        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));
        when(branchRepository.save(any(Branch.class))).thenAnswer(inv -> inv.getArgument(0));

        BranchResponse result = service.update(branchId, request);

        assertThat(result.getName()).isEqualTo("Branch Updated");
        assertThat(result.isActive()).isFalse();
    }

    @Test
    @DisplayName("update should throw NOT_FOUND when branch missing")
    void update_notFound() {
        when(branchRepository.findById(branchId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(branchId, buildRequest()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
        verify(branchRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete should remove branch")
    void delete_success() {
        when(branchRepository.existsById(branchId)).thenReturn(true);

        service.delete(branchId);

        verify(branchRepository).deleteById(branchId);
    }

    @Test
    @DisplayName("delete should throw NOT_FOUND when branch missing")
    void delete_notFound() {
        when(branchRepository.existsById(branchId)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(branchId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
        verify(branchRepository, never()).deleteById(any());
    }
}
