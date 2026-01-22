package com.recallstudio.repo;

import com.recallstudio.domain.Item;
import com.recallstudio.exception.NotFoundException;
import org.springframework.stereotype.Repository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Repository
public class ItemRepository {
    private final DataDirService dataDirService;
    private final JsonFileStore store;

    public ItemRepository(DataDirService dataDirService, JsonFileStore store) {
        this.dataDirService = dataDirService;
        this.store = store;
    }

    public List<Item> findAll() {
        Path dir = dataDirService.itemsDir();
        if (!Files.exists(dir)) {
            return List.of();
        }
        List<Item> result = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> p.toString().endsWith(".json")).forEach(p -> result.add(store.read(p, Item.class)));
        } catch (Exception ex) {
            throw new RuntimeException("failed to list items", ex);
        }
        return result;
    }

    public Optional<Item> findById(String itemId) {
        Path path = itemPath(itemId);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        return Optional.of(store.read(path, Item.class));
    }

    public Item save(Item item) {
        if (item.getItemId() == null || item.getItemId().isBlank()) {
            item.setItemId(UUID.randomUUID().toString());
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (item.getCreatedAt() == null) {
            item.setCreatedAt(now);
        }
        item.setUpdatedAt(now);
        store.writeAtomic(itemPath(item.getItemId()), item);
        return item;
    }

    public void delete(String itemId) {
        Path path = itemPath(itemId);
        if (!Files.exists(path)) {
            throw new NotFoundException("item not found");
        }
        try {
            Files.deleteIfExists(path);
        } catch (Exception ex) {
            throw new RuntimeException("failed to delete item", ex);
        }
    }

    private Path itemPath(String itemId) {
        return dataDirService.itemsDir().resolve(itemId + ".json");
    }
}
