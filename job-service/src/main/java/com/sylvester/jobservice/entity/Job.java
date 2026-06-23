package com.sylvester.jobservice.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "jobs", indexes = {
        @Index(name = "idx_job_name", columnList = "title")
})
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String companyId;

    private String title;

    private String location;

    @Enumerated(EnumType.STRING)
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    private WorkMode workMode;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String responsibilities;

    @Column(columnDefinition = "TEXT")
    private String qualifications;

    @Column(columnDefinition = "TEXT")
    private String benefits;

    @Column(columnDefinition = "TEXT")
    private String whyJoinUs;

    private LocalDateTime postedAt;

    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

}
