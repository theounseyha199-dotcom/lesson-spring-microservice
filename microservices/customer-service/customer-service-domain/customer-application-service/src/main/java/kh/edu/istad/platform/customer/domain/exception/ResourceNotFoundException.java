package kh.edu.istad.platform.customer.domain.exception;
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource) { super(resource + " not found"); }
}
