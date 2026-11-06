package istad.ha.iamservice.restapi.error;

import istad.ha.iamservice.application.exception.ApplicationException;
import istad.ha.iamservice.domain.exception.DomainException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.net.URI;
import java.util.*;

@RestControllerAdvice(basePackages = "istad.ha.iamservice.restapi.controller")
public class ApiErrors extends ResponseEntityExceptionHandler {
    public record FieldError(String field, String message) {}

    public static ProblemDetail problem(HttpStatus status, String code, String detail) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("https://iam.istad.co/problems/" + code.replace('_', '-')));
        problem.setTitle(status == HttpStatus.BAD_REQUEST ? "Validation failed" : status.getReasonPhrase());
        return problem;
    }

    @ExceptionHandler(ApplicationException.class)
    ProblemDetail application(ApplicationException ex) {
        var result = failure(ex.code(), ex.getMessage());
        if (!ex.errors().isEmpty()) result.setProperty("errors", ex.errors().entrySet().stream()
                .map(e -> new FieldError(e.getKey(), e.getValue())).toList());
        return result;
    }
    @ExceptionHandler(DomainException.class)
    ProblemDetail domain(DomainException ex) { return failure(ex.code(), ex.getMessage()); }

    private ProblemDetail failure(String code, String detail) {
        HttpStatus status = switch (code) {
            case "not_found", "identity_not_found" -> HttpStatus.NOT_FOUND;
            case "validation", "invalid_current_password", "profile_completion_required" -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.CONFLICT;
        };
        var result = problem(status, status == HttpStatus.BAD_REQUEST ? "validation-error" : code, detail);
        result.setProperty("code", code);
        if (status == HttpStatus.BAD_REQUEST) result.setProperty("errors", List.of(new FieldError("request", detail)));
        return result;
    }
    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail forbidden(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "forbidden", "You do not have permission to perform this operation");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var result = problem(HttpStatus.BAD_REQUEST, "validation-error", "Correct the invalid request fields");
        result.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldError(e.getField(), Objects.requireNonNullElse(e.getDefaultMessage(), "Invalid value"))).toList());
        return handleExceptionInternal(ex, result, headers, status, request);
    }
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        // Never return parser messages: they may contain passwords or provider proof.
        var result = problem(HttpStatus.BAD_REQUEST, "validation-error", "Malformed JSON, unsupported field, or invalid value type");
        result.setProperty("errors", List.of(new FieldError("request", "Check the JSON fields and value types")));
        return handleExceptionInternal(ex, result, headers, status, request);
    }
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(org.springframework.beans.TypeMismatchException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String field = ex instanceof MethodArgumentTypeMismatchException mismatch ? mismatch.getName() : "request";
        var result = problem(HttpStatus.BAD_REQUEST, "validation-error", "Invalid path or query parameter");
        result.setProperty("errors", List.of(new FieldError(field, "Invalid value type")));
        return handleExceptionInternal(ex, result, headers, status, request);
    }
}
