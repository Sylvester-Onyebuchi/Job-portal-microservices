package com.sylvester.applicationservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String jobId;

    private String candidateUserId;

    private String companyId;

    private String fullName;

    @Column(unique = true)
    private String email;

    private String phoneNumber;

    @Column(columnDefinition = "TEXT")
    private String coverLetter;

    private String portfolioUrl;

    private String linkedInUrl;

    private String githubUrl;

    @ElementCollection
    @CollectionTable(
            name = "application_education",
            joinColumns = @JoinColumn(name = "application_id")
    )
    private Set<Education> education = new HashSet<>();

    @ElementCollection
    @CollectionTable(
            name = "application_experience",
            joinColumns = @JoinColumn(name = "application_id")
    )
    private Set<Experience> experience;

    @ElementCollection
    private Set<String> skills  = new HashSet<>();

    @ElementCollection
    private Set<String> languages = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    @Lob
    @JsonIgnore
    private byte[] cv;
    private String cvFilename;
    private String cvContent;

    private LocalDateTime appliedAt;

}



