package com.astrallaris.quasar.infrastructure.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.astrallaris.quasar.application.usecases.CreateBucket;
import com.astrallaris.quasar.application.usecases.QueryBuckets;
import com.astrallaris.quasar.domain.entities.Bucket;
import com.astrallaris.quasar.domain.repositories.BucketRepository;

@ExtendWith(MockitoExtension.class)
class BucketsControllerTest {

    @Mock
    private BucketRepository bucketRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new BucketsController(new QueryBuckets(bucketRepository), new CreateBucket(bucketRepository)))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void listReturnsBuckets() throws Exception {
        when(bucketRepository.findAll()).thenReturn(List.of(new Bucket(UUID.randomUUID(), "images")));

        mockMvc.perform(get("/buckets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("images"))
                .andExpect(jsonPath("$[0].id").exists());
    }

    @Test
    void createStoresNewBucket() throws Exception {
        when(bucketRepository.findByName("images")).thenReturn(null);

        mockMvc.perform(post("/buckets").param("name", "images"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("images"))
                .andExpect(jsonPath("$.id").exists());

        verify(bucketRepository).save(any(Bucket.class));
    }

    @Test
    void createReturnsConflictForExistingBucket() throws Exception {
        when(bucketRepository.findByName("images")).thenReturn(new Bucket(UUID.randomUUID(), "images"));

        mockMvc.perform(post("/buckets").param("name", "images"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("bucket already exists: images"));

        verify(bucketRepository, never()).save(any());
    }

    @Test
    void createReturnsBadRequestForUnsafeName() throws Exception {
        mockMvc.perform(post("/buckets").param("name", "../evil"))
                .andExpect(status().isBadRequest());

        verify(bucketRepository, never()).save(any());
    }

    @Test
    void createRequiresNameParameter() throws Exception {
        mockMvc.perform(post("/buckets"))
                .andExpect(status().isBadRequest());
    }
}
