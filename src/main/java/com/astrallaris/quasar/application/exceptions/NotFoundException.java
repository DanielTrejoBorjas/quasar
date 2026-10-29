package com.astrallaris.quasar.application.exceptions;

public class NotFoundException extends RuntimeException {

    private NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException bucket(String name) {
        return new NotFoundException("bucket not found: " + name);
    }

    public static NotFoundException object(String bucket, String key) {
        return new NotFoundException("object not found: " + bucket + "/" + key);
    }
}
