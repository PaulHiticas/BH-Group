package com.bhstays.pms.service.mapper;

import com.bhstays.pms.domain.IcalImportFeed;
import com.bhstays.pms.dto.ical.IcalImportFeedResponse;
import org.springframework.stereotype.Component;

@Component
public class IcalImportFeedMapper {

    public IcalImportFeedResponse toResponse(IcalImportFeed feed) {
        return new IcalImportFeedResponse(
                feed.getId(), feed.getSource(), feed.getFeedUrl(),
                feed.getLastSyncedAt(), feed.getLastSyncStatus(), feed.getLastSyncError());
    }
}
