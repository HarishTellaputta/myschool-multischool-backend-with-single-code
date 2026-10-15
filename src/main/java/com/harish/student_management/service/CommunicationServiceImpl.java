package com.harish.student_management.service;

import com.harish.student_management.dto.*;
import com.harish.student_management.entity.*;
import com.harish.student_management.exception.ResourceNotFoundException;
import com.harish.student_management.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CommunicationServiceImpl
        implements CommunicationService {

    private final CommunicationConversationRepository conversations;

    private final CommunicationMessageRepository messages;

    private final ParentRepository parents;

    private final TeacherRepository teachers;

    private final StudentRepository students;

    private final TeacherClassAssignmentRepository assignments;

    private final UserRepository users;


    /*
     * ============================================================
     * START CONVERSATION
     *
     * Supports:
     *
     * PARENT  -> TEACHER
     * TEACHER -> PARENT
     * ============================================================
     */
    @Override
    public ConversationResponseDto initiate(
            ConversationRequestDto r,
            String username
    ) {

        String role = role(username);

        User u = user(username);

        Student s = students.findById(r.getStudentId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Student not found"
                        )
                );


        /*
         * ========================================================
         * TEACHER STARTS CHAT
         * ========================================================
         */
        if ("TEACHER".equals(role)) {

            /*
             * Get Teacher domain object from authenticated User.
             */
            Teacher t = teachers.findByUserId(u.getId())
                    .orElseThrow(() ->
                            new SecurityException(
                                    "Teacher identity is not linked to authenticated user"
                            )
                    );


            /*
             * Student must have parent.
             */
            if (s.getParent() == null) {

                throw new SecurityException(
                        "This student does not have a parent linked"
                );
            }


            /*
             * Parent is automatically obtained
             * from selected student.
             */
            Parent p = s.getParent();


            /*
             * SECURITY:
             *
             * Teacher must be assigned to student's
             * class / subject.
             */
            authorize(
                    t,
                    s,
                    r.getSubject()
            );


            /*
             * Find existing conversation.
             *
             * If already exists, reuse it.
             */
            CommunicationConversation c =
                    conversations
                            .findByParentIdAndTeacherIdAndStudentIdAndSubject(
                                    p.getId(),
                                    t.getId(),
                                    s.getId(),
                                    r.getSubject()
                            )
                            .orElseGet(() ->
                                    conversations.save(
                                            CommunicationConversation
                                                    .builder()
                                                    .parent(p)
                                                    .teacher(t)
                                                    .student(s)
                                                    .subject(r.getSubject())
                                                    .build()
                                    )
                            );


            /*
             * Save first message as TEACHER.
             */
            messages.save(
                    CommunicationMessage
                            .builder()
                            .conversation(c)
                            .content(r.getInitialMessage())
                            .messageType("TEXT")
                            .senderRole("TEACHER")
                            .read(false)
                            .build()
            );


            return map(c);
        }


        /*
         * ========================================================
         * PARENT STARTS CHAT
         * ========================================================
         */

        Parent p = parents.findByUserId(u.getId())
                .orElseThrow(() ->
                        new SecurityException(
                                "Parent identity is not linked to authenticated user"
                        )
                );


        /*
         * Validate parentId if provided.
         */
        if (r.getParentId() != null &&
                !r.getParentId().equals(p.getId())) {

            throw new SecurityException(
                    "Authenticated user is not this parent"
            );
        }


        /*
         * Parent must provide teacherId.
         */
        if (r.getTeacherId() == null) {

            throw new IllegalArgumentException(
                    "Teacher ID is required when parent starts a conversation"
            );
        }


        Teacher t = teachers.findById(r.getTeacherId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Teacher not found"
                        )
                );


        /*
         * Student must belong to parent.
         */
        if (s.getParent() == null ||
                !s.getParent().getId().equals(p.getId())) {

            throw new SecurityException(
                    "Student is not a child of this parent"
            );
        }


        /*
         * Teacher must be assigned to student's class/subject.
         */
        authorize(
                t,
                s,
                r.getSubject()
        );


        CommunicationConversation c =
                conversations
                        .findByParentIdAndTeacherIdAndStudentIdAndSubject(
                                p.getId(),
                                t.getId(),
                                s.getId(),
                                r.getSubject()
                        )
                        .orElseGet(() ->
                                conversations.save(
                                        CommunicationConversation
                                                .builder()
                                                .parent(p)
                                                .teacher(t)
                                                .student(s)
                                                .subject(r.getSubject())
                                                .build()
                                )
                        );


        /*
         * Save first message as PARENT.
         */
        messages.save(
                CommunicationMessage
                        .builder()
                        .conversation(c)
                        .content(r.getInitialMessage())
                        .messageType("TEXT")
                        .senderRole("PARENT")
                        .read(false)
                        .build()
        );


        return map(c);
    }


    /*
     * ============================================================
     * GET TEACHER'S STUDENTS
     *
     * Only students belonging to classes assigned
     * to authenticated teacher are returned.
     * ============================================================
     */
    @Override
    public List<StudentChatResponseDto> getTeacherStudents(
            String username
    ) {

        /*
         * Get authenticated User.
         */
        User u = user(username);


        /*
         * Convert User -> Teacher.
         */
        Teacher teacher = teachers.findByUserId(u.getId())
                .orElseThrow(() ->
                        new SecurityException(
                                "Teacher identity is not linked to authenticated user"
                        )
                );


        /*
         * Get all assignments for teacher.
         */
        List<TeacherClassAssignment> teacherAssignments =
                assignments.findByTeacherId(
                        teacher.getId()
                );


        /*
         * Get class IDs assigned to teacher.
         */
        Set<Long> assignedClassIds =
                teacherAssignments
                        .stream()
                        .filter(a ->
                                a.getClassEntity() != null
                        )
                        .map(a ->
                                a.getClassEntity().getId()
                        )
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());


        /*
         * If teacher has no class assignment,
         * return empty list.
         */
        if (assignedClassIds.isEmpty()) {
            return List.of();
        }


        /*
         * Get students and keep only students
         * belonging to teacher's assigned classes.
         */
        return students.findAll()
                .stream()
                .filter(s ->
                        s.getSection() != null
                )
                .filter(s ->
                        s.getSection().getClassEntity() != null
                )
                .filter(s ->
                        assignedClassIds.contains(
                                s.getSection()
                                        .getClassEntity()
                                        .getId()
                        )
                )
                .filter(s ->
                        s.getParent() != null
                )
                .map(this::mapStudentForChat)
                .toList();
    }


    /*
     * ============================================================
     * MAP STUDENT FOR CHAT
     * ============================================================
     */
    private StudentChatResponseDto mapStudentForChat(
            Student s
    ) {

        Long classId = null;
        String className = null;

        Long sectionId = null;
        String sectionName = null;


        if (s.getSection() != null) {

            sectionId = s.getSection().getId();

            sectionName = s.getSection().getName();

            if (s.getSection().getClassEntity() != null) {

                classId =
                        s.getSection()
                                .getClassEntity()
                                .getId();

                className =
                        s.getSection()
                                .getClassEntity()
                                .getName();
            }
        }


        Parent parent = s.getParent();


        return StudentChatResponseDto
                .builder()
                .studentId(s.getId())
                .studentName(s.getName())
                .parentId(parent.getId())
                .parentName(parent.getFatherName())
                .classId(classId)
                .className(className)
                .sectionId(sectionId)
                .sectionName(sectionName)
                .build();
    }


    /*
     * ============================================================
     * SEND MESSAGE
     * ============================================================
     */
    @Override
    public MessageResponseDto reply(
            Long id,
            String username,
            MessageRequestDto r
    ) {

        CommunicationConversation c = find(id);

        String role = role(username);

        Long actor = actorId(
                username,
                role
        );


        boolean notParticipant =
                ("PARENT".equals(role)
                        && !c.getParent()
                        .getId()
                        .equals(actor))
                        ||
                        ("TEACHER".equals(role)
                                && !c.getTeacher()
                                .getId()
                                .equals(actor));


        if (notParticipant) {

            throw new SecurityException(
                    "Not your conversation"
            );
        }


        return map(
                messages.save(
                        CommunicationMessage
                                .builder()
                                .conversation(c)
                                .content(r.getContent())
                                .messageType(
                                        r.getMessageType() == null
                                                ? "TEXT"
                                                : r.getMessageType()
                                                .toUpperCase()
                                )
                                .senderRole(role)
                                .read(false)
                                .build()
                )
        );
    }


    /*
     * ============================================================
     * GET CONVERSATION HISTORY
     * ============================================================
     */
    @Override
    public ConversationResponseDto history(
            Long id,
            String username
    ) {

        CommunicationConversation c = find(id);

        String role = role(username);

        Long actor = actorId(
                username,
                role
        );


        authorizeParticipant(
                c,
                actor,
                role
        );


        /*
         * Mark messages from opposite role as read.
         */
        messages
                .findByConversationIdOrderByCreatedAtAsc(id)
                .stream()
                .filter(m ->
                        !m.getSenderRole()
                                .equals(role)
                )
                .forEach(m ->
                        m.setRead(true)
                );


        return map(c);
    }


    /*
     * ============================================================
     * GET MY CONVERSATIONS
     * ============================================================
     */
    @Override
    public List<ConversationResponseDto> conversations(
            String username
    ) {

        String role = role(username);

        Long id = actorId(
                username,
                role
        );


        List<CommunicationConversation> cs;


        if ("PARENT".equals(role)) {

            cs = conversations
                    .findByParentIdOrderByCreatedAtDesc(id);

        } else {

            cs = conversations
                    .findByTeacherIdOrderByCreatedAtDesc(id);
        }


        return cs
                .stream()
                .map(this::map)
                .toList();
    }


    /*
     * ============================================================
     * FIND USER
     * ============================================================
     */
    private User user(String username) {

        return users
                .findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );
    }


    /*
     * ============================================================
     * GET ROLE
     * ============================================================
     */
    private String role(String username) {

        String r =
                user(username)
                        .getRole()
                        .getName()
                        .toUpperCase();


        if (!Set.of("PARENT", "TEACHER").contains(r)) {

            throw new SecurityException(
                    "Only parent or teacher may communicate"
            );
        }


        return r;
    }


    /*
     * ============================================================
     * GET DOMAIN ACTOR ID
     *
     * User ID -> Parent ID / Teacher ID
     * ============================================================
     */
    private Long actorId(
            String username,
            String role
    ) {

        User u = user(username);


        if ("PARENT".equals(role)) {

            return parents
                    .findByUserId(u.getId())
                    .map(Parent::getId)
                    .orElseThrow(() ->
                            new SecurityException(
                                    "Parent identity is not linked to authenticated user"
                            )
                    );
        }


        return teachers
                .findByUserId(u.getId())
                .map(Teacher::getId)
                .orElseThrow(() ->
                        new SecurityException(
                                "Teacher identity is not linked to authenticated user"
                        )
                );
    }


    /*
     * ============================================================
     * TEACHER -> STUDENT AUTHORIZATION
     * ============================================================
     */
    private void authorize(
            Teacher t,
            Student s,
            String subject
    ) {

        if (s.getSection() == null) {

            throw new SecurityException(
                    "Teacher is not assigned to this child's class or subject"
            );
        }


        if (s.getSection().getClassEntity() == null) {

            throw new SecurityException(
                    "Student class is not available"
            );
        }


        Long studentClassId =
                s.getSection()
                        .getClassEntity()
                        .getId();


        boolean authorized =
                assignments
                        .findByTeacherId(t.getId())
                        .stream()
                        .anyMatch(a ->

                                a.getClassEntity() != null

                                        &&

                                        a.getClassEntity()
                                                .getId()
                                                .equals(studentClassId)

                                        &&

                                        (
                                                subject == null

                                                        ||

                                                        subject.isBlank()

                                                        ||

                                                        (
                                                                a.getSubject() != null

                                                                        &&

                                                                        a.getSubject()
                                                                                .getName() != null

                                                                        &&

                                                                        subject.equalsIgnoreCase(
                                                                                a.getSubject()
                                                                                        .getName()
                                                                        )
                                                        )
                                        )
                        );


        if (!authorized) {

            throw new SecurityException(
                    "Teacher is not assigned to this child's class or subject"
            );
        }
    }


    /*
     * ============================================================
     * PARTICIPANT AUTHORIZATION
     * ============================================================
     */
    private void authorizeParticipant(
            CommunicationConversation c,
            Long id,
            String role
    ) {

        boolean notParticipant =
                ("PARENT".equals(role)
                        && !c.getParent()
                        .getId()
                        .equals(id))
                        ||
                        ("TEACHER".equals(role)
                                && !c.getTeacher()
                                .getId()
                                .equals(id));


        if (notParticipant) {

            throw new SecurityException(
                    "Not a participant"
            );
        }
    }


    /*
     * ============================================================
     * FIND CONVERSATION
     * ============================================================
     */
    private CommunicationConversation find(
            Long id
    ) {

        return conversations
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Conversation not found"
                        )
                );
    }


    /*
     * ============================================================
     * MAP MESSAGE
     * ============================================================
     */
    private MessageResponseDto map(
            CommunicationMessage m
    ) {

        return MessageResponseDto
                .builder()
                .id(m.getId())
                .conversationId(
                        m.getConversation().getId()
                )
                .content(m.getContent())
                .messageType(m.getMessageType())
                .senderRole(m.getSenderRole())
                .read(m.getRead())
                .createdAt(m.getCreatedAt())
                .build();
    }


    /*
     * ============================================================
     * MAP CONVERSATION
     * ============================================================
     */
    private ConversationResponseDto map(
            CommunicationConversation c
    ) {

        /*
         * For teacher:
         *
         * unread = messages sent by parent
         */
        long unreadCount =
                messages
                        .countByConversationIdAndReadFalseAndSenderRole(
                                c.getId(),
                                "PARENT"
                        );


        return ConversationResponseDto
                .builder()
                .id(c.getId())
                .parentId(
                        c.getParent().getId()
                )
                .teacherId(
                        c.getTeacher().getId()
                )
                .teacherName(
                        c.getTeacher().getName()
                )
                .studentId(
                        c.getStudent().getId()
                )
                .studentName(
                        c.getStudent().getName()
                )
                .subject(
                        c.getSubject()
                )
                .createdAt(
                        c.getCreatedAt()
                )
                .messages(
                        messages
                                .findByConversationIdOrderByCreatedAtAsc(
                                        c.getId()
                                )
                                .stream()
                                .map(this::map)
                                .toList()
                )
                .unreadCount(
                        unreadCount
                )
                .build();
    }

    private String getParentDisplayName(Parent parent) {
        if (parent == null) {
            return null;
        }

        if (parent.getFatherName() != null && !parent.getFatherName().isBlank()) {
            return parent.getFatherName();
        }

        if (parent.getMotherName() != null && !parent.getMotherName().isBlank()) {
            return parent.getMotherName();
        }

        if (parent.getGuardianName() != null && !parent.getGuardianName().isBlank()) {
            return parent.getGuardianName();
        }

        return "Parent";
    }
}