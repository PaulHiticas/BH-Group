package com.bhgroup.pms.service.mapper;

import com.bhgroup.pms.domain.IcalImportFeed;
import com.bhgroup.pms.dto.ical.IcalImportFeedResponse;
import org.springframework.stereotype.Component;

@Component
public class IcalImportFeedMapper {

    public IcalImportFeedResponse toResponse(IcalImportFeed feed) {
        return new IcalImportFeedResponse(
                feed.getId(), feed.getSource(), feed.getFeedUrl(),
                feed.getLastSyncedAt(), feed.getLastSyncStatus(), feed.getLastSyncError());
    }
}
