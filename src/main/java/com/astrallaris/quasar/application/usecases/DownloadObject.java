package com.astrallaris.quasar.application.usecases;

import org.springframework.stereotype.Service;

import com.astrallaris.quasar.application.exceptions.NotFoundException;
import com.astrallaris.quasar.domain.StorageNames;
import com.astrallaris.quasar.domain.entities.Bucket;
import com.astrallaris.quasar.domain.entities.StoredObject;
import com.astrallaris.quasar.domain.repositories.BucketRepository;
import com.astrallaris.quasar.domain.repositories.FilesystemRepository;
import com.astrallaris.quasar.domain.repositories.ObjectsRepository;

@Service
public class DownloadObject {
    private final BucketRepository bucketRepository;
    private final ObjectsRepository objectsRepository;
    private final FilesystemRepository filesystemRepository;

    public DownloadObject(BucketRepository bucketRepository, ObjectsRepository objectsRepository,
            FilesystemRepository filesystemRepository) {
        this.bucketRepository = bucketRepository;
        this.objectsRepository = objectsRepository;
        this.filesystemRepository = filesystemRepository;
    }

    public byte[] execute(String bucketName, String key) {
        String canonicalKey = StorageNames.canonicalKey(key);
        Bucket bucket = bucketRepository.findByName(bucketName);
        if (bucket == null) {
            throw NotFoundException.bucket(bucketName);
        }
        StoredObject object = objectsRepository.findByBucketAndKey(bucket.id(), canonicalKey);
        if (object == null) {
            throw NotFoundException.object(bucketName, key);
        }
        return filesystemRepository.read(bucketName, canonicalKey)
                .orElseThrow(() -> NotFoundException.object(bucketName, key));
    }
}
