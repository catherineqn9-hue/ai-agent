package com.sherry.supervision.service;

import com.sherry.supervision.dto.AssignItemRequest;
import com.sherry.supervision.dto.RejectAssignmentRequest;
import com.sherry.supervision.entity.AppUser;
import com.sherry.supervision.entity.ItemAssignee;
import java.util.List;
import java.util.UUID;

public interface ItemAssignmentService {

    ItemAssignee assign(UUID itemId, AssignItemRequest request, AppUser assignedBy);

    List<ItemAssignee> listByItem(UUID itemId);

    List<ItemAssignee> listByAssignee(String username);

    ItemAssignee getCurrentAssignment(UUID itemId, String username);

    ItemAssignee confirmReceive(UUID itemId, AppUser currentUser);

    ItemAssignee reject(UUID itemId, RejectAssignmentRequest request, AppUser currentUser);
}
