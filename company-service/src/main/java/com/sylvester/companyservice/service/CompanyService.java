package com.sylvester.companyservice.service;


import com.sylvester.companyservice.dtos.CompanyDto;
import com.sylvester.companyservice.dtos.RegisterRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public interface CompanyService {

    void createCompany(RegisterRequest request, String userId);
    void updateCompany(RegisterRequest request,String ownerId);
    void deleteCompany(String id);
    CompanyDto getCompany(String id);
}
