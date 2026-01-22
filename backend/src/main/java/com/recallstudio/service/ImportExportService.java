package com.recallstudio.service;

import com.recallstudio.domain.Deck;
import com.recallstudio.domain.Item;
import com.recallstudio.domain.Review;
import com.recallstudio.domain.Settings;
import com.recallstudio.repo.DeckRepository;
import com.recallstudio.repo.ItemRepository;
import com.recallstudio.repo.ReviewRepository;
import com.recallstudio.repo.SettingsRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ImportExportService {
    private final DeckRepository deckRepository;
    private final ItemRepository itemRepository;
    private final ReviewRepository reviewRepository;
    private final SettingsRepository settingsRepository;

    public ImportExportService(DeckRepository deckRepository,
                               ItemRepository itemRepository,
                               ReviewRepository reviewRepository,
                               SettingsRepository settingsRepository) {
        this.deckRepository = deckRepository;
        this.itemRepository = itemRepository;
        this.reviewRepository = reviewRepository;
        this.settingsRepository = settingsRepository;
    }

    public ImportResult importData(ImportRequest request) {
        int importedDecks = 0;
        int importedItems = 0;
        if (request.decks() != null) {
            for (Deck deck : request.decks()) {
                deckRepository.save(deck);
                importedDecks++;
            }
        }
        if (request.items() != null) {
            for (Item item : request.items()) {
                itemRepository.save(item);
                importedItems++;
            }
        }
        return new ImportResult(importedDecks, importedItems, 0);
    }

    public ExportResponse exportData(boolean includeReviews) {
        List<Deck> decks = deckRepository.findAll();
        List<Item> items = itemRepository.findAll();
        List<Review> reviews = includeReviews ? reviewRepository.findAll() : List.of();
        Settings settings = settingsRepository.load();
        return new ExportResponse(decks, items, reviews, settings);
    }

    public record ImportRequest(List<Deck> decks, List<Item> items) {}

    public record ImportResult(int importedDecks, int importedItems, int skippedItems) {}

    public record ExportResponse(List<Deck> decks, List<Item> items, List<Review> reviews, Settings settings) {}
}
