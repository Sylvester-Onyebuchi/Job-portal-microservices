package com.sylvester.notificationservice.applicationService;

public record ApplicationEvent(
        String candidateEmail,
        String candidateFullName,
        String companyName,
        String jobName
) {

}
