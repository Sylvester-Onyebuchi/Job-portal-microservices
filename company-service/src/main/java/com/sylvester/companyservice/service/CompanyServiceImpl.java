package com.sylvester.companyservice.service;

import com.sylvester.companyservice.dtos.CompanyDto;
import com.sylvester.companyservice.dtos.RegisterRequest;
import com.sylvester.companyservice.entity.Company;
import com.sylvester.companyservice.exceptions.AlreadyExistsException;
import com.sylvester.companyservice.exceptions.NotFoundException;
import com.sylvester.companyservice.rabbitMq.CreateCompanyEvent;
import com.sylvester.companyservice.rabbitMq.Producer;
import com.sylvester.companyservice.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final Producer producer;


    @Override
    @Transactional
    public void createCompany(RegisterRequest request, String userId) {

        if (companyRepository.findByName(request.name()).isPresent()) {
            throw new AlreadyExistsException("Company with name " + request.name() + " already exists");
        }

        if (companyRepository.findCompanyByOwnerId(userId).isPresent()) {
            throw new AlreadyExistsException("You can only have one company");
        }
        Company newCompany = Company.builder()
                .name(request.name())
                .ownerId(userId)
                .email(request.email())
                .industry(request.industry())
                .location(request.location())
                .phone(request.phone())
                .description(request.description())
                .createdAt(LocalDateTime.now())
                .website(request.website())
                .build();
        var savedCompany = companyRepository.save(newCompany);
        log.info("Company with name {} has been created", newCompany.getName());
        CreateCompanyEvent createCompanyEvent = new CreateCompanyEvent(
                savedCompany.getName(), savedCompany.getLocation(),
                savedCompany.getEmail(), savedCompany.getWebsite()
        );
        producer.sendMessage(createCompanyEvent);

    }

    @Override
    @Transactional
    public void updateCompany(RegisterRequest request,String ownerId) {
        var company = companyRepository.findCompanyByOwnerId(ownerId).orElseThrow(
                () -> new NotFoundException("Company not found")
        );
        company.setName(request.name());
        company.setEmail(request.email());
        company.setLocation(request.location());
        company.setPhone(request.phone());
        company.setDescription(request.description());
        company.setIndustry(request.industry());
        company.setWebsite(request.website());
        companyRepository.save(company);
        log.info("Company with name {} has been updated", company.getName());
    }

    @Override
    @Transactional
    public void deleteCompany(String id) {

        var company = companyRepository.findCompanyById(id).orElseThrow(
                () -> new NotFoundException("Company with not found")
        );
        companyRepository.delete(company);
        log.info("Company with name {} has been deleted", company.getName());

    }

    @Override
    @Transactional(readOnly = true)
    public CompanyDto getCompany(String ownerId) {

        var company = companyRepository.findCompanyByOwnerId(ownerId).orElseThrow(
                () -> new NotFoundException("Company not found")
        );

        log.info("Company with name {} has been found", company.getName());
        CompanyDto companyDto = new CompanyDto();
        companyDto.setId(company.getId());
        companyDto.setOwnerId(company.getOwnerId());
        companyDto.setName(company.getName());
        companyDto.setWebsite(company.getWebsite());
        companyDto.setLocation(company.getLocation());
        companyDto.setDescription(company.getDescription());
        companyDto.setEmail(company.getEmail());
        return companyDto;



    }

}
