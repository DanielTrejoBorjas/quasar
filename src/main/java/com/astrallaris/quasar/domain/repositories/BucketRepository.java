package com.astrallaris.quasar.domain.repositories;

import java.util.List;
import java.util.UUID;

import com.astrallaris.quasar.domain.entities.Bucket;

public interface BucketRepository {
    public Bucket findByName(String name);
    public Bucket findById(UUID id);
    public List<Bucket> findAll();
    public void save(Bucket bucket);
    public void delete(Bucket bucket);
} 