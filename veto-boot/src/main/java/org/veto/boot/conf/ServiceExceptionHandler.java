package org.veto.boot.conf;

import jakarta.annotation.Resource;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.veto.shared.Response;
import org.veto.shared.exception.VETO_EXCEPTION_CODE;

import java.util.Map;

/**
 * Global exception handler for service
 */
@RestControllerAdvice
public class ServiceExceptionHandler {

    /**
     * Handle validation exceptions
     * @param exception the validation exception
     * @return appropriate error response
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Response handleMethodArgumentNotValid(
            ConstraintViolationException  exception) {
//        return exception.getConstraintViolations().stream().findFirst()
//                .map(violation -> Response.error(errorCodeResolver.get(violation.getMessage())))
//                .orElseGet(() -> Response.error(errorCodeResolver.get("error.params")));
        return exception.getConstraintViolations().stream().findFirst()
                .map(violation -> Response.error(violation.getMessage()))
                .orElseGet(() -> Response.error(VETO_EXCEPTION_CODE.PARAMS_INVALID));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Response handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        return exception.getBindingResult().getFieldErrors().stream().findFirst()
                .map(violation -> Response.error(violation.getDefaultMessage()))
                .orElseGet(() -> Response.error(VETO_EXCEPTION_CODE.PARAMS_INVALID));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response handleInvalidJson(HttpMessageNotReadableException ex){
        return Response
                .error(VETO_EXCEPTION_CODE.PARAMS_INVALID, ex.getMostSpecificCause().getMessage());
    }
}