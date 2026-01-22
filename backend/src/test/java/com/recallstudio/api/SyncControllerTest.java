package com.recallstudio.api;

import com.recallstudio.domain.Item;
import com.recallstudio.repo.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SyncControllerTest {
    private static final Path DATA_DIR = initDataDir();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ItemRepository itemRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("app.dataDir", () -> DATA_DIR.toString());
    }

    @BeforeEach
    void cleanDataDir() {
        try (Stream<Path> stream = Files.list(DATA_DIR)) {
            stream.filter(path -> !path.getFileName().toString().equals(".git"))
                    .forEach(this::deleteRecursively);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    void snapshotFlowMarksDirtyAndClean() throws Exception {
        Item item = new Item();
        item.setDeckId("d1");
        item.setType("concept");
        item.setPrompt("示例题干");
        itemRepository.save(item);

        mockMvc.perform(get("/api/sync/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dirty").value(true));

        mockMvc.perform(post("/api/sync/snapshot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dirty").value(false))
                .andExpect(jsonPath("$.data.hasSnapshot").value(true));

        item.setPrompt("更新题干");
        itemRepository.save(item);

        mockMvc.perform(get("/api/sync/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dirty").value(true));
    }

    private static Path initDataDir() {
        try {
            Path dir = Files.createTempDirectory("recall-sync-");
            Files.createDirectories(dir.resolve(".git"));
            return dir;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void deleteRecursively(Path path) {
        try (Stream<Path> stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
