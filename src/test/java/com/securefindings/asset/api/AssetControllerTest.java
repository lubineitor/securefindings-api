package com.securefindings.asset.api;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.securefindings.api.error.GlobalExceptionHandler;
import com.securefindings.asset.application.AssetHasFindingsException;
import com.securefindings.asset.application.AssetIdentifierAlreadyExistsException;
import com.securefindings.asset.application.AssetService;
import com.securefindings.asset.domain.Asset;
import com.securefindings.asset.domain.AssetCriticality;
import com.securefindings.asset.domain.AssetEnvironment;
import com.securefindings.asset.domain.AssetStatus;
import com.securefindings.asset.domain.AssetType;
import com.securefindings.security.SecurityConfig;

@WebMvcTest(controllers = AssetController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
@WithMockUser(username = "analista", roles = "ANALYST")
class AssetControllerTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private AssetService assetService;

    private MockMvc mockMvc;

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void deberiaListarActivosPaginados() throws Exception {
        when(assetService.findPage(0, 20, null, null, null, null, null))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/assets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void deberiaRechazarUnFiltroDeTipoDesconocido() throws Exception {
        mockMvc.perform(get("/api/v1/assets").param("type", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(assetService);
    }

    @Test
    @WithMockUser(username = "administrador", roles = "ADMIN")
    void deberiaCrearUnActivoYDevolverLocation() throws Exception {
        Asset asset = sampleAsset(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        when(assetService.create(
                "API pagos",
                "https://payments.example.test",
                AssetType.API,
                AssetEnvironment.PRODUCTION,
                AssetCriticality.HIGH))
                .thenReturn(asset);

        mockMvc.perform(post("/api/v1/assets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "API pagos",
                          "identifier": "https://payments.example.test",
                          "type": "API",
                          "environment": "PRODUCTION",
                          "criticality": "HIGH"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        org.hamcrest.Matchers.endsWith(
                                "/api/v1/assets/22222222-2222-2222-2222-222222222222")))
                .andExpect(jsonPath("$.name").value("API pagos"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(assetService).create(
                "API pagos",
                "https://payments.example.test",
                AssetType.API,
                AssetEnvironment.PRODUCTION,
                AssetCriticality.HIGH);
    }

    @Test
    void analystNoPuedeCrearActivos() throws Exception {
        mockMvc.perform(post("/api/v1/assets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(assetService);
    }

    @Test
    void analystNoPuedeEliminarActivos() throws Exception {
        mockMvc.perform(delete("/api/v1/assets/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(assetService);
    }

    @Test
    @WithMockUser(username = "administrador", roles = "ADMIN")
    void deberiaPermitirArchivarConUnaActualizacionParcial() throws Exception {
        UUID assetId = UUID.randomUUID();
        Asset archivedAsset = new Asset(
                assetId,
                "API pagos",
                "https://payments.example.test",
                AssetType.API,
                AssetEnvironment.PRODUCTION,
                AssetCriticality.HIGH,
                AssetStatus.ARCHIVED,
                Instant.parse("2026-10-01T10:00:00Z"),
                Instant.parse("2026-10-02T10:00:00Z"));
        when(assetService.update(
                assetId,
                null,
                null,
                null,
                null,
                null,
                AssetStatus.ARCHIVED))
                .thenReturn(archivedAsset);

        mockMvc.perform(patch("/api/v1/assets/{id}", assetId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "status": "ARCHIVED" }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        verify(assetService).update(
                assetId,
                null,
                null,
                null,
                null,
                null,
                AssetStatus.ARCHIVED);
    }

    @Test
    @WithMockUser(username = "administrador", roles = "ADMIN")
    void deberiaRechazarUnNombreEnBlancoEnLaActualizacionParcial() throws Exception {
        mockMvc.perform(patch("/api/v1/assets/{id}", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "   " }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(assetService);
    }

    @Test
    @WithMockUser(username = "administrador", roles = "ADMIN")
    void deberiaValidarLosCamposObligatoriosAlCrearUnActivo() throws Exception {
        mockMvc.perform(post("/api/v1/assets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.name")
                        .value("El nombre del activo es obligatorio"));

        verifyNoInteractions(assetService);
    }

    @Test
    @WithMockUser(username = "administrador", roles = "ADMIN")
    void deberiaResponder409SiElIdentificadorYaExiste() throws Exception {
        when(assetService.create(
                "API pagos",
                "payments-api",
                AssetType.API,
                AssetEnvironment.PRODUCTION,
                AssetCriticality.HIGH))
                .thenThrow(new AssetIdentifierAlreadyExistsException("payments-api"));

        mockMvc.perform(post("/api/v1/assets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "API pagos",
                          "identifier": "payments-api",
                          "type": "API",
                          "environment": "PRODUCTION",
                          "criticality": "HIGH"
                        }
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("ASSET_IDENTIFIER_ALREADY_EXISTS"));
    }

    @Test
    @WithMockUser(username = "administrador", roles = "ADMIN")
    void deberiaResponder409AlEliminarUnActivoAsociado() throws Exception {
        UUID assetId = UUID.randomUUID();
        doThrow(new AssetHasFindingsException(assetId))
                .when(assetService).delete(assetId);

        mockMvc.perform(delete("/api/v1/assets/{id}", assetId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ASSET_HAS_FINDINGS"));
    }

    private Asset sampleAsset(UUID id) {
        Instant now = Instant.parse("2026-10-01T10:00:00Z");
        return new Asset(
                id,
                "API pagos",
                "https://payments.example.test",
                AssetType.API,
                AssetEnvironment.PRODUCTION,
                AssetCriticality.HIGH,
                AssetStatus.ACTIVE,
                now,
                now);
    }
}