package com.recallstudio.service;

import com.recallstudio.domain.Deck;
import com.recallstudio.exception.NotFoundException;
import com.recallstudio.repo.DeckRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeckService {
    private final DeckRepository repository;

    public DeckService(DeckRepository repository) {
        this.repository = repository;
    }

    public List<Deck> list(String keyword, Boolean archived) {
        return repository.findAll().stream()
                .filter(deck -> archived == null || deck.isArchived() == archived)
                .filter(deck -> keyword == null || keyword.isBlank()
                        || (deck.getName() != null && deck.getName().contains(keyword))
                        || (deck.getDescription() != null && deck.getDescription().contains(keyword)))
                .collect(Collectors.toList());
    }

    public Deck get(String deckId) {
        return repository.findById(deckId).orElseThrow(() -> new NotFoundException("deck not found"));
    }

    public Deck create(Deck deck) {
        deck.setArchived(false);
        return repository.save(deck);
    }

    public Deck update(String deckId, String name, String description, List<String> tags, Boolean archived) {
        Deck existing = get(deckId);
        if (name != null) {
            existing.setName(name);
        }
        if (description != null) {
            existing.setDescription(description);
        }
        if (tags != null) {
            existing.setTags(tags);
        }
        if (archived != null) {
            existing.setArchived(archived);
        }
        return repository.save(existing);
    }

    public void delete(String deckId) {
        repository.delete(deckId);
    }
}
