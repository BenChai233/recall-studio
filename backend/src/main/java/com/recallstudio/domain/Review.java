package com.recallstudio.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
public class Review {
    private String reviewId;
    private String itemId;
    private String deckId;
    private String sessionId;
    private OffsetDateTime reviewedAt;
    private int score;
    private String answer;
    private List<String> reasonTags;
    private SrsState prevSrs;
    private boolean prevSrsPresent;
}
