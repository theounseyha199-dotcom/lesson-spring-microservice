package kh.edu.istad.platform.business.restapi;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateBusinessRequest(@NotBlank String name, @NotBlank @Email String email,
                                    @NotBlank String address) {}
