package com.harish.student_management.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BirthdayChatReactionResponse {

    private String reaction;

    private long count;

    private boolean reactedByCurrentUser;
}