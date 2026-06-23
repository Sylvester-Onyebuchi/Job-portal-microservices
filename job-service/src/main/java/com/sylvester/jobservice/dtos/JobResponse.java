package com.sylvester.jobservice.dtos;

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
    private String jobName;
    private String companyName;
    private String companyOwnerEmail;
    private LocalDateTime postedAt;
}
