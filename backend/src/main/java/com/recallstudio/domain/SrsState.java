package com.recallstudio.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class SrsState {
    private double easiness;
    private int interval;
    private OffsetDateTime due;
    private int reps;
    private Integer lastScore;
    private OffsetDateTime lastReviewedAt;
    private Integer wrongStep;
    private OffsetDateTime wrongDue;
}
