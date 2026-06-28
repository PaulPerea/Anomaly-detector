package com.security.infrastructure.resource;

import com.security.application.service.AlertService;
import com.security.shared.dto.Dtos.AlertResponse;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/alerts")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class AlertResource {

    @Inject
    AlertService alertService;

    @GET
    @RolesAllowed("ADMIN")
    public List<AlertResponse> getAll(@QueryParam("pending") boolean pendingOnly) {
        return pendingOnly ? alertService.getPending() : alertService.getAll();
    }

    @GET
    @Path("/user/{userId}")
    @RolesAllowed("ADMIN")
    public List<AlertResponse> getByUser(@PathParam("userId") Long userId) {
        return alertService.getByUser(userId);
    }

    @PUT
    @Path("/{id}/acknowledge")
    @RolesAllowed("ADMIN")
    public Response acknowledge(@PathParam("id") Long id) {
        return Response.ok(alertService.acknowledge(id)).build();
    }
}
