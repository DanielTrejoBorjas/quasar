package com.astrallaris.quasar.domain.entities;

import java.time.Instant;
import java.util.UUID;

public record StoredObject(UUID id, UUID bucket_id, String key, String hash, long size, int version, Instant createAt, Instant updateAt) {
    
}
