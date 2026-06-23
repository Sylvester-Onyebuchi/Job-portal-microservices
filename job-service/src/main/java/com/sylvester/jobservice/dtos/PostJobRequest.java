package com.sylvester.jobservice.dtos;


import jakarta.validation.constraints.NotBlank;

public record PostJobRequest(
        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Location is required")
        String location,

        @NotBlank(message = "Employment is required")
        String employmentType,

        @NotBlank(message = "WorkMode is required")
        String workMode,

        @NotBlank(message = "Description is required")
        String description,

        @NotBlank(message = "Responsibility is required")
        String responsibilities,

        @NotBlank(message = "Qualification is required")
        String qualifications,

        @NotBlank(message = "Benefit is required")
        String benefits,


        String whyJoinUs
) {
}
