
package com.harish.student_management.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupChatReactionResponse {

    private Long id;
    private Long userId;
    private String userName;
    private String reaction;
    private LocalDateTime createdAt;
}
