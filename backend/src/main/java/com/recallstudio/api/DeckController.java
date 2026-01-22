package com.recallstudio.api;

import com.recallstudio.domain.Deck;
import com.recallstudio.service.DeckService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/decks")
public class DeckController {
    private final DeckService deckService;

    public DeckController(DeckService deckService) {
        this.deckService = deckService;
    }

    @GetMapping
    public List<Deck> list(@RequestParam(name = "keyword", required = false) String keyword,
                           @RequestParam(name = "archived", required = false) Boolean archived) {
        return deckService.list(keyword, archived);
    }

    @GetMapping("/{deckId}")
    public Deck get(@PathVariable("deckId") String deckId) {
        return deckService.get(deckId);
    }

    @PostMapping
    public Deck create(@Valid @RequestBody DeckCreateRequest request) {
        Deck deck = new Deck();
        deck.setName(request.name());
        deck.setDescription(request.description());
        deck.setTags(request.tags());
        return deckService.create(deck);
    }

    @PutMapping("/{deckId}")
    public Deck update(@PathVariable("deckId") String deckId, @RequestBody DeckUpdateRequest request) {
        return deckService.update(deckId, request.name(), request.description(), request.tags(), request.archived());
    }

    @DeleteMapping("/{deckId}")
    public void delete(@PathVariable("deckId") String deckId) {
        deckService.delete(deckId);
    }

    public record DeckCreateRequest(@NotBlank String name, String description, List<String> tags) {}

    public record DeckUpdateRequest(String name, String description, List<String> tags, Boolean archived) {}
}
