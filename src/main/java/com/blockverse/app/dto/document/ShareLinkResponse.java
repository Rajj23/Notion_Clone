package com.blockverse.app.dto.document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class ShareLinkResponse {
    private String url;
    private LocalDateTime expiryTime;
}
