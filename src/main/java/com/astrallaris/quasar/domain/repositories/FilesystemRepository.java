package com.astrallaris.quasar.domain.repositories;

import java.util.Optional;

public interface FilesystemRepository {
    public void write(String bucket, String key, byte[] content);
    public Optional<byte[]> read(String bucket, String key);
    public void delete(String bucket, String key);
}
