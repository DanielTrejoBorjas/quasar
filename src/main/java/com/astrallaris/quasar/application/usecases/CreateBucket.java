package com.astrallaris.quasar.application.usecases;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.astrallaris.quasar.application.exceptions.BucketAlreadyExistsException;
import com.astrallaris.quasar.application.exceptions.InvalidNameException;
import com.astrallaris.quasar.domain.StorageNames;
import com.astrallaris.quasar.domain.entities.Bucket;
import com.astrallaris.quasar.domain.repositories.BucketRepository;

@Service
public class CreateBucket {
    private final BucketRepository bucketRepository;

    public CreateBucket(BucketRepository bucketRepository) {
        this.bucketRepository = bucketRepository;
    }

    public Bucket execute(String name) {
        if (!StorageNames.isSafeName(name)) {
            throw new InvalidNameException("invalid bucket name: " + name);
        }
        if (bucketRepository.findByName(name) != null) {
            throw new BucketAlreadyExistsException(name);
        }
        Bucket bucket = new Bucket(UUID.randomUUID(), name);
        bucketRepository.save(bucket);
        return bucket;
    }
}
