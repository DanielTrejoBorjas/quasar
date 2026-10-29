package com.astrallaris.quasar.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.astrallaris.quasar.domain.entities.Bucket;

class SQLiteBucketRepositoryTest {

    @TempDir
    Path tempDir;

    private SQLiteBucketRepository repository;

    @BeforeEach
    void setUp() {
        repository = new SQLiteBucketRepository("jdbc:sqlite:" + tempDir.resolve("quasar-test.db"));
    }

    @Test
    void savesAndFindsBucketById() {
        Bucket bucket = new Bucket(UUID.randomUUID(), "photos");

        repository.save(bucket);

        assertEquals(bucket, repository.findById(bucket.id()));
    }

    @Test
    void findsBucketByName() {
        Bucket bucket = new Bucket(UUID.randomUUID(), "photos");

        repository.save(bucket);

        assertEquals(bucket, repository.findByName("photos"));
    }

    @Test
    void returnsNullForUnknownBucket() {
        assertNull(repository.findById(UUID.randomUUID()));
        assertNull(repository.findByName("missing"));
    }

    @Test
    void saveUpdatesExistingBucket() {
        UUID id = UUID.randomUUID();
        repository.save(new Bucket(id, "photos"));

        repository.save(new Bucket(id, "images"));

        assertEquals("images", repository.findById(id).name());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void findAllReturnsBucketsOrderedByName() {
        repository.save(new Bucket(UUID.randomUUID(), "videos"));
        repository.save(new Bucket(UUID.randomUUID(), "images"));
        repository.save(new Bucket(UUID.randomUUID(), "audio"));

        List<String> names = repository.findAll().stream().map(Bucket::name).toList();

        assertEquals(List.of("audio", "images", "videos"), names);
    }

    @Test
    void deletesBucket() {
        Bucket bucket = new Bucket(UUID.randomUUID(), "photos");
        repository.save(bucket);

        repository.delete(bucket);

        assertNull(repository.findById(bucket.id()));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void rejectsDuplicateBucketNames() {
        repository.save(new Bucket(UUID.randomUUID(), "photos"));

        assertThrows(IllegalStateException.class, () -> repository.save(new Bucket(UUID.randomUUID(), "photos")));
    }
}
