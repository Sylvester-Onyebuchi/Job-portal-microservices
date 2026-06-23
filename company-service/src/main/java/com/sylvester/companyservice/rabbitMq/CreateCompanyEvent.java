package com.sylvester.companyservice.rabbitMq;

public record CreateCompanyEvent(
        String name,
        String location,
        String email,
        String website
) {

}
