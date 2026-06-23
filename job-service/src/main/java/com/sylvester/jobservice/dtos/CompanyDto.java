package com.sylvester.jobservice.dtos;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompanyDto {
    private String id;
    private String ownerId;
    private String name;
    private String email;
    private String website;
    private String location;
    private String description;
}
