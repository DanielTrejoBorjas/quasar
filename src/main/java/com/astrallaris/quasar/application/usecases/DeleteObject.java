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
public class DeleteObject {
    private final BucketRepository bucketRepository;
    private final ObjectsRepository objectsRepository;
    private final FilesystemRepository filesystemRepository;

    public DeleteObject(BucketRepository bucketRepository, ObjectsRepository objectsRepository,
            FilesystemRepository filesystemRepository) {
        this.bucketRepository = bucketRepository;
        this.objectsRepository = objectsRepository;
        this.filesystemRepository = filesystemRepository;
    }

    public void execute(String bucketName, String key) {
        String canonicalKey = StorageNames.canonicalKey(key);
        Bucket bucket = bucketRepository.findByName(bucketName);
        if (bucket == null) {
            throw NotFoundException.bucket(bucketName);
        }
        StoredObject object = objectsRepository.findByBucketAndKey(bucket.id(), canonicalKey);
        if (object == null) {
            throw NotFoundException.object(bucketName, key);
        }
        objectsRepository.deleteById(object.id());
        filesystemRepository.delete(bucketName, canonicalKey);
    }
}
