package com.harish.student_management.dto; import lombok.*; import java.time.*; @Data @Builder public class JourneyEventDto { LocalTime time; String type,title,description,source; }
