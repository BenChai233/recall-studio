package com.recallstudio.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
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
}
