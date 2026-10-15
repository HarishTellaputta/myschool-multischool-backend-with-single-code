package com.harish.student_management.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bus_drivers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusDriver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String driverId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 30)
    private String licenseNumber;

    @Column(length = 20)
    private String phone;

    @Column(length = 20)
    private String status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;
}
