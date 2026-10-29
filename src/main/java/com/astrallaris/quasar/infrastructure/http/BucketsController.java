package com.astrallaris.quasar.infrastructure.http;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.astrallaris.quasar.application.usecases.CreateBucket;
import com.astrallaris.quasar.application.usecases.QueryBuckets;
import com.astrallaris.quasar.domain.entities.Bucket;

@RestController
public class BucketsController {

    private final QueryBuckets queryBuckets;
    private final CreateBucket createBucket;

    public BucketsController(QueryBuckets queryBuckets, CreateBucket createBucket) {
        this.queryBuckets = queryBuckets;
        this.createBucket = createBucket;
    }

    @GetMapping("/buckets")
    public List<Bucket> list() {
        return queryBuckets.execute();
    }

    @PostMapping("/buckets")
    public ResponseEntity<Bucket> create(@RequestParam String name) {
        return ResponseEntity.status(HttpStatus.CREATED).body(createBucket.execute(name));
    }
}
