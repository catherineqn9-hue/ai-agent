package com.sherry.supervision.service;

import com.sherry.supervision.dto.SupervisionItemRequest;
import com.sherry.supervision.entity.SupervisionItem;
import java.util.List;
import java.util.UUID;

public interface SupervisionItemService {

    List<SupervisionItem> list(String status, String keyword);

    SupervisionItem get(UUID id);

    SupervisionItem create(SupervisionItemRequest request);

    SupervisionItem update(UUID id, SupervisionItemRequest request);

    SupervisionItem updateStatus(UUID id, String status);

    void delete(UUID id);
}
