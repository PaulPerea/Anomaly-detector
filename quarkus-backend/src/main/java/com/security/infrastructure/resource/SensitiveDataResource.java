package com.security.infrastructure.resource;

import com.security.domain.model.SensitiveData;
import com.security.domain.model.User;
import com.security.shared.dto.Dtos.*;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;

@Path("/sensitive-data")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class SensitiveDataResource {

    @Inject
    JsonWebToken jwt;

    /**
     * ADMIN: ve todos los registros.
     * USER: solo ve sus propios datos.
     */
    @GET
    public Response getData() {
        String role = jwt.getClaim("role");
        Long userId = Long.parseLong(jwt.getSubject());

        List<SensitiveDataResponse> data;

        if ("ADMIN".equals(role)) {
            data = SensitiveData.<SensitiveData>listAll().stream()
                    .map(SensitiveDataResponse::from).toList();
        } else {
            data = SensitiveData.findByOwner(userId).stream()
                    .map(SensitiveDataResponse::from).toList();
        }

        return Response.ok(data).build();
    }

    /**
     * Solo ADMIN puede ver datos de un usuario específico
     */
    @GET
    @Path("/user/{userId}")
    @RolesAllowed("ADMIN")
    public Response getByUser(@PathParam("userId") Long userId) {
        List<SensitiveDataResponse> data = SensitiveData.findByOwner(userId).stream()
                .map(SensitiveDataResponse::from).toList();
        return Response.ok(data).build();
    }
}
