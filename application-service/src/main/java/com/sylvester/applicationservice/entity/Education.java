package com.sylvester.applicationservice.entity;


import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Education {
    private String school;
    private String degree;
    private String fieldOfStudy;
    private Integer startYear;
    private Integer endYear;
}
