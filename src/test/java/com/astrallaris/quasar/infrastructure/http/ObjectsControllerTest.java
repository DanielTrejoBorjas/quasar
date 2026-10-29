package com.astrallaris.quasar.infrastructure.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.astrallaris.quasar.application.usecases.DeleteObject;
import com.astrallaris.quasar.application.usecases.DownloadObject;
import com.astrallaris.quasar.application.usecases.QueryObjectsByBucket;
import com.astrallaris.quasar.application.usecases.UploadObject;
import com.astrallaris.quasar.domain.entities.Bucket;
import com.astrallaris.quasar.domain.entities.StoredObject;
import com.astrallaris.quasar.domain.repositories.BucketRepository;
import com.astrallaris.quasar.domain.repositories.FilesystemRepository;
import com.astrallaris.quasar.domain.repositories.ObjectsRepository;

@ExtendWith(MockitoExtension.class)
class ObjectsControllerTest {

    private static final UUID BUCKET_ID = UUID.randomUUID();
    private static final Bucket BUCKET = new Bucket(BUCKET_ID, "images");

    @Mock
    private BucketRepository bucketRepository;

    @Mock
    private ObjectsRepository objectsRepository;

    @Mock
    private FilesystemRepository filesystemRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ObjectsController(
                        new QueryObjectsByBucket(bucketRepository, objectsRepository),
                        new UploadObject(bucketRepository, objectsRepository, filesystemRepository),
                        new DownloadObject(bucketRepository, objectsRepository, filesystemRepository),
                        new DeleteObject(bucketRepository, objectsRepository, filesystemRepository)))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void listReturnsObjectsOfBucket() throws Exception {
        StoredObject object = new StoredObject(UUID.randomUUID(), BUCKET_ID, "a.txt", "hash", 12, 1,
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
        when(bucketRepository.findByName("images")).thenReturn(BUCKET);
        when(objectsRepository.findByBucket(BUCKET_ID)).thenReturn(List.of(object));

        mockMvc.perform(get("/list").param("bucket", "images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("a.txt"))
                .andExpect(jsonPath("$[0].bucket_id").value(BUCKET_ID.toString()))
                .andExpect(jsonPath("$[0].hash").value("hash"))
                .andExpect(jsonPath("$[0].size").value(12))
                .andExpect(jsonPath("$[0].version").value(1))
                .andExpect(jsonPath("$[0].createAt").exists())
                .andExpect(jsonPath("$[0].updateAt").exists());
    }

    @Test
    void listReturnsNotFoundForUnknownBucket() throws Exception {
        when(bucketRepository.findByName("missing")).thenReturn(null);

        mockMvc.perform(get("/list").param("bucket", "missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("bucket not found: missing"));
    }

    @Test
    void listRequiresBucketParameter() throws Exception {
        mockMvc.perform(get("/list"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadStoresNewObject() throws Exception {
        byte[] content = "meow".getBytes(StandardCharsets.UTF_8);
        when(bucketRepository.findByName("images")).thenReturn(BUCKET);
        when(objectsRepository.findByBucketAndKey(BUCKET_ID, "a.txt")).thenReturn(null);

        mockMvc.perform(post("/upload")
                        .param("bucket", "images")
                        .param("key", "a.txt")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(content))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("a.txt"))
                .andExpect(jsonPath("$.bucket_id").value(BUCKET_ID.toString()))
                .andExpect(jsonPath("$.size").value(content.length))
                .andExpect(jsonPath("$.version").value(1));

        verify(filesystemRepository).write("images", "a.txt", content);
        verify(objectsRepository).save(any(StoredObject.class));
    }

    @Test
    void uploadOverwritesExistingObject() throws Exception {
        byte[] content = "meow".getBytes(StandardCharsets.UTF_8);
        StoredObject existing = new StoredObject(UUID.randomUUID(), BUCKET_ID, "a.txt", "old-hash", 3, 1,
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
        when(bucketRepository.findByName("images")).thenReturn(BUCKET);
        when(objectsRepository.findByBucketAndKey(BUCKET_ID, "a.txt")).thenReturn(existing);

        mockMvc.perform(post("/upload")
                        .param("bucket", "images")
                        .param("key", "a.txt")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(content))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(existing.id().toString()))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    void uploadFlattensSlashesInKey() throws Exception {
        byte[] content = "meow".getBytes(StandardCharsets.UTF_8);
        when(bucketRepository.findByName("images")).thenReturn(BUCKET);
        when(objectsRepository.findByBucketAndKey(BUCKET_ID, "photos-2024-cat.jpg")).thenReturn(null);

        mockMvc.perform(post("/upload")
                        .param("bucket", "images")
                        .param("key", "photos/2024/cat.jpg")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(content))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("photos-2024-cat.jpg"));

        verify(filesystemRepository).write("images", "photos-2024-cat.jpg", content);
    }

    @Test
    void uploadReturnsNotFoundForUnknownBucket() throws Exception {
        when(bucketRepository.findByName("missing")).thenReturn(null);

        mockMvc.perform(post("/upload")
                        .param("bucket", "missing")
                        .param("key", "a.txt")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content("meow".getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isNotFound());
    }

    @Test
    void uploadReturnsBadRequestForUnsafeKey() throws Exception {
        mockMvc.perform(post("/upload")
                        .param("bucket", "images")
                        .param("key", "..")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content("meow".getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isBadRequest());

        verify(filesystemRepository, never()).write(any(), any(), any());
    }

    @Test
    void downloadReturnsObjectContent() throws Exception {
        byte[] content = "meow".getBytes(StandardCharsets.UTF_8);
        StoredObject object = new StoredObject(UUID.randomUUID(), BUCKET_ID, "a.txt", "hash", 4, 1,
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
        when(bucketRepository.findByName("images")).thenReturn(BUCKET);
        when(objectsRepository.findByBucketAndKey(BUCKET_ID, "a.txt")).thenReturn(object);
        when(filesystemRepository.read("images", "a.txt")).thenReturn(java.util.Optional.of(content));

        mockMvc.perform(get("/download").param("bucket", "images").param("key", "a.txt"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(content().bytes(content));
    }

    @Test
    void downloadReturnsNotFoundForUnknownObject() throws Exception {
        when(bucketRepository.findByName("images")).thenReturn(BUCKET);
        when(objectsRepository.findByBucketAndKey(BUCKET_ID, "missing.txt")).thenReturn(null);

        mockMvc.perform(get("/download").param("bucket", "images").param("key", "missing.txt"))
                .andExpect(status().isNotFound());
    }

    @Test
    void downloadReturnsNotFoundForUnknownBucket() throws Exception {
        when(bucketRepository.findByName("missing")).thenReturn(null);

        mockMvc.perform(get("/download").param("bucket", "missing").param("key", "a.txt"))
                .andExpect(status().isNotFound());
    }

    @Test
    void downloadRequiresKeyParameter() throws Exception {
        mockMvc.perform(get("/download").param("bucket", "images"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteRemovesObjectAndBlob() throws Exception {
        StoredObject object = new StoredObject(UUID.randomUUID(), BUCKET_ID, "a.txt", "hash", 4, 1,
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
        when(bucketRepository.findByName("images")).thenReturn(BUCKET);
        when(objectsRepository.findByBucketAndKey(BUCKET_ID, "a.txt")).thenReturn(object);

        mockMvc.perform(delete("/objects").param("bucket", "images").param("key", "a.txt"))
                .andExpect(status().isNoContent());

        verify(objectsRepository).deleteById(object.id());
        verify(filesystemRepository).delete("images", "a.txt");
    }

    @Test
    void deleteReturnsNotFoundForUnknownObject() throws Exception {
        when(bucketRepository.findByName("images")).thenReturn(BUCKET);
        when(objectsRepository.findByBucketAndKey(BUCKET_ID, "missing.txt")).thenReturn(null);

        mockMvc.perform(delete("/objects").param("bucket", "images").param("key", "missing.txt"))
                .andExpect(status().isNotFound());

        verify(objectsRepository, never()).deleteById(any());
    }
}
