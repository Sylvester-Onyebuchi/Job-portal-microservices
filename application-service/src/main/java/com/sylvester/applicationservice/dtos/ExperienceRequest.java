package com.sylvester.applicationservice.dtos;

public record ExperienceRequest(
        String company,

        String position,

        String description,

        Integer startYear,

        Integer endYear,
        String stillWorking

) {
}
