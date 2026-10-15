package com.harish.student_management.controller;

import com.harish.student_management.dto.*;
import com.harish.student_management.entity.*;
import com.harish.student_management.repository.*;
import com.harish.student_management.service.TransportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/transport")
@RequiredArgsConstructor
public class TransportController {
    private final BusRepository buses;
    private final BusDriverRepository drivers;
    private final BusRouteRepository routes;
    private final PickupPointRepository points;
    private final TransportService service;

    @PostMapping("/buses")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public Bus bus(@Valid @RequestBody TransportDtos.BusRequest r) {
        return buses.save(Bus.builder().registrationNumber(r.registrationNumber).model(r.model).capacity(r.capacity).status(r.status == null ? "ACTIVE" : r.status).build());
    }

    @PostMapping("/drivers")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public BusDriver driver(@Valid @RequestBody TransportDtos.DriverRequest r) {
        return drivers.save(BusDriver.builder().name(r.name).licenseNumber(r.licenseNumber).phone(r.phone).status(r.status == null ? "ACTIVE" : r.status).build());
    }

    @PostMapping("/routes")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public BusRoute route(@Valid @RequestBody TransportDtos.RouteRequest r) {
        return routes.save(BusRoute.builder().name(r.name).bus(r.busId == null ? null : buses.findById(r.busId).orElseThrow()).driver(r.driverId == null ? null : drivers.findById(r.driverId).orElseThrow()).status(r.status == null ? "ACTIVE" : r.status).build());
    }

    @PostMapping("/points")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PickupPoint point(@Valid @RequestBody TransportDtos.PointRequest r) {
        return points.save(PickupPoint.builder().route(routes.findById(r.routeId).orElseThrow()).name(r.name).address(r.address).sequence(r.sequence).pointType(r.pointType).build());
    }

    @PostMapping("/allocations")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public TransportResponseDto allocate(@Valid @RequestBody TransportDtos.AllocationRequest r) {
        return service.allocate(r);
    }

    @GetMapping("/students/{studentId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PARENT')")
    public List<TransportResponseDto> child(@PathVariable Long studentId) {
        return service.byStudent(studentId);
    }

    @GetMapping("/allocations")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public List<TransportResponseDto> all() {
        return service.all();
    }

    @GetMapping("/buses")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public List<Bus> buses() {
        return buses.findAll();
    }

    @GetMapping("/routes")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PARENT')")
    public List<BusRoute> routes() {
        return routes.findAll();
    }
}
