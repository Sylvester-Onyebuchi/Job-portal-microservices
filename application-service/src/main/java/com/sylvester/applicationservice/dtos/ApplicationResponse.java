package com.sylvester.applicationservice.dtos;


import com.sylvester.applicationservice.entity.Education;
import com.sylvester.applicationservice.entity.Experience;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationResponse {
    private String id;
    private String candidateUserId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String githubLink;
    private Set<Experience> experience;
    private Set<Education> education;
}
