package com.sylvester.companyservice.service;

import com.sylvester.companyservice.dtos.CompanyDto;
import com.sylvester.companyservice.dtos.RegisterRequest;
import com.sylvester.companyservice.entity.Company;
import com.sylvester.companyservice.exceptions.AlreadyExistsException;
import com.sylvester.companyservice.exceptions.NotFoundException;
import com.sylvester.companyservice.rabbitMq.CreateCompanyEvent;
import com.sylvester.companyservice.rabbitMq.Producer;
import com.sylvester.companyservice.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CompanyServiceImplTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private Producer producer;

    @InjectMocks
    private CompanyServiceImpl companyService;

    private RegisterRequest request;
    private Company company;

    @BeforeEach
    void setUp() {
        request = new RegisterRequest(
                "Test Inc",
                "Software",
                "https://test.example.com",
                "Zagreb",
                "+3858012345678",
                "contact@test.example.com",
                "A software company building hiring tools"
        );

        company = Company.builder()
                .id("company-id")
                .ownerId("owner-id")
                .name("Acme Inc")
                .email("contact@acme.example.com")
                .industry("Software")
                .location("Zagreb")
                .phone("+3858012345678")
                .website("https://test.example.com")
                .description("A software company building hiring tools")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createCompany_shouldSaveCompanyAndPublishEvent() {
        given(companyRepository.findByName(request.name())).willReturn(Optional.empty());
        given(companyRepository.save(any(Company.class))).willReturn(company);

        companyService.createCompany(request, "owner-id");

        ArgumentCaptor<Company> companyCaptor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).save(companyCaptor.capture());
        Company savedCompany = companyCaptor.getValue();
        assertEquals(request.name(), savedCompany.getName());
        assertEquals("owner-id", savedCompany.getOwnerId());
        assertEquals(request.email(), savedCompany.getEmail());
        assertEquals(request.industry(), savedCompany.getIndustry());
        assertEquals(request.location(), savedCompany.getLocation());
        assertEquals(request.phone(), savedCompany.getPhone());
        assertEquals(request.website(), savedCompany.getWebsite());
        assertEquals(request.description(), savedCompany.getDescription());
        assertNotNull(savedCompany.getCreatedAt());

        ArgumentCaptor<CreateCompanyEvent> eventCaptor =
                ArgumentCaptor.forClass(CreateCompanyEvent.class);
        verify(producer).sendMessage(eventCaptor.capture());
        CreateCompanyEvent event = eventCaptor.getValue();
        assertEquals(company.getName(), event.name());
        assertEquals(company.getLocation(), event.location());
        assertEquals(company.getEmail(), event.email());
        assertEquals(company.getWebsite(), event.website());
    }

    @Test
    void createCompany_shouldThrowAlreadyExistsException_whenCompanyNameExists() {
        given(companyRepository.findByName(request.name())).willReturn(Optional.of(company));

        assertThrows(
                AlreadyExistsException.class,
                () -> companyService.createCompany(request, "owner-id")
        );

        verify(companyRepository, never()).save(any(Company.class));
        verify(producer, never()).sendMessage(any(CreateCompanyEvent.class));
    }

    @Test
    void updateCompany_shouldUpdateExistingCompany() {
        RegisterRequest updateRequest = new RegisterRequest(
                "Test Updated",
                "Fintech",
                "https://updated.example.com",
                "Zagreb",
                "+3858099999999",
                "hello@updated.example.com",
                "An updated company description"
        );
        given(companyRepository.findCompanyByOwnerId("owner-id")).willReturn(Optional.of(company));

        companyService.updateCompany(updateRequest, "owner-id");

        assertEquals(updateRequest.name(), company.getName());
        assertEquals(updateRequest.email(), company.getEmail());
        assertEquals(updateRequest.location(), company.getLocation());
        assertEquals(updateRequest.phone(), company.getPhone());
        assertEquals(updateRequest.description(), company.getDescription());
        assertEquals(updateRequest.industry(), company.getIndustry());
        assertEquals(updateRequest.website(), company.getWebsite());
        verify(companyRepository).save(company);
    }

    @Test
    void updateCompany_shouldThrowNotFoundException_whenCompanyDoesNotExist() {
        given(companyRepository.findCompanyByOwnerId("owner-id")).willReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> companyService.updateCompany(request, "owner-id")
        );

        verify(companyRepository, never()).save(any(Company.class));
    }

    @Test
    void deleteCompany_shouldDeleteExistingCompany() {
        given(companyRepository.findCompanyByOwnerId("owner-id")).willReturn(Optional.of(company));

        companyService.deleteCompany("owner-id");

        verify(companyRepository).delete(company);
    }

    @Test
    void deleteCompany_shouldThrowNotFoundException_whenCompanyDoesNotExist() {
        given(companyRepository.findCompanyByOwnerId("owner-id")).willReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> companyService.deleteCompany("owner-id"));

        verify(companyRepository, never()).delete(any(Company.class));
    }

    @Test
    void getCompany_shouldReturnCompanyDto() {
        given(companyRepository.findCompanyByOwnerId("owner-id")).willReturn(Optional.of(company));

        CompanyDto companyDto = companyService.getCompany("owner-id");

        assertEquals(company.getId(), companyDto.getId());
        assertEquals(company.getOwnerId(), companyDto.getOwnerId());
        assertEquals(company.getName(), companyDto.getName());
        assertEquals(company.getWebsite(), companyDto.getWebsite());
        assertEquals(company.getLocation(), companyDto.getLocation());
        assertEquals(company.getDescription(), companyDto.getDescription());
        assertEquals("owner@example.com", companyDto.getEmail());
    }

    @Test
    void getCompany_shouldThrowNotFoundException_whenCompanyDoesNotExist() {
        given(companyRepository.findCompanyByOwnerId("owner-id")).willReturn(Optional.empty());

        Pageable pageable = PageRequest.of(0, 10);
        assertThrows(NotFoundException.class, () -> companyService.getCompany("owner-id"));
    }
}
