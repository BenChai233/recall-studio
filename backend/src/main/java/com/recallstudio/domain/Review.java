package com.recallstudio.domain;

import java.time.OffsetDateTime;
import java.util.List;

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

    public String getReviewId() {
        return reviewId;
    }

    public void setReviewId(String reviewId) {
        this.reviewId = reviewId;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getDeckId() {
        return deckId;
    }

    public void setDeckId(String deckId) {
        this.deckId = deckId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public OffsetDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(OffsetDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<String> getReasonTags() {
        return reasonTags;
    }

    public void setReasonTags(List<String> reasonTags) {
        this.reasonTags = reasonTags;
    }

    public SrsState getPrevSrs() {
        return prevSrs;
    }

    public void setPrevSrs(SrsState prevSrs) {
        this.prevSrs = prevSrs;
    }

    public boolean isPrevSrsPresent() {
        return prevSrsPresent;
    }

    public void setPrevSrsPresent(boolean prevSrsPresent) {
        this.prevSrsPresent = prevSrsPresent;
    }
}
