package com.sylvester.companyservice.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record RegisterRequest(
        @NotBlank(message = "Name is required")
        String name,
        @NotBlank(message = "Industry is required")
        String industry,
        @NotBlank(message = "website is required")
        String website,
        @NotBlank(message = "Location is required")
         String location,
        @NotBlank(message = "Location is required")
         String phone,
        @Email(message = "Email must be in a valid format")
        @NotBlank(message = "Email is required")
        String email,
        @NotBlank(message = "Description is required")
        @Length(min = 15,  message = "Description must be more than 15 characters")
         String description
) {
}
