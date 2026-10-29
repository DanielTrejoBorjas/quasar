package com.astrallaris.quasar.infrastructure.http;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.astrallaris.quasar.application.usecases.DeleteObject;
import com.astrallaris.quasar.application.usecases.DownloadObject;
import com.astrallaris.quasar.application.usecases.QueryObjectsByBucket;
import com.astrallaris.quasar.application.usecases.UploadObject;
import com.astrallaris.quasar.domain.entities.StoredObject;

@RestController
public class ObjectsController {

    private final QueryObjectsByBucket queryObjectsByBucket;
    private final UploadObject uploadObject;
    private final DownloadObject downloadObject;
    private final DeleteObject deleteObject;

    public ObjectsController(QueryObjectsByBucket queryObjectsByBucket, UploadObject uploadObject,
            DownloadObject downloadObject, DeleteObject deleteObject) {
        this.queryObjectsByBucket = queryObjectsByBucket;
        this.uploadObject = uploadObject;
        this.downloadObject = downloadObject;
        this.deleteObject = deleteObject;
    }

    @GetMapping("/list")
    public List<StoredObject> list(@RequestParam String bucket) {
        return queryObjectsByBucket.execute(bucket);
    }

    @PostMapping("/upload")
    public ResponseEntity<StoredObject> upload(@RequestParam String bucket, @RequestParam String key,
            @RequestBody byte[] content) {
        return ResponseEntity.status(HttpStatus.CREATED).body(uploadObject.execute(bucket, key, content));
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> download(@RequestParam String bucket, @RequestParam String key) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(downloadObject.execute(bucket, key));
    }

    @DeleteMapping("/objects")
    public ResponseEntity<Void> delete(@RequestParam String bucket, @RequestParam String key) {
        deleteObject.execute(bucket, key);
        return ResponseEntity.noContent().build();
    }
}
