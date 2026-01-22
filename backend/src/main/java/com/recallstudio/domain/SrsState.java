package com.recallstudio.domain;

import java.time.OffsetDateTime;

public class SrsState {
    private double easiness;
    private int interval;
    private OffsetDateTime due;
    private int reps;
    private Integer lastScore;
    private OffsetDateTime lastReviewedAt;
    private Integer wrongStep;
    private OffsetDateTime wrongDue;

    public double getEasiness() {
        return easiness;
    }

    public void setEasiness(double easiness) {
        this.easiness = easiness;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public OffsetDateTime getDue() {
        return due;
    }

    public void setDue(OffsetDateTime due) {
        this.due = due;
    }

    public int getReps() {
        return reps;
    }

    public void setReps(int reps) {
        this.reps = reps;
    }

    public Integer getLastScore() {
        return lastScore;
    }

    public void setLastScore(Integer lastScore) {
        this.lastScore = lastScore;
    }

    public OffsetDateTime getLastReviewedAt() {
        return lastReviewedAt;
    }

    public void setLastReviewedAt(OffsetDateTime lastReviewedAt) {
        this.lastReviewedAt = lastReviewedAt;
    }

    public Integer getWrongStep() {
        return wrongStep;
    }

    public void setWrongStep(Integer wrongStep) {
        this.wrongStep = wrongStep;
    }

    public OffsetDateTime getWrongDue() {
        return wrongDue;
    }

    public void setWrongDue(OffsetDateTime wrongDue) {
        this.wrongDue = wrongDue;
    }
}
