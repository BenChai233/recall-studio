package com.recallstudio.api;

import com.recallstudio.domain.Item;
import com.recallstudio.service.ItemService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemController {
    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping
    public ApiResponse<List<Item>> list(@RequestParam(name = "deckId", required = false) String deckId,
                                        @RequestParam(name = "type", required = false) String type,
                                        @RequestParam(name = "tag", required = false) String tag,
                                        @RequestParam(name = "archived", required = false) Boolean archived,
                                        @RequestParam(name = "dueBefore", required = false) String dueBefore) {
        OffsetDateTime due = null;
        if (dueBefore != null && !dueBefore.isBlank()) {
            due = OffsetDateTime.parse(dueBefore);
        }
        return ApiResponse.ok(itemService.list(deckId, type, tag, archived, due));
    }

    @GetMapping("/{itemId}")
    public ApiResponse<Item> get(@PathVariable("itemId") String itemId) {
        return ApiResponse.ok(itemService.get(itemId));
    }

    @PostMapping
    public ApiResponse<Item> create(@Valid @RequestBody ItemCreateRequest request) {
        Item item = new Item();
        item.setDeckId(request.deckId());
        item.setType(request.type());
        item.setPrompt(request.prompt());
        item.setHint(request.hint());
        item.setAnswerMarkdown(request.answerMarkdown());
        item.setTags(request.tags());
        item.setDifficulty(request.difficulty());
        return ApiResponse.ok(itemService.create(item));
    }

    @PutMapping("/{itemId}")
    public ApiResponse<Item> update(@PathVariable("itemId") String itemId, @RequestBody ItemUpdateRequest request) {
        Item updates = new Item();
        updates.setDeckId(request.deckId());
        updates.setType(request.type());
        updates.setPrompt(request.prompt());
        updates.setHint(request.hint());
        updates.setAnswerMarkdown(request.answerMarkdown());
        updates.setTags(request.tags());
        updates.setDifficulty(request.difficulty());
        return ApiResponse.ok(itemService.update(itemId, updates, request.archived()));
    }

    @PatchMapping("/{itemId}/archive")
    public ApiResponse<Item> archive(@PathVariable("itemId") String itemId, @RequestBody ArchiveRequest request) {
        boolean archived = request.archived() != null && request.archived();
        return ApiResponse.ok(itemService.archive(itemId, archived));
    }

    @DeleteMapping("/{itemId}")
    public ApiResponse<Void> delete(@PathVariable("itemId") String itemId) {
        itemService.delete(itemId);
        return ApiResponse.ok(null);
    }

    public record ItemCreateRequest(@NotBlank String deckId, @NotBlank String type, @NotBlank String prompt,
                                    String hint, String answerMarkdown,
                                    List<String> tags, String difficulty) {}

    public record ItemUpdateRequest(String deckId, String type, String prompt, String hint,
                                    String answerMarkdown,
                                    List<String> tags, String difficulty, Boolean archived) {}

    public record ArchiveRequest(Boolean archived) {}
}
