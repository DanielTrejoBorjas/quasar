package com.astrallaris.quasar.domain.repositories;

import java.util.List;
import java.util.UUID;

import com.astrallaris.quasar.domain.entities.StoredObject;

public interface ObjectsRepository {
    public void save(StoredObject object);
    public StoredObject findById(UUID id);
    public List<StoredObject> findByBucket(UUID bucketId);
    public StoredObject findByBucketAndKey(UUID bucketId, String key);
    public void deleteById(UUID id);
}
