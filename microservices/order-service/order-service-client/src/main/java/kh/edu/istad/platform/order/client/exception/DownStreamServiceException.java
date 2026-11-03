package kh.edu.istad.platform.order.client.exception;

import org.springframework.web.reactive.function.client.WebClientResponseException;

public class DownStreamServiceException extends RuntimeException {
    private final String service;


    public DownStreamServiceException(String service, WebClientResponseException ex) {
        super("Downstream service " + service + " is unavailable", ex);
        this.service = service;
    }

    public String getService() {
        return service;
    }
}
