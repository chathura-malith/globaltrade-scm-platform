package com.globaltrade.web.advisor;

import com.globaltrade.core.exception.BaseApplicationException;
import com.globaltrade.core.util.StandardResponseDto;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable exception) {
        LOGGER.log(Level.SEVERE, "Exception caught by GlobalExceptionMapper: " + exception.getMessage(), exception);

        if (exception instanceof BaseApplicationException baseAppEx) {
            return Response.status(baseAppEx.getStatusCode())
                    .entity(new StandardResponseDto(baseAppEx.getStatusCode(), baseAppEx.getMessage(), null))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        if (exception instanceof NotAuthorizedException) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\", error_description=\"Invalid, expired or missing authentication token\"")
                    .entity(new StandardResponseDto(401, "Invalid, expired or missing authorization token", null))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        if (exception instanceof ForbiddenException) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(new StandardResponseDto(403, "Access denied: You do not have permission to access this resource", null))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        if (exception instanceof ProcessingException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new StandardResponseDto(400, "Malformed or empty request payload", null))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        if (exception instanceof WebApplicationException webAppEx) {
            int statusCode = webAppEx.getResponse().getStatus();
            return Response.status(statusCode)
                    .entity(new StandardResponseDto(statusCode, webAppEx.getMessage(), null))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new StandardResponseDto(500, "An unexpected internal error occurred: " + exception.getMessage(), null))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}