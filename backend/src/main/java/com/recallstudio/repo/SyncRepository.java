package com.recallstudio.repo;

import com.recallstudio.domain.SyncSnapshot;
import org.springframework.stereotype.Repository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Repository
public class SyncRepository {
    private final DataDirService dataDirService;
    private final JsonFileStore store;

    public SyncRepository(DataDirService dataDirService, JsonFileStore store) {
        this.dataDirService = dataDirService;
        this.store = store;
    }

    public Optional<SyncSnapshot> load() {
        Path path = snapshotPath();
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        return Optional.of(store.read(path, SyncSnapshot.class));
    }

    public SyncSnapshot save(SyncSnapshot snapshot) {
        store.writeAtomic(snapshotPath(), snapshot);
        return snapshot;
    }

    private Path snapshotPath() {
        return dataDirService.configDir().resolve("last-sync.json");
    }
}
