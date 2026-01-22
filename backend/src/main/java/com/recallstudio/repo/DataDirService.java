package com.recallstudio.repo;

import com.recallstudio.config.AppProperties;
import com.recallstudio.exception.DataDirNotSetException;
import com.recallstudio.exception.GitRepoInvalidException;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class DataDirService {
    private final AppProperties properties;

    public DataDirService(AppProperties properties) {
        this.properties = properties;
    }

    public Path getDataDir() {
        String configured = properties.getDataDir();
        if (configured == null || configured.isBlank()) {
            throw new DataDirNotSetException();
        }
        Path path = resolvePath(configured);
        Path gitDir = path.resolve(".git");
        if (!Files.isDirectory(gitDir)) {
            throw new GitRepoInvalidException("dataDir is not a git repository: " + path);
        }
        return path;
    }

    private Path resolvePath(String configured) {
        Path raw = Paths.get(configured);
        if (raw.isAbsolute()) {
            Path abs = raw.normalize();
            if (!Files.isDirectory(abs)) {
                throw new GitRepoInvalidException("dataDir is not a directory: " + abs);
            }
            return abs;
        }

        Path candidate = raw.toAbsolutePath().normalize();
        if (Files.isDirectory(candidate)) {
            return candidate;
        }

        Path parentCandidate = Paths.get("..").resolve(raw).toAbsolutePath().normalize();
        if (Files.isDirectory(parentCandidate)) {
            return parentCandidate;
        }

        throw new GitRepoInvalidException("dataDir is not a directory: " + candidate);
    }

    public Path ensureDir(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (Exception ex) {
            throw new GitRepoInvalidException("cannot create directory: " + dir);
        }
        return dir;
    }

    public Path decksDir() {
        return ensureDir(getDataDir().resolve("decks"));
    }

    public Path itemsDir() {
        return ensureDir(getDataDir().resolve("items"));
    }

    public Path reviewsDir() {
        return ensureDir(getDataDir().resolve("reviews"));
    }

    public Path configDir() {
        return ensureDir(getDataDir().resolve("config"));
    }
}
