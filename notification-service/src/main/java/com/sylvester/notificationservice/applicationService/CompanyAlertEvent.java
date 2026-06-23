package com.sylvester.notificationservice.applicationService;

public record CompanyAlertEvent(
        String companyEmail,
        String candidateEmail,
        String candidateFullName,
        String jobName
) {
}
