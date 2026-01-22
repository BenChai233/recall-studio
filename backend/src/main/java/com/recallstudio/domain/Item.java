package com.recallstudio.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
public class Item {
    private String itemId;
    private String deckId;
    private String type;
    private String prompt;
    private String hint;
    private String answerMarkdown;
    private List<String> tags;
    private String difficulty;
    private boolean archived;
    private SrsState srs;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
