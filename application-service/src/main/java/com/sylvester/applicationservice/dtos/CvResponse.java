package com.sylvester.applicationservice.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CvResponse {
    private String name;
    private String contentType;
    private byte[] image;
}
