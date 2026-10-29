package com.astrallaris.quasar.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.astrallaris.quasar.domain.entities.StoredObject;

class SQLiteObjectsRepositoryTest {

    private static final Instant CREATE_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATE_AT = Instant.parse("2026-02-02T12:30:00Z");

    @TempDir
    Path tempDir;

    private final UUID bucketId = UUID.randomUUID();

    private SQLiteObjectsRepository repository;

    @BeforeEach
    void setUp() {
        repository = new SQLiteObjectsRepository("jdbc:sqlite:" + tempDir.resolve("quasar-test.db"));
    }

    @Test
    void savesAndFindsObjectById() {
        StoredObject object = object(bucketId, "a.txt", "hash", 12, 1, CREATE_AT, CREATE_AT);

        repository.save(object);

        assertEquals(object, repository.findById(object.id()));
    }

    @Test
    void findsObjectByBucketAndKey() {
        StoredObject object = object(bucketId, "images-hero.png", "hash", 12, 1, CREATE_AT, CREATE_AT);
        repository.save(object);

        assertEquals(object, repository.findByBucketAndKey(bucketId, "images-hero.png"));
        assertNull(repository.findByBucketAndKey(bucketId, "other.png"));
    }

    @Test
    void findByBucketReturnsObjectsOfBucketOrderedByKey() {
        repository.save(object(bucketId, "b.txt", "hash", 1, 1, CREATE_AT, CREATE_AT));
        repository.save(object(bucketId, "a.txt", "hash", 1, 1, CREATE_AT, CREATE_AT));
        repository.save(object(UUID.randomUUID(), "c.txt", "hash", 1, 1, CREATE_AT, CREATE_AT));

        List<String> keys = repository.findByBucket(bucketId).stream().map(StoredObject::key).toList();

        assertEquals(List.of("a.txt", "b.txt"), keys);
    }

    @Test
    void saveUpdatesExistingObjectKeepingCreateAt() {
        UUID id = UUID.randomUUID();
        repository.save(new StoredObject(id, bucketId, "a.txt", "old-hash", 3, 1, CREATE_AT, CREATE_AT));

        repository.save(new StoredObject(id, bucketId, "a.txt", "new-hash", 6, 2, CREATE_AT, UPDATE_AT));

        StoredObject stored = repository.findById(id);
        assertEquals("new-hash", stored.hash());
        assertEquals(6, stored.size());
        assertEquals(2, stored.version());
        assertEquals(CREATE_AT, stored.createAt());
        assertEquals(UPDATE_AT, stored.updateAt());
        assertEquals(1, repository.findByBucket(bucketId).size());
    }

    @Test
    void returnsNullForUnknownObject() {
        assertNull(repository.findById(UUID.randomUUID()));
    }

    @Test
    void deleteRemovesObject() {
        StoredObject object = object(bucketId, "a.txt", "hash", 12, 1, CREATE_AT, CREATE_AT);
        repository.save(object);

        repository.deleteById(object.id());

        assertNull(repository.findById(object.id()));
        assertTrue(repository.findByBucket(bucketId).isEmpty());
    }

    private StoredObject object(UUID bucketId, String key, String hash, long size, int version, Instant createAt,
            Instant updateAt) {
        return new StoredObject(UUID.randomUUID(), bucketId, key, hash, size, version, createAt, updateAt);
    }
}
