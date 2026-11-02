package kh.edu.istad.common.webflux.exception;


import kh.edu.istad.common.webflux.dto.ErrorApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.bind.annotation.ExceptionHandler;

@RestControllerAdvice
public class DomainExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorApiResponse> handleException(RuntimeException ex, ServerWebExchange exchange) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorApiResponse(
                        ex.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR.value(), exchange.getRequest().getPath().value()
                ));
    }

}