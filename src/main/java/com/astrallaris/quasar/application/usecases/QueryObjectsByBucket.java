package com.astrallaris.quasar.application.usecases;

import java.util.List;

import org.springframework.stereotype.Service;

import com.astrallaris.quasar.application.exceptions.NotFoundException;
import com.astrallaris.quasar.domain.entities.Bucket;
import com.astrallaris.quasar.domain.entities.StoredObject;
import com.astrallaris.quasar.domain.repositories.BucketRepository;
import com.astrallaris.quasar.domain.repositories.ObjectsRepository;

@Service
public class QueryObjectsByBucket {
    private final BucketRepository bucketRepository;
    private final ObjectsRepository objectsRepository;

    public QueryObjectsByBucket(BucketRepository bucketRepository, ObjectsRepository objectsRepository) {
        this.bucketRepository = bucketRepository;
        this.objectsRepository = objectsRepository;
    }

    public List<StoredObject> execute(String bucketName) {
        Bucket bucket = bucketRepository.findByName(bucketName);
        if (bucket == null) {
            throw NotFoundException.bucket(bucketName);
        }
        return objectsRepository.findByBucket(bucket.id());
    }
}
