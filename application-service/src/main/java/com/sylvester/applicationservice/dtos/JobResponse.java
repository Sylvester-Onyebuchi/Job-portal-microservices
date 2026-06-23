package com.sylvester.applicationservice.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobResponse {
    private String jobId;
    private String companyId;
    private String companyName;
    private String companyOwnerEmail;
    private String jobName;
    private LocalDateTime postedAt;
}
