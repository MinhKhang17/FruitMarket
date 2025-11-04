package com.example.fruitmarket.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GeoJsonLoader {

    private final ObjectMapper mapper = new ObjectMapper();

    @Getter private List<Simple> provinces = Collections.emptyList();
    @Getter
    private List<DistrictJson> districts = Collections.emptyList();
    @Getter private List<WardJson> wards = Collections.emptyList();

    @PostConstruct
    public void load() {
        provinces = readList("geo/provinces.json", new TypeReference<>(){});
        districts = readList("geo/districts.json", new TypeReference<>(){});
        wards     = readList("geo/wards.json",     new TypeReference<>(){});
    }

    private <T> List<T> readList(String path, TypeReference<List<T>> type) {
        try (InputStream is = new ClassPathResource(path).getInputStream()) {
            return mapper.readValue(is, type);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // DTO đơn giản cho JSON
    public record Simple(Object id, String name) {}
    public record DistrictJson(Integer id, String name, Integer provinceId) {}
    public record WardJson(String id, String name, Integer districtId) {}
}