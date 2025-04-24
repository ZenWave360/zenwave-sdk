package com.example.clinical.config;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class WebExceptionsTranslator extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ProblemDetail body = ex.updateAndGetBody(getMessageSource(), LocaleContextHolder.getLocale());
        fillBindingErrors(body, ex);
        return this.createResponseEntity(body, headers, statusCode, request);
    }

    private void fillBindingErrors(ProblemDetail body, MethodArgumentNotValidException errors) {
        errors.getBindingResult()
                .getGlobalErrors()
                .forEach(fieldError -> body.setProperty(fieldError.getObjectName(), fieldError.toString()));
        errors.getBindingResult()
                .getFieldErrors()
                .forEach(fieldError -> body.setProperty(fieldError.getObjectName() + "." + fieldError.getField(),
                        fieldError.getDefaultMessage()));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        if (statusCode.is5xxServerError()) {
            logger.error(String.format("Internal Error: %s %s", request.getContextPath(), statusCode), ex);
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

}
