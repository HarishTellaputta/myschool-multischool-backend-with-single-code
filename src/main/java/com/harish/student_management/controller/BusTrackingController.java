package com.harish.student_management.controller;

import com.harish.student_management.dto.BusTrackingDtos.*;
import com.harish.student_management.service.BusTrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/transport/tracking")
@RequiredArgsConstructor
public class BusTrackingController {
    private final BusTrackingService service;

    @PostMapping("/trips")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<TrackingResponse> start(@Valid @RequestBody TripRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.start(r));
    }

    @PostMapping("/trips/{tripId}/location")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','TEACHER')")
    public TrackingResponse location(@PathVariable Long tripId, @RequestParam Long driverId, @Valid @RequestBody LocationRequest r) {
        return service.location(tripId, driverId, r);
    }

    @GetMapping("/buses/{busId}/current")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PARENT','TEACHER','STUDENT')")
    public TrackingResponse current(@PathVariable Long busId) {
        return service.current(busId);
    }

    @PostMapping("/trips/{tripId}/events")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','TEACHER')")
    public TrackingResponse event(@PathVariable Long tripId, @RequestParam Long driverId, @Valid @RequestBody EventRequest r) {
        return service.event(tripId, driverId, r);
    }

    @GetMapping("/trips/{tripId}/events")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PARENT','TEACHER','STUDENT')")
    public List<TrackingResponse> events(@PathVariable Long tripId) {
        return service.events(tripId);
    }
}
