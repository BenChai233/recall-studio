package com.recallstudio.repo;

import com.recallstudio.domain.Settings;
import org.springframework.stereotype.Repository;

import java.nio.file.Files;
import java.nio.file.Path;

@Repository
public class SettingsRepository {
    private final DataDirService dataDirService;
    private final JsonFileStore store;

    public SettingsRepository(DataDirService dataDirService, JsonFileStore store) {
        this.dataDirService = dataDirService;
        this.store = store;
    }

    public Settings load() {
        Path path = settingsPath();
        if (!Files.exists(path)) {
            return Settings.defaultSettings();
        }
        return store.read(path, Settings.class);
    }

    public Settings save(Settings settings) {
        store.writeAtomic(settingsPath(), settings);
        return settings;
    }

    private Path settingsPath() {
        return dataDirService.configDir().resolve("settings.json");
    }
}
