        package com.harish.student_management.dto;

        import lombok.AllArgsConstructor;
        import lombok.Builder;
        import lombok.Data;
        import lombok.NoArgsConstructor;

        import java.time.LocalDateTime;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public class ExamResultResponseDto {

            private Long id;
            private Long examScheduleId;
            private Long studentId;
            private String studentName;
            private String studentRollNumber;
            private Long markedByTeacherId;
            private String markedByTeacherName;
            private Integer marksObtained;
            private Integer maxMarks;
            private Double percentage;
            private String grade;
            private Integer classRank;
            private Integer sectionRank;
            private String remarks;
            private String status;
            private Boolean isPublished;
            private LocalDateTime markedAt;
            private LocalDateTime publishedAt;
            private LocalDateTime createdAt;
            private LocalDateTime updatedAt;
        }
