package com.campusops.asset.controller;

import com.campusops.asset.dto.request.CreateAssetRequest;
import com.campusops.asset.dto.request.UpdateAssetRequest;
import com.campusops.asset.dto.response.AssetResponse;
import com.campusops.asset.entity.AssetStatus;
import com.campusops.asset.entity.AssetType;
import com.campusops.asset.exception.GlobalExceptionHandler;
import com.campusops.asset.exception.ResourceNotFoundException;
import com.campusops.asset.service.AssetService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AssetControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private AssetService assetService;

    @InjectMocks
    private AssetController assetController;

    private AssetResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(assetController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        sampleResponse = AssetResponse.builder()
                .id("asset-100")
                .name("Epson EB-2250U")
                .type(AssetType.PROJECTOR)
                .location("Lab 3")
                .status(AssetStatus.ACTIVE)
                .attributes(Map.of("resolution", "1920x1080", "lampHours", 50))
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/assets with valid payload should return 201 Created")
    void createAsset_withValidPayload_shouldReturn201() throws Exception {
        CreateAssetRequest request = CreateAssetRequest.builder()
                .name("Epson EB-2250U")
                .type(AssetType.PROJECTOR)
                .location("Lab 3")
                .status(AssetStatus.ACTIVE)
                .attributes(Map.of("resolution", "1920x1080"))
                .build();

        when(assetService.createAsset(any(CreateAssetRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is("asset-100")))
                .andExpect(jsonPath("$.name", is("Epson EB-2250U")))
                .andExpect(jsonPath("$.type", is("PROJECTOR")))
                .andExpect(jsonPath("$.location", is("Lab 3")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    @DisplayName("POST /api/v1/assets with blank fields should return 400 Bad Request with validation errors")
    void createAsset_withInvalidPayload_shouldReturn400() throws Exception {
        CreateAssetRequest invalidRequest = CreateAssetRequest.builder()
                .name("")
                .type(null)
                .location("")
                .build();

        mockMvc.perform(post("/api/v1/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.type").exists())
                .andExpect(jsonPath("$.validationErrors.location").exists());
    }

    @Test
    @DisplayName("GET /api/v1/assets should return 200 OK with list of assets")
    void getAllAssets_shouldReturn200() throws Exception {
        when(assetService.getAllAssets(null, null)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/assets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is("asset-100")))
                .andExpect(jsonPath("$[0].name", is("Epson EB-2250U")));
    }

    @Test
    @DisplayName("GET /api/v1/assets/{id} with existing ID should return 200 OK")
    void getAssetById_whenFound_shouldReturn200() throws Exception {
        when(assetService.getAssetById("asset-100")).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/assets/asset-100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("asset-100")))
                .andExpect(jsonPath("$.name", is("Epson EB-2250U")));
    }

    @Test
    @DisplayName("GET /api/v1/assets/{id} when missing should return 404 Not Found")
    void getAssetById_whenNotFound_shouldReturn404() throws Exception {
        when(assetService.getAssetById("missing-100"))
                .thenThrow(new ResourceNotFoundException("Asset not found with id: missing-100"));

        mockMvc.perform(get("/api/v1/assets/missing-100"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Asset not found with id: missing-100")));
    }

    @Test
    @DisplayName("PUT /api/v1/assets/{id} with valid payload should return 200 OK")
    void updateAsset_withValidPayload_shouldReturn200() throws Exception {
        UpdateAssetRequest updateRequest = UpdateAssetRequest.builder()
                .name("Epson EB-2250U (Updated)")
                .type(AssetType.PROJECTOR)
                .location("Lab 4")
                .status(AssetStatus.UNDER_MAINTENANCE)
                .build();

        AssetResponse updatedResponse = AssetResponse.builder()
                .id("asset-100")
                .name("Epson EB-2250U (Updated)")
                .type(AssetType.PROJECTOR)
                .location("Lab 4")
                .status(AssetStatus.UNDER_MAINTENANCE)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(assetService.updateAsset(eq("asset-100"), any(UpdateAssetRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/v1/assets/asset-100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("asset-100")))
                .andExpect(jsonPath("$.name", is("Epson EB-2250U (Updated)")))
                .andExpect(jsonPath("$.location", is("Lab 4")))
                .andExpect(jsonPath("$.status", is("UNDER_MAINTENANCE")));
    }

    @Test
    @DisplayName("PUT /api/v1/assets/{id} with missing fields should return 400 Bad Request")
    void updateAsset_withInvalidPayload_shouldReturn400() throws Exception {
        UpdateAssetRequest invalidRequest = UpdateAssetRequest.builder()
                .name("")
                .type(null)
                .location(null)
                .build();

        mockMvc.perform(put("/api/v1/assets/asset-100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.type").exists())
                .andExpect(jsonPath("$.validationErrors.location").exists());
    }

    @Test
    @DisplayName("DELETE /api/v1/assets/{id} should return 204 No Content")
    void deleteAsset_shouldReturn204() throws Exception {
        doNothing().when(assetService).deleteAsset("asset-100");

        mockMvc.perform(delete("/api/v1/assets/asset-100"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/assets/{id} when not found should return 404 Not Found")
    void deleteAsset_whenNotFound_shouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Asset not found with id: missing-100"))
                .when(assetService).deleteAsset("missing-100");

        mockMvc.perform(delete("/api/v1/assets/missing-100"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }
}
