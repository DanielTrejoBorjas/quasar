package com.astrallaris.quasar.application.exceptions;

public class BucketAlreadyExistsException extends RuntimeException {

    public BucketAlreadyExistsException(String bucketName) {
        super("bucket already exists: " + bucketName);
    }
}
