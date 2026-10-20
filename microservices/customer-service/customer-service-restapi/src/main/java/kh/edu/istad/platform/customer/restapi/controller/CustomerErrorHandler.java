package kh.edu.istad.platform.customer.restapi.controller;
import kh.edu.istad.platform.customer.domain.exception.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;

/** Consistent Problem Details errors expose business failures without leaking database internals. */
@RestControllerAdvice(basePackageClasses = CustomerController.class)
public class CustomerErrorHandler {
    @ExceptionHandler({CustomerNotFoundException.class, ResourceNotFoundException.class})
    public ProblemDetail notFound(RuntimeException error) { return problem(HttpStatus.NOT_FOUND, error.getMessage()); }
    @ExceptionHandler({ConflictException.class, CustomerDomainException.class})
    public ProblemDetail conflict(RuntimeException error) { return problem(HttpStatus.CONFLICT, error.getMessage()); }
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail invalid(IllegalArgumentException error) { return problem(HttpStatus.BAD_REQUEST, error.getMessage()); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validation(MethodArgumentNotValidException error) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request: " + error.getBindingResult().getFieldErrors().stream()
                .map(field -> field.getField() + " " + field.getDefaultMessage()).distinct().collect(java.util.stream.Collectors.joining(", ")));
    }
    private ProblemDetail problem(HttpStatus status, String detail) { return ProblemDetail.forStatusAndDetail(status, detail); }
}
