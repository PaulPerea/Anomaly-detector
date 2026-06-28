package com.security.infrastructure.client;

import com.security.shared.dto.Dtos.AiAnalysisRequest;
import com.security.shared.dto.Dtos.AiAnalysisResponse;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "ai-service")
@Path("/ai")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface AiServiceClient {

    @POST
    @Path("/analyze")
    Uni<AiAnalysisResponse> analyze(AiAnalysisRequest request);
}
