package com.sylvester.applicationservice.rabbitMq;

public record ApplicationEvent(
        String candidateEmail,
        String candidateFullName,
        String companyName,
        String jobName
) {

}
