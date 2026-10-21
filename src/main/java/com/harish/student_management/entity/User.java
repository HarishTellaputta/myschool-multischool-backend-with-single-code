
package com.harish.student_management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "password_reset_token_hash", length = 64)
    private String passwordResetTokenHash;

    @Column(name = "password_reset_expires_at")
    private LocalDateTime passwordResetExpiresAt;

    // Existing primary role — kept for backward compatibility.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;

    // Additional roles for users who have more than one responsibility.
    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_additional_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"),
            uniqueConstraints = @UniqueConstraint(
                    columnNames = {"user_id", "role_id"}
            )
    )
    private Set<Role> additionalRoles = new HashSet<>();

    @Column(length = 20)
    private String status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Returns the primary role and any additional roles without duplicates.
    @Transient
    public Set<String> getAllRoleNames() {
        Set<String> names = new HashSet<>();

        if (role != null && role.getName() != null) {
            names.add(role.getName());
        }

        if (additionalRoles != null) {
            for (Role additionalRole : additionalRoles) {
                if (additionalRole != null && additionalRole.getName() != null) {
                    names.add(additionalRole.getName());
                }
            }
        }

//        if (names.isEmpty()) {
//            // Do not grant a default role when no role is assigned.
//           // names.add("STUDENT");
//        }

        return names;
    }

    public void addAdditionalRole(Role additionalRole) {
        if (additionalRole == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }

        if (additionalRoles == null) {
            additionalRoles = new HashSet<>();
        }

        // Avoid storing the primary role twice.
        if (role == null || !role.getId().equals(additionalRole.getId())) {
            additionalRoles.add(additionalRole);
        }
    }

    public void removeAdditionalRole(Role additionalRole) {
        if (additionalRoles != null && additionalRole != null) {
            additionalRoles.removeIf(existing ->
                    existing.getId() != null
                            && existing.getId().equals(additionalRole.getId()));
        }
    }
}
