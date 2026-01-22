package com.recallstudio.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Settings {
    private int dailyLimit;
    private double newRatio;
    private Map<String, String> scoreMap;
    private String srsAlgo;
    private List<Integer> wrongReviewSchedule;

    public static Settings defaultSettings() {
        Settings settings = new Settings();
        settings.dailyLimit = 50;
        settings.newRatio = 0.2;
        Map<String, String> scoreMap = new LinkedHashMap<>();
        scoreMap.put("0", "Again");
        scoreMap.put("1", "Hard");
        scoreMap.put("2", "Good");
        settings.scoreMap = scoreMap;
        settings.srsAlgo = "SM2_SIMPLE";
        settings.wrongReviewSchedule = List.of(1, 3, 7);
        return settings;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public double getNewRatio() {
        return newRatio;
    }

    public void setNewRatio(double newRatio) {
        this.newRatio = newRatio;
    }

    public Map<String, String> getScoreMap() {
        return scoreMap;
    }

    public void setScoreMap(Map<String, String> scoreMap) {
        this.scoreMap = scoreMap;
    }

    public String getSrsAlgo() {
        return srsAlgo;
    }

    public void setSrsAlgo(String srsAlgo) {
        this.srsAlgo = srsAlgo;
    }

    public List<Integer> getWrongReviewSchedule() {
        return wrongReviewSchedule;
    }

    public void setWrongReviewSchedule(List<Integer> wrongReviewSchedule) {
        this.wrongReviewSchedule = wrongReviewSchedule;
    }
}
