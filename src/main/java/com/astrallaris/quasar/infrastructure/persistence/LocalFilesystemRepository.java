package com.astrallaris.quasar.infrastructure.persistence;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.astrallaris.quasar.domain.StorageNames;
import com.astrallaris.quasar.domain.repositories.FilesystemRepository;

@Repository
public class LocalFilesystemRepository implements FilesystemRepository {

    private final Path root;

    public LocalFilesystemRepository(@Value("${quasar.storage.root:data}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    @Override
    public void write(String bucket, String key, byte[] content) {
        Path object = objectPath(bucket, key);
        try {
            Files.createDirectories(object.getParent());
            Files.write(object, content);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write key " + key + " in bucket " + bucket, e);
        }
    }

    @Override
    public Optional<byte[]> read(String bucket, String key) {
        Optional<Path> object = objectPathIfSafe(bucket, key);
        if (object.isEmpty() || !Files.isRegularFile(object.get())) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(object.get()));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read key " + key + " in bucket " + bucket, e);
        }
    }

    @Override
    public void delete(String bucket, String key) {
        Path object = objectPath(bucket, key);
        try {
            Files.deleteIfExists(object);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete key " + key + " in bucket " + bucket, e);
        }
    }

    private Path objectPath(String bucket, String key) {
        return objectPathIfSafe(bucket, key)
                .orElseThrow(() -> new IllegalArgumentException("unsafe bucket or key: " + bucket + "/" + key));
    }

    private Optional<Path> objectPathIfSafe(String bucket, String key) {
        String fileName = StorageNames.canonicalKey(key);
        if (!StorageNames.isSafeName(bucket) || !StorageNames.isSafeName(fileName)) {
            return Optional.empty();
        }
        return Optional.of(root.resolve(bucket).resolve(fileName));
    }
}
