package com.security.infrastructure.resource;

import com.security.application.service.AuthService;
import com.security.shared.dto.Dtos.*;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService authService;

    @POST
    @Path("/login")
    public Response login(@Valid LoginRequest req) {
        LoginResponse response = authService.login(req);
        return Response.ok(response).build();
    }

    @POST
    @Path("/register")
    public Response register(@Valid CreateUserRequest req) {
        UserResponse response = authService.register(req);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }
}
