package com.sherry.supervision.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sherry.supervision.dto.AssignmentCandidateResponse;
import com.sherry.supervision.entity.AppUser;
import com.sherry.supervision.entity.SupervisionItem;
import com.sherry.supervision.mapper.AppUserMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AssignmentRecommendationServiceTest {

    private AppUserMapper appUserMapper;
    private SupervisionItemService supervisionItemService;
    private AssignmentRecommendationService service;

    @BeforeEach
    void setUp() {
        appUserMapper = mock(AppUserMapper.class);
        supervisionItemService = mock(SupervisionItemService.class);
        service = new AssignmentRecommendationService(appUserMapper, supervisionItemService);
    }

    @Test
    void shouldPreferUserWithRequestedRoleAndRequireHumanReview() {
        UUID itemId = UUID.randomUUID();
        SupervisionItem item = new SupervisionItem();
        item.setPriority("normal");
        when(supervisionItemService.get(itemId)).thenReturn(item);
        when(appUserMapper.selectList(any())).thenReturn(List.of(
                user("reviewer_user", "审核人", "reviewer"),
                user("owner_user", "主责人", "owner")));

        List<AssignmentCandidateResponse> candidates = service.recommend(itemId, "owner", "operations");

        assertThat(candidates).hasSize(2);
        assertThat(candidates.get(0).assigneeUserId()).isEqualTo("owner_user");
        assertThat(candidates.get(0).roleType()).isEqualTo("owner");
        assertThat(candidates.get(0).requiresHumanReview()).isTrue();
        assertThat(candidates.get(0).confidence()).isGreaterThan(candidates.get(1).confidence());
    }

    private AppUser user(String username, String displayName, String roleKey) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setDepartmentId("operations");
        user.setDepartmentName("运营部");
        user.setRoleKey(roleKey);
        user.setRoleName(displayName);
        user.setEnabled(true);
        return user;
    }
}
