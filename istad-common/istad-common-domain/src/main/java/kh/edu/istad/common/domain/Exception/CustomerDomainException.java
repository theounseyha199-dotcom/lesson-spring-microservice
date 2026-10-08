package kh.edu.istad.common.domain.Exception;

public class CustomerDomainException extends DomainException{
    public CustomerDomainException(String message) {
        super(message);
    }

    public CustomerDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
