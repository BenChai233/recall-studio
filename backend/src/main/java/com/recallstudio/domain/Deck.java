package com.recallstudio.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
public class Deck {
    private String deckId;
    private String name;
    private String description;
    private List<String> tags;
    private boolean archived;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
