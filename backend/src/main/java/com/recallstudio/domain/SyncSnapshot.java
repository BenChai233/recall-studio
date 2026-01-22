package com.recallstudio.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class SyncSnapshot {
    private OffsetDateTime lastSyncAt;
    private String fingerprint;
    private int fileCount;
    private OffsetDateTime lastModifiedAt;
}
