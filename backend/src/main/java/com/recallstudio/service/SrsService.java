package com.recallstudio.service;

import com.recallstudio.domain.SrsState;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class SrsService {
    public SrsState update(SrsState current, int score, OffsetDateTime now) {
        SrsState srs = current == null ? new SrsState() : current;
        if (srs.getEasiness() <= 0) {
            srs.setEasiness(2.5);
        }
        int reps = srs.getReps();
        if (score <= 0) {
            srs.setEasiness(Math.max(1.3, srs.getEasiness() - 0.2));
            srs.setInterval(1);
            reps = 0;
        } else if (score == 1) {
            srs.setEasiness(Math.max(1.3, srs.getEasiness() - 0.15));
            srs.setInterval(Math.max(1, (int) Math.round(srs.getInterval() * 1.2)));
            reps += 1;
        } else {
            srs.setEasiness(Math.min(2.8, srs.getEasiness() + 0.1));
            if (reps <= 0) {
                srs.setInterval(1);
            } else if (reps == 1) {
                srs.setInterval(3);
            } else {
                srs.setInterval(Math.max(1, (int) Math.round(srs.getInterval() * srs.getEasiness())));
            }
            reps += 1;
        }
        srs.setReps(reps);
        srs.setDue(now.plusDays(srs.getInterval()));
        srs.setLastScore(score);
        srs.setLastReviewedAt(now);
        return srs;
    }
}
