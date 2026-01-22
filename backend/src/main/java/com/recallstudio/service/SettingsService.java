package com.recallstudio.service;

import com.recallstudio.domain.Settings;
import com.recallstudio.repo.SettingsRepository;
import org.springframework.stereotype.Service;

@Service
public class SettingsService {
    private final SettingsRepository repository;

    public SettingsService(SettingsRepository repository) {
        this.repository = repository;
    }

    public Settings get() {
        return repository.load();
    }

    public Settings update(Settings settings) {
        return repository.save(settings);
    }
}
