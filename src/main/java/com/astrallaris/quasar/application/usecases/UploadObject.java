package com.astrallaris.quasar.application.usecases;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.astrallaris.quasar.application.exceptions.InvalidNameException;
import com.astrallaris.quasar.application.exceptions.NotFoundException;
import com.astrallaris.quasar.domain.StorageNames;
import com.astrallaris.quasar.domain.entities.Bucket;
import com.astrallaris.quasar.domain.entities.StoredObject;
import com.astrallaris.quasar.domain.repositories.BucketRepository;
import com.astrallaris.quasar.domain.repositories.FilesystemRepository;
import com.astrallaris.quasar.domain.repositories.ObjectsRepository;

@Service
public class UploadObject {
    private final BucketRepository bucketRepository;
    private final ObjectsRepository objectsRepository;
    private final FilesystemRepository filesystemRepository;

    public UploadObject(BucketRepository bucketRepository, ObjectsRepository objectsRepository,
            FilesystemRepository filesystemRepository) {
        this.bucketRepository = bucketRepository;
        this.objectsRepository = objectsRepository;
        this.filesystemRepository = filesystemRepository;
    }

    public StoredObject execute(String bucketName, String key, byte[] content) {
        String canonicalKey = StorageNames.canonicalKey(key);
        if (!StorageNames.isSafeName(canonicalKey)) {
            throw new InvalidNameException("invalid key: " + key);
        }
        Bucket bucket = bucketRepository.findByName(bucketName);
        if (bucket == null) {
            throw NotFoundException.bucket(bucketName);
        }
        StoredObject existing = objectsRepository.findByBucketAndKey(bucket.id(), canonicalKey);
        Instant now = Instant.now();
        StoredObject object = existing == null
                ? new StoredObject(UUID.randomUUID(), bucket.id(), canonicalKey, hash(content), content.length, 1, now, now)
                : new StoredObject(existing.id(), bucket.id(), canonicalKey, hash(content), content.length,
                        existing.version() + 1, existing.createAt(), now);
        filesystemRepository.write(bucketName, canonicalKey, content);
        objectsRepository.save(object);
        return object;
    }

    private String hash(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
