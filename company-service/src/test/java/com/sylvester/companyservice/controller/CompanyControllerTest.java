package com.sylvester.companyservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sylvester.companyservice.dtos.CompanyDto;
import com.sylvester.companyservice.dtos.RegisterRequest;
import com.sylvester.companyservice.service.CompanyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CompanyControllerTest {

    @Mock
    private CompanyService companyService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Jwt jwt;
    private RegisterRequest request;

    @BeforeEach
    void setUp() {
        jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("owner-id")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claims(claims -> {
                    claims.put("userId", "owner-id");
                    claims.put("email", "owner@example.com");
                })
                .build();

        request = new RegisterRequest(
                "Acme Inc",
                "Software",
                "https://acme.example.com",
                "Lagos",
                "+2348012345678",
                "contact@acme.example.com",
                "A software company building hiring tools"
        );

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new CompanyController(companyService))
                .setValidator(validator)
                .setCustomArgumentResolvers(new JwtArgumentResolver(jwt))
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void createCompany_shouldReturnCreatedAndCallServiceWithOwnerId() throws Exception {
        mockMvc.perform(post("/api/v1/companies/create")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(companyService).createCompany(request, "owner-id");
    }

    @Test
    void createCompany_shouldReturnBadRequest_whenPayloadIsInvalid() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest(
                "",
                "",
                "",
                "",
                "",
                "invalid-email",
                "too short"
        );

        mockMvc.perform(post("/api/v1/companies/create")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(companyService);
    }

    @Test
    void getCompany_shouldReturnCompanyFromService() throws Exception {
        CompanyDto companyDto = new CompanyDto(
                "company-id",
                "owner-id",
                "Acme Inc",
                "owner@example.com",
                "https://acme.example.com",
                "Lagos",
                "A software company building hiring tools"
        );
        given(companyService.getCompany("owner-id")).willReturn(companyDto);

        mockMvc.perform(get("/api/v1/companies/company/owner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("company-id"))
                .andExpect(jsonPath("$.ownerId").value("owner-id"))
                .andExpect(jsonPath("$.name").value("Acme Inc"))
                .andExpect(jsonPath("$.email").value("owner@example.com"))
                .andExpect(jsonPath("$.website").value("https://acme.example.com"))
                .andExpect(jsonPath("$.location").value("Lagos"))
                .andExpect(jsonPath("$.description").value("A software company building hiring tools"));

        verify(companyService).getCompany("owner-id");
    }

    @Test
    void updateCompany_shouldReturnOkAndCallServiceWithOwnerId() throws Exception {
        mockMvc.perform(put("/api/v1/companies/company/update")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(companyService).updateCompany(request, "owner-id");
    }

    @Test
    void deleteCompany_shouldReturnOkAndCallServiceWithCompanyId() throws Exception {
        mockMvc.perform(delete("/api/v1/companies/company/delete/company-id"))
                .andExpect(status().isOk());

        verify(companyService).deleteCompany("company-id");
    }

    private static class JwtArgumentResolver implements HandlerMethodArgumentResolver {

        private final Jwt jwt;

        private JwtArgumentResolver(Jwt jwt) {
            this.jwt = jwt;
        }

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                    && Jwt.class.isAssignableFrom(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(
                MethodParameter parameter,
                ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest,
                WebDataBinderFactory binderFactory
        ) {
            return jwt;
        }
    }
}
