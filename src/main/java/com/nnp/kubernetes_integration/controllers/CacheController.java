package com.nnp.kubernetes_integration.controllers;

import com.nnp.kubernetes_integration.services.NnpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for cache eviction and invalidation operations.
 */
@RestController
@RequestMapping("cache")
@Tag(name = "Cache Controller", description = "In-memory cache invalidation endpoints")
public class CacheController {

    private final NnpService nnpService;

    @Autowired
    public CacheController(NnpService nnpService) {
        this.nnpService = nnpService;
    }

    @Operation(summary = "Clear Environment Cache", description = "Evicts cached environment mappings and tokens forcing a fresh fetch from NNP Dashboard")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Environment cache successfully evicted")
    })
    @DeleteMapping("env")
    public void clearNnpEnvironmentCache(){
        nnpService.clearAllEnvironmentsCache();
    }
}
