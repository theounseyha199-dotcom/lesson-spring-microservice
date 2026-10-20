package kh.edu.istad.platform.customer.restapi.dto;
import jakarta.validation.constraints.*;
public record CustomerUpdateRequest(@NotBlank @Size(max=100) String familyName,
                                    @NotBlank @Size(max=100) String givenName,
                                    @Size(min=1, max=100) String username,
                                    @Email @Size(max=254) String email,
                                    @Pattern(regexp="\\+?[0-9]{8,15}") String phoneNumber) {}
