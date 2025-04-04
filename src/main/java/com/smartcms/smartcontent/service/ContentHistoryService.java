package com.smartcms.smartcontent.service;

import com.smartcms.smartcontent.model.ContentHistory;
import com.smartcms.smartcontent.repository.ContentHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContentHistoryService {

    private final ContentHistoryRepository contentHistoryRepository;

    public List<ContentHistory> getContentHistory(String contentId) {
        return contentHistoryRepository.findByContentSnapshotId(contentId);
    }

}
