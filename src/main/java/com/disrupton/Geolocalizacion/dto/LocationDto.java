package com.disrupton.Geolocalizacion.dto;

import lombok.Data;

@Data
public class LocationDto {
    private Double latitude;
    private Double longitude;
    private String department;
    private String district;
    private String street;
    private String city;
    private String country;
    private String postalCode;
    private String fullAddress;
}
