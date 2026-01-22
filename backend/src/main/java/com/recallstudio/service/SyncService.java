package com.recallstudio.service;

import com.recallstudio.domain.SyncSnapshot;
import com.recallstudio.repo.DataDirService;
import com.recallstudio.repo.SyncRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class SyncService {
    private final DataDirService dataDirService;
    private final SyncRepository syncRepository;

    public SyncService(DataDirService dataDirService, SyncRepository syncRepository) {
        this.dataDirService = dataDirService;
        this.syncRepository = syncRepository;
    }

    public SyncStatus getStatus() {
        DataFingerprint current = computeFingerprint();
        Optional<SyncSnapshot> snapshot = syncRepository.load();
        boolean hasSnapshot = snapshot.isPresent();
        OffsetDateTime lastSyncAt = snapshot.map(SyncSnapshot::getLastSyncAt).orElse(null);
        boolean dirty = snapshot
                .map(s -> !s.getFingerprint().equals(current.fingerprint()))
                .orElse(current.fileCount() > 0);
        return new SyncStatus(hasSnapshot, lastSyncAt, dirty, current.fileCount(), current.lastModifiedAt());
    }

    public SyncStatus markSnapshot() {
        DataFingerprint current = computeFingerprint();
        SyncSnapshot snapshot = new SyncSnapshot();
        snapshot.setLastSyncAt(OffsetDateTime.now());
        snapshot.setFingerprint(current.fingerprint());
        snapshot.setFileCount(current.fileCount());
        snapshot.setLastModifiedAt(current.lastModifiedAt());
        syncRepository.save(snapshot);
        return new SyncStatus(true, snapshot.getLastSyncAt(), false, current.fileCount(), current.lastModifiedAt());
    }

    private DataFingerprint computeFingerprint() {
        Path dataDir = dataDirService.getDataDir();
        List<String> entries = new ArrayList<>();
        OffsetDateTime latest = null;
        try (var stream = Files.walk(dataDir)) {
            for (Path path : (Iterable<Path>) stream::iterator) {
                if (!Files.isRegularFile(path) || isExcluded(dataDir, path)) {
                    continue;
                }
                BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                long size = attrs.size();
                long modified = attrs.lastModifiedTime().toMillis();
                String relative = dataDir.relativize(path).toString().replace('\\', '/');
                entries.add(relative + "|" + size + "|" + modified);
                OffsetDateTime modifiedAt = OffsetDateTime.ofInstant(attrs.lastModifiedTime().toInstant(),
                        ZoneId.systemDefault());
                if (latest == null || modifiedAt.isAfter(latest)) {
                    latest = modifiedAt;
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("failed to compute data fingerprint", ex);
        }

        entries.sort(Comparator.naturalOrder());
        String fingerprint;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String entry : entries) {
                digest.update(entry.getBytes(StandardCharsets.UTF_8));
                digest.update((byte) '\n');
            }
            fingerprint = toHex(digest.digest());
        } catch (Exception ex) {
            throw new RuntimeException("failed to compute data fingerprint", ex);
        }

        return new DataFingerprint(fingerprint, entries.size(), latest);
    }

    private boolean isExcluded(Path dataDir, Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        Path gitDir = dataDir.resolve(".git").toAbsolutePath().normalize();
        if (normalized.startsWith(gitDir)) {
            return true;
        }
        Path snapshot = dataDir.resolve("config").resolve("last-sync.json").toAbsolutePath().normalize();
        return normalized.equals(snapshot);
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    private record DataFingerprint(String fingerprint, int fileCount, OffsetDateTime lastModifiedAt) {}

    public record SyncStatus(boolean hasSnapshot, OffsetDateTime lastSyncAt, boolean dirty,
                             int fileCount, OffsetDateTime lastModifiedAt) {}
}
