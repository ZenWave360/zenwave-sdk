package io.zenwave360.example.config

import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.util.function.Consumer

@ControllerAdvice
class WebExceptionsTranslator : ResponseEntityExceptionHandler() {
    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders, statusCode: HttpStatusCode, request: WebRequest
    ): ResponseEntity<Any> {
        val body = ex.updateAndGetBody(getMessageSource(), LocaleContextHolder.getLocale())
        fillBindingErrors(body, ex)
        return this.createResponseEntity(body, headers, statusCode, request)
    }

    private fun fillBindingErrors(body: ProblemDetail, errors: MethodArgumentNotValidException) {
        errors.getBindingResult()
            .getGlobalErrors()
            .forEach(Consumer { fieldError: ObjectError? ->
                body.setProperty(
                    fieldError!!.getObjectName(),
                    fieldError.toString()
                )
            })
        errors.getBindingResult()
            .getFieldErrors()
            .forEach(Consumer { fieldError: FieldError? ->
                body.setProperty(
                    fieldError!!.getObjectName() + "." + fieldError.getField(),
                    fieldError.getDefaultMessage()
                )
            })
    }

    override fun handleExceptionInternal(
        ex: Exception, body: Any?, headers: HttpHeaders,
        statusCode: HttpStatusCode, request: WebRequest
    ): ResponseEntity<Any>? {
        if (statusCode.is5xxServerError()) {
            logger.error(String.format("Internal Error: %s %s", request.getContextPath(), statusCode), ex)
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request)
    }
}
