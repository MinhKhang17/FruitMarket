package com.example.fruitmarket.controller;

import com.example.fruitmarket.repository.DistrictRepo;
import com.example.fruitmarket.repository.ProvinceRepo;
import com.example.fruitmarket.repository.WardRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/geo")
public class GeoRestController {
    private final ProvinceRepo provinceRepo;
    private final DistrictRepo districtRepo;
    private final WardRepo wardRepo;
    private final GeoJsonLoader json; // <— inject loader

    @GetMapping("/provinces")
    public ResponseEntity<?> provinces() {
        if (provinceRepo.count() > 0) {
            return ResponseEntity.ok(provinceRepo.findAll()
                    .stream().map(p -> new Simple(p.getProvinceId(), p.getProvinceName())).toList());
        }
        // Fallback JSON
        return ResponseEntity.ok(json.getProvinces());
    }

    @GetMapping("/districts")
    public ResponseEntity<?> districts(@RequestParam Integer provinceId) {
        if (districtRepo.count() > 0 && provinceRepo.existsById(provinceId)) {
            var province = provinceRepo.findById(provinceId).orElseThrow();
            return ResponseEntity.ok(districtRepo.findByProvinceOrderByDistrictNameAsc(province)
                    .stream().map(d -> new Simple(d.getDistrictId(), d.getDistrictName())).toList());
        }
        // Fallback JSON: lọc theo provinceId
        var list = json.getDistricts().stream()
                .filter(d -> d.provinceId()!=null && d.provinceId().equals(provinceId))
                .map(d -> new Simple(d.id(), d.name()))
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/wards")
    public ResponseEntity<?> wards(@RequestParam Integer districtId) {
        if (wardRepo.count() > 0 && districtRepo.existsById(districtId)) {
            var district = districtRepo.findById(districtId).orElseThrow();
            return ResponseEntity.ok(wardRepo.findByDistrictOrderByWardNameAsc(district)
                    .stream().map(w -> new Simple(w.getWardCode(), w.getWardName())).toList());
        }
        // Fallback JSON: lọc theo districtId
        var list = json.getWards().stream()
                .filter(w -> w.districtId()!=null && w.districtId().equals(districtId))
                .map(w -> new Simple(w.id(), w.name()))
                .toList();
        return ResponseEntity.ok(list);
    }

    record Simple(Object id, String name) {}
}