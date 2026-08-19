package com.globaltrade.web.advisor;

import com.globaltrade.core.exception.BaseApplicationException;
import com.globaltrade.core.util.StandardResponseDto;
import jakarta.ws.rs.WebApplicationException;
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
            StandardResponseDto errorResponse = new StandardResponseDto(
                    baseAppEx.getStatusCode(),
                    baseAppEx.getMessage(),
                    null
            );
            return Response.status(baseAppEx.getStatusCode())
                    .entity(errorResponse)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        if (exception instanceof WebApplicationException webAppEx) {
            int statusCode = webAppEx.getResponse().getStatus();
            StandardResponseDto errorResponse = new StandardResponseDto(
                    statusCode,
                    webAppEx.getMessage(),
                    null
            );
            return Response.status(statusCode)
                    .entity(errorResponse)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        StandardResponseDto errorResponse = new StandardResponseDto(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "An unexpected internal error occurred: " + exception.getMessage(),
                null
        );

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}