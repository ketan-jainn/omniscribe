package com.omniscribe.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omniscribe.models.JobCreateRequest;
import com.omniscribe.repositories.JobRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JobRepository jobRepository;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
    }

    @Test
    void testCreateNewJob() throws Exception {
        String userId = "test_user_123";
        String idempotencyKey = "test_key_abc";
        JobCreateRequest request = new JobCreateRequest(userId, List.of("s3://test-bucket/chunk1.mp3"));

        mockMvc.perform(post("/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", idempotencyKey)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.idempotencyKey").value(idempotencyKey))
                .andExpect(jsonPath("$.status").value("PENDING_UPLOAD"))
                .andExpect(jsonPath("$.jobId").exists());
    }

    @Test
    void testIdempotentJobCreation() throws Exception {
        String userId = "test_user_456";
        String idempotencyKey = "test_key_def";
        JobCreateRequest request = new JobCreateRequest(userId, List.of("s3://test-bucket/chunk2.mp3"));

        // First request: 202 Accepted
        MvcResult firstResult = mockMvc.perform(post("/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", idempotencyKey)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andReturn();

        String jobId1 = objectMapper.readTree(firstResult.getResponse().getContentAsString()).get("jobId").asText();

        // Second request with same idempotency key: 200 OK
        MvcResult secondResult = mockMvc.perform(post("/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", idempotencyKey)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String jobId2 = objectMapper.readTree(secondResult.getResponse().getContentAsString()).get("jobId").asText();

        assertEquals(jobId1, jobId2);
        assertEquals(1, jobRepository.count());
    }

    @Test
    void testGetJobById() throws Exception {
        String userId = "test_user_get";
        String idempotencyKey = "test_key_get";
        JobCreateRequest request = new JobCreateRequest(userId, List.of("s3://test-bucket/chunk-get.mp3"));

        MvcResult createResult = mockMvc.perform(post("/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", idempotencyKey)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andReturn();

        String jobId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("jobId").asText();

        mockMvc.perform(get("/v1/jobs/" + jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.idempotencyKey").value(idempotencyKey));
    }

    @Test
    void testGetJobByIdReturns404ForMissingJob() throws Exception {
        mockMvc.perform(get("/v1/jobs/missing-job-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Job not found"));
    }

    @Test
    void testCreateJobRequiresIdempotencyKeyHeader() throws Exception {
        String jsonPayload = "{\"userId\": \"test_user_missing_header\", \"chunks\": [\"s3://test-bucket/chunk3.mp3\"]}";

        mockMvc.perform(post("/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void testCreateJobWithMissingRequiredField() throws Exception {
        String jsonPayload = "{\"chunks\": [\"s3://test-bucket/chunk3.mp3\"]}";

        mockMvc.perform(post("/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "bad_request_key")
                        .content(jsonPayload))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void testCreateJobWithInvalidChunksList() throws Exception {
        String jsonPayload = "{\"userId\": \"test_user_789\", \"chunks\": []}";

        mockMvc.perform(post("/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "invalid_chunks_key")
                        .content(jsonPayload))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").exists());
    }
}
