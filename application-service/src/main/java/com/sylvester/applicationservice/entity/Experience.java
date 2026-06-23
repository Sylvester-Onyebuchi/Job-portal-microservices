package com.sylvester.applicationservice.entity;


import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Experience {
    private String company;
    private String position;
    private String description;
    private Integer startYear;
    private Integer endYear;
    private boolean stillWorking = false;
}