package com.sylvester.companyservice.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "company",indexes = {
        @Index(name = "idx_company_name", columnList = "name")
})
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String ownerId;
    @Column(unique = true)
    private String name;
    @Column(unique = true)
    private String email;
    private String industry;
    private String location;
    @Column(unique = true)
    private String phone;
    @Column(unique = true)
    private String website;
    private String description;
    private LocalDateTime createdAt;
}
