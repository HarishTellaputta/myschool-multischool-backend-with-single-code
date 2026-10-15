
package com.harish.student_management.service;

import com.harish.student_management.dto.GroupChatMessageRequest;
import com.harish.student_management.dto.GroupChatMessageResponse;
import com.harish.student_management.dto.GroupChatReactionRequest;

import java.util.List;

public interface GroupChatService {

    List<GroupChatMessageResponse> getMessages();

    GroupChatMessageResponse sendMessage(
            GroupChatMessageRequest request
    );

    GroupChatMessageResponse editMessage(
            Long messageId,
            GroupChatMessageRequest request
    );

    void deleteMessage(Long messageId);

    GroupChatMessageResponse addReaction(
            Long messageId,
            GroupChatReactionRequest request
    );

    void removeReaction(
            Long messageId,
            GroupChatReactionRequest request
    );
}
