package com.sylvester.jobservice.service;


import com.sylvester.jobservice.dtos.CompanyDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;


@Component
@FeignClient(name = "company-service")
public interface CompanyClient {

    @GetMapping("/api/v1/companies/company/owner")
    CompanyDto getJobById(@RequestHeader("Authorization") String authorizationHeader);
}
