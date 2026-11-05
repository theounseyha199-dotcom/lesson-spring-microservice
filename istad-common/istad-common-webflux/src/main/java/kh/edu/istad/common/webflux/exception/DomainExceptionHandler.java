package kh.edu.istad.common.webflux.exception;

import kh.edu.istad.common.domain.Exception.DomainException;


import kh.edu.istad.common.webflux.dto.ErrorApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.bind.annotation.ExceptionHandler;

@RestControllerAdvice
public class DomainExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorApiResponse> handleException(DomainException ex, ServerWebExchange exchange) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorApiResponse(
                        ex.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR.value(), exchange.getRequest().getPath().value()
                ));
    }

}
