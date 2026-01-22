package com.recallstudio.repo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
public class JsonFileStore {
    private final ObjectMapper mapper;

    public JsonFileStore(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public <T> T read(Path path, Class<T> type) {
        try {
            return mapper.readValue(path.toFile(), type);
        } catch (IOException ex) {
            throw new RuntimeException("failed to read json: " + path, ex);
        }
    }

    public <T> T fromJson(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (IOException ex) {
            throw new RuntimeException("failed to parse json", ex);
        }
    }

    public <T> void writeAtomic(Path path, T value) {
        try {
            Files.createDirectories(path.getParent());
            Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
            mapper.writeValue(tmp.toFile(), value);
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ex) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new RuntimeException("failed to write json: " + path, ex);
        }
    }

    public String toJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (IOException ex) {
            throw new RuntimeException("failed to serialize json", ex);
        }
    }
}
