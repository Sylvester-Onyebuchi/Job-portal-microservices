package com.sylvester.applicationservice.rabbitMq;

public record CompanyAlertEvent(
        String companyEmail,
        String candidateEmail,
        String candidateFullName,
        String jobName
) {
}
