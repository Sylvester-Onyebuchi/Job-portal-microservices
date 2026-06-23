package com.sylvester.applicationservice.dtos;

import java.util.Set;

public record SubmitApplicationRequest(
        String jobId,

        String fullName,

        String email,

        String phoneNumber,

        String coverLetter,

        String portfolioUrl,

        String linkedInUrl,

        String githubUrl,

        Set<String> skills,

        Set<String> languages,

        Set<EducationRequest> education,

        Set<ExperienceRequest> experience
) {
}
