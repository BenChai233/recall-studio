package com.recallstudio.service;

import com.recallstudio.domain.Item;
import com.recallstudio.exception.NotFoundException;
import com.recallstudio.repo.ItemRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ItemService {
    private final ItemRepository repository;

    public ItemService(ItemRepository repository) {
        this.repository = repository;
    }

    public List<Item> list(String deckId, String type, String tag, Boolean archived, OffsetDateTime dueBefore) {
        return repository.findAll().stream()
                .filter(item -> deckId == null || deckId.isBlank() || deckId.equals(item.getDeckId()))
                .filter(item -> type == null || type.isBlank() || type.equals(item.getType()))
                .filter(item -> tag == null || tag.isBlank()
                        || (item.getTags() != null && item.getTags().contains(tag)))
                .filter(item -> archived == null || item.isArchived() == archived)
                .filter(item -> dueBefore == null || item.getSrs() == null
                        || item.getSrs().getDue() == null
                        || !item.getSrs().getDue().isAfter(dueBefore))
                .collect(Collectors.toList());
    }

    public Item get(String itemId) {
        return repository.findById(itemId).orElseThrow(() -> new NotFoundException("item not found"));
    }

    public Item create(Item item) {
        item.setArchived(false);
        return repository.save(item);
    }

    public Item update(String itemId, Item updates, Boolean archived) {
        Item existing = get(itemId);
        if (updates.getDeckId() != null) {
            existing.setDeckId(updates.getDeckId());
        }
        if (updates.getType() != null) {
            existing.setType(updates.getType());
        }
        if (updates.getPrompt() != null) {
            existing.setPrompt(updates.getPrompt());
        }
        if (updates.getHint() != null) {
            existing.setHint(updates.getHint());
        }
        if (updates.getAnswerMarkdown() != null) {
            existing.setAnswerMarkdown(updates.getAnswerMarkdown());
        }
        if (updates.getTags() != null) {
            existing.setTags(updates.getTags());
        }
        if (updates.getDifficulty() != null) {
            existing.setDifficulty(updates.getDifficulty());
        }
        if (archived != null) {
            existing.setArchived(archived);
        }
        return repository.save(existing);
    }

    public Item archive(String itemId, boolean archived) {
        Item existing = get(itemId);
        existing.setArchived(archived);
        return repository.save(existing);
    }

    public void delete(String itemId) {
        repository.delete(itemId);
    }
}
