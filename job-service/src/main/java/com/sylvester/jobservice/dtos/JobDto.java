package com.sylvester.jobservice.dtos;

import com.sylvester.jobservice.entity.EmploymentType;
import com.sylvester.jobservice.entity.WorkMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobDto {

    private String id;
    private String title;
    private EmploymentType employmentType;
    private WorkMode workMode;
    private String location;
    private String description;
    private String responsibilities;
    private String qualifications;
    private String benefits;
    private String whyJoinUs;
    private LocalDateTime postedAt;

}
