package com.astrallaris.quasar.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFilesystemRepositoryTest {

    @TempDir
    Path storageRoot;

    private LocalFilesystemRepository repository;

    @BeforeEach
    void setUp() {
        repository = new LocalFilesystemRepository(storageRoot.toString());
    }

    @Test
    void writeCreatesBucketDirectoryAndStoresContent() {
        repository.write("images", "hero-image-v1.png", bytes("meow"));

        assertTrue(Files.isRegularFile(storageRoot.resolve("images").resolve("hero-image-v1.png")));
    }

    @Test
    void writeFlattensSlashesInKey() {
        repository.write("images", "photos/2024/cat.jpg", bytes("meow"));

        assertTrue(Files.isRegularFile(storageRoot.resolve("images").resolve("photos-2024-cat.jpg")));
    }

    @Test
    void writeOverwritesExistingObject() {
        repository.write("images", "a.txt", bytes("first"));

        repository.write("images", "a.txt", bytes("second"));

        assertArrayEquals(bytes("second"), repository.read("images", "a.txt").orElseThrow());
    }

    @Test
    void readReturnsWrittenContent() {
        repository.write("images", "a.txt", bytes("first"));

        assertArrayEquals(bytes("first"), repository.read("images", "a.txt").orElseThrow());
    }

    @Test
    void readReturnsEmptyForUnknownObject() {
        assertTrue(repository.read("images", "missing.txt").isEmpty());
    }

    @Test
    void deleteRemovesObject() {
        repository.write("images", "a.txt", bytes("first"));

        repository.delete("images", "a.txt");

        assertTrue(repository.read("images", "a.txt").isEmpty());
    }

    @Test
    void deleteOfUnknownObjectIsIgnored() {
        repository.delete("images", "missing.txt");

        assertTrue(repository.read("images", "missing.txt").isEmpty());
    }

    @Test
    void writeRejectsUnsafeNames() {
        assertThrows(IllegalArgumentException.class, () -> repository.write("..", "a.txt", bytes("content")));
        assertThrows(IllegalArgumentException.class, () -> repository.write("images", "..", bytes("content")));
    }

    @Test
    void readNeverEscapesStorageRoot() throws IOException {
        Files.writeString(storageRoot.getParent().resolve("secret.txt"), "secret");

        assertTrue(repository.read("..", "secret.txt").isEmpty());
        assertTrue(repository.read("images", "..").isEmpty());
        assertTrue(repository.read("images", "../../secret.txt").isEmpty());
    }

    @Test
    void deleteNeverEscapesStorageRoot() throws IOException {
        Path secret = storageRoot.getParent().resolve("secret.txt");
        Files.writeString(secret, "secret");

        assertThrows(IllegalArgumentException.class, () -> repository.delete("..", "secret.txt"));

        assertTrue(Files.exists(secret));
    }

    private byte[] bytes(String content) {
        return content.getBytes(StandardCharsets.UTF_8);
    }
}
