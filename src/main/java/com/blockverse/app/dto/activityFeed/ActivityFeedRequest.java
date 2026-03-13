package com.blockverse.app.dto.activityFeed;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActivityFeedRequest {
    @NotNull(message = "Page number is required")
    int page;
    @NotNull(message = "Page size is required")
    int size;
}
