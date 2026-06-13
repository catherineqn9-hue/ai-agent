package com.sherry.supervision.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sherry.supervision.dto.AssignItemRequest;
import com.sherry.supervision.entity.AppUser;
import com.sherry.supervision.entity.ItemAssignee;
import com.sherry.supervision.entity.SupervisionItem;
import com.sherry.supervision.mapper.ItemAssigneeMapper;
import com.sherry.supervision.service.impl.ItemAssignmentServiceImpl;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ItemAssignmentServiceImplTest {

    private ItemAssigneeMapper itemAssigneeMapper;
    private SupervisionItemService supervisionItemService;
    private ItemAssignmentService service;

    @BeforeEach
    void setUp() {
        itemAssigneeMapper = mock(ItemAssigneeMapper.class);
        supervisionItemService = mock(SupervisionItemService.class);
        service = new ItemAssignmentServiceImpl(itemAssigneeMapper, supervisionItemService);
    }

    @Test
    void shouldRecordAssignedByWhenAssigningItem() {
        UUID itemId = UUID.randomUUID();
        when(supervisionItemService.get(itemId)).thenReturn(new SupervisionItem());
        AppUser currentUser = currentUser();

        service.assign(itemId, new AssignItemRequest(
                "zhangsan", "张三", "owner", null, null, "请负责本周数据汇总"), currentUser);

        ArgumentCaptor<ItemAssignee> captor = ArgumentCaptor.forClass(ItemAssignee.class);
        verify(itemAssigneeMapper).insert(captor.capture());
        ItemAssignee assignment = captor.getValue();
        assertThat(assignment.getAssignedByUserId()).isEqualTo("manager");
        assertThat(assignment.getAssignedByName()).isEqualTo("李四");
        assertThat(assignment.getAssigneeUserId()).isEqualTo("zhangsan");
        assertThat(assignment.getAssignmentNote()).isEqualTo("请负责本周数据汇总");
        assertThat(assignment.getConfirmStatus()).isEqualTo("pending");
    }

    private AppUser currentUser() {
        AppUser user = new AppUser();
        user.setUsername("manager");
        user.setDisplayName("李四");
        return user;
    }
}
