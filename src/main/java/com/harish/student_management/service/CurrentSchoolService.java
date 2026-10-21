package com.harish.student_management.service;

import com.harish.student_management.entity.School;
import com.harish.student_management.entity.User;
import com.harish.student_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication
        .AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentSchoolService {

    private final UserRepository userRepository;

    public Long getCurrentSchoolId() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Authenticated user not found"
            );
        }

        User user = userRepository.findByUsername(
                authentication.getName()
        ).orElseThrow(() ->
                new AuthenticationCredentialsNotFoundException(
                        "Authenticated user does not exist"
                )
        );

        if (user.getSchool() == null || user.getSchool().getId() == null) {
            throw new IllegalStateException(
                    "No school is assigned to the authenticated user"
            );
        }

        return user.getSchool().getId();
    }

    public School getCurrentSchool() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Authenticated user not found"
            );
        }

        User user = userRepository.findByUsername(
                authentication.getName()
        ).orElseThrow(() ->
                new AuthenticationCredentialsNotFoundException(
                        "Authenticated user does not exist"
                )
        );

        if (user.getSchool() == null || user.getSchool().getId() == null) {
            throw new IllegalStateException(
                    "No school is assigned to the authenticated user"
            );
        }

        return user.getSchool();
    }
}
