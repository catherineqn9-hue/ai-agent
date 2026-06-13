package com.sherry.supervision.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sherry.supervision.entity.SupervisionItem;
import com.sherry.supervision.exception.BusinessConflictException;
import com.sherry.supervision.mapper.SupervisionItemMapper;
import com.sherry.supervision.service.impl.SupervisionItemServiceImpl;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SupervisionItemServiceImplTest {

    private SupervisionItemMapper mapper;
    private SupervisionItemServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(SupervisionItemMapper.class);
        service = new SupervisionItemServiceImpl(mapper);
    }

    @Test
    void shouldAllowPendingAssignToInProgress() {
        UUID id = UUID.randomUUID();
        SupervisionItem item = item(id, "pending_assign");
        when(mapper.selectById(id)).thenReturn(item);

        SupervisionItem updated = service.updateStatus(id, "in_progress");

        assertThat(updated.getStatus()).isEqualTo("in_progress");
        verify(mapper).updateById(item);
    }

    @Test
    void shouldAcceptChineseStatusLabel() {
        UUID id = UUID.randomUUID();
        SupervisionItem item = item(id, "pending_assign");
        when(mapper.selectById(id)).thenReturn(item);

        SupervisionItem updated = service.updateStatus(id, "进行中");

        assertThat(updated.getStatus()).isEqualTo("in_progress");
        assertThat(updated.getStatusName()).isEqualTo("进行中");
        verify(mapper).updateById(item);
    }

    @Test
    void shouldGetItemById() {
        UUID id = UUID.randomUUID();
        SupervisionItem item = item(id, "pending_assign");
        when(mapper.selectById(id)).thenReturn(item);

        SupervisionItem result = service.get(id);

        assertThat(result).isSameAs(item);
        verify(mapper).selectById(id);
    }

    @Test
    void shouldRejectCompletedToInProgress() {
        UUID id = UUID.randomUUID();
        SupervisionItem item = item(id, "completed");
        when(mapper.selectById(id)).thenReturn(item);

        assertThatThrownBy(() -> service.updateStatus(id, "in_progress"))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("状态不允许从 已完成 流转到 进行中");
    }

    @Test
    void shouldRejectUnknownStatus() {
        UUID id = UUID.randomUUID();
        SupervisionItem item = item(id, "pending_assign");
        when(mapper.selectById(id)).thenReturn(item);

        assertThatThrownBy(() -> service.updateStatus(id, "unknown_status"))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("未知事项状态");
    }

    private SupervisionItem item(UUID id, String status) {
        SupervisionItem item = new SupervisionItem();
        item.setId(id);
        item.setItemNo("ITEM-TEST");
        item.setTitle("状态流转测试");
        item.setStatus(status);
        return item;
    }
}
