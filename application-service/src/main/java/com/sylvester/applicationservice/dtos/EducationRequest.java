package com.sylvester.applicationservice.dtos;

public record EducationRequest(
        String school,

        String degree,

        String fieldOfStudy,

        Integer startYear,

        Integer endYear
) {
}
