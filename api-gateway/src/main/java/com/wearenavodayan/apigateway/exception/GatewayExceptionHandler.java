package com.wearenavodayan.apigateway.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;

@Component
public class GatewayExceptionHandler implements HandlerExceptionResolver {

    private final ApiErrorResponseWriter errorResponseWriter;

    public GatewayExceptionHandler(ApiErrorResponseWriter errorResponseWriter) {
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    public ModelAndView resolveException(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception) {

        if (response.isCommitted()) {
            return null;
        }

        try {
            errorResponseWriter.write(
                    request,
                    response,
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                    "An unexpected error occurred.");

            return new ModelAndView();
        } catch (IOException writeException) {
            return null;
        }
    }
}