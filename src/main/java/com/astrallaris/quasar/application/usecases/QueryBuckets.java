package com.astrallaris.quasar.application.usecases;

import java.util.List;

import org.springframework.stereotype.Service;

import com.astrallaris.quasar.domain.entities.Bucket;
import com.astrallaris.quasar.domain.repositories.BucketRepository;

@Service
public class QueryBuckets {
    private final BucketRepository bucketRepository;
    
    public QueryBuckets(BucketRepository bucketRepository){
        this.bucketRepository = bucketRepository;
    }

    public List<Bucket> execute() {
        return bucketRepository.findAll();
    }
}
