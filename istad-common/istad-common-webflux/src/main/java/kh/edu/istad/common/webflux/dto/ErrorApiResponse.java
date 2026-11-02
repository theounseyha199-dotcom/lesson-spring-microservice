package kh.edu.istad.common.webflux.dto;

public record ErrorApiResponse(String message, int status, String path) {
}
