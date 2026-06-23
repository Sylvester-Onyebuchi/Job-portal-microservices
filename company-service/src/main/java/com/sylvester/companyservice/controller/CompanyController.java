package com.sylvester.companyservice.controller;



import com.sylvester.companyservice.dtos.CompanyDto;
import com.sylvester.companyservice.dtos.RegisterRequest;
import com.sylvester.companyservice.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/companies")
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping("/create")
    public ResponseEntity<?> createCompany(@Valid @RequestBody RegisterRequest request, @AuthenticationPrincipal Jwt jwt) {
        String ownerId = jwt.getSubject();
        companyService.createCompany(request, ownerId);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("/company/owner")
    public ResponseEntity<CompanyDto> getCompany(@AuthenticationPrincipal Jwt jwt) {
        String ownerId = jwt.getSubject();
        String email = jwt.getClaim("email");
        return new ResponseEntity<>(companyService.getCompany(ownerId), HttpStatus.OK);
    }


    @PutMapping("/company/update")
    public ResponseEntity<?> updateCompany(@Valid @RequestBody RegisterRequest request, @AuthenticationPrincipal Jwt jwt) {
        String ownerId = jwt.getSubject();
        companyService.updateCompany(request, ownerId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/company/delete/{id}")
    public ResponseEntity<?> deleteCompany(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        String ownerId = jwt.getClaim("userId");
        companyService.deleteCompany(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
