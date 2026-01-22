package com.recallstudio.repo;

import com.recallstudio.domain.Deck;
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
public class DeckRepository {
    private final DataDirService dataDirService;
    private final JsonFileStore store;

    public DeckRepository(DataDirService dataDirService, JsonFileStore store) {
        this.dataDirService = dataDirService;
        this.store = store;
    }

    public List<Deck> findAll() {
        Path dir = dataDirService.decksDir();
        if (!Files.exists(dir)) {
            return List.of();
        }
        List<Deck> result = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> p.toString().endsWith(".json")).forEach(p -> result.add(store.read(p, Deck.class)));
        } catch (Exception ex) {
            throw new RuntimeException("failed to list decks", ex);
        }
        return result;
    }

    public Optional<Deck> findById(String deckId) {
        Path path = deckPath(deckId);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        return Optional.of(store.read(path, Deck.class));
    }

    public Deck save(Deck deck) {
        if (deck.getDeckId() == null || deck.getDeckId().isBlank()) {
            deck.setDeckId(UUID.randomUUID().toString());
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (deck.getCreatedAt() == null) {
            deck.setCreatedAt(now);
        }
        deck.setUpdatedAt(now);
        store.writeAtomic(deckPath(deck.getDeckId()), deck);
        return deck;
    }

    public void delete(String deckId) {
        Path path = deckPath(deckId);
        if (!Files.exists(path)) {
            throw new NotFoundException("deck not found");
        }
        try {
            Files.deleteIfExists(path);
        } catch (Exception ex) {
            throw new RuntimeException("failed to delete deck", ex);
        }
    }

    private Path deckPath(String deckId) {
        return dataDirService.decksDir().resolve(deckId + ".json");
    }
}
