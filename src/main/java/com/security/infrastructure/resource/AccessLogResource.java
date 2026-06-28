package com.security.infrastructure.resource;

import com.security.application.service.AuditService;
import com.security.shared.dto.Dtos.AccessLogResponse;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/access-log")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class AccessLogResource {

    @Inject
    AuditService auditService;

    @GET
    @RolesAllowed("ADMIN")
    public List<AccessLogResponse> getAll() {
        return auditService.getAll();
    }

    @GET
    @Path("/high-risk")
    @RolesAllowed("ADMIN")
    public List<AccessLogResponse> getHighRisk() {
        return auditService.getHighRisk();
    }
}
