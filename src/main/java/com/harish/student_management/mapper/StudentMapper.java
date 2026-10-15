package com.harish.student_management.mapper;

import com.harish.student_management.dto.StudentRequestDto;
import com.harish.student_management.dto.StudentResponseDto;
import com.harish.student_management.entity.AcademicYear;
import com.harish.student_management.entity.Parent;
import com.harish.student_management.entity.Section;
import com.harish.student_management.entity.Student;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StudentMapper {

    @Mapping(source = "sectionId", target = "section")
    @Mapping(source = "parentId", target = "parent")
    @Mapping(source = "academicYearId", target = "academicYear")
    Student toEntity(StudentRequestDto dto);

    @Mapping(source = "section.id", target = "sectionId")
    @Mapping(source = "section.name", target = "sectionName")

    @Mapping(source = "section.classEntity.id", target = "classId")
    @Mapping(source = "section.classEntity.name", target = "className")

    @Mapping(source = "parent.id", target = "parentId")
    @Mapping(source = "parent.fatherName", target = "parentName")
    @Mapping(source = "academicYear.id", target = "academicYearId")
    @Mapping(source = "academicYear.name", target = "academicYearName")
    @Mapping(source = "rollNumber", target = "rollNumber")
    StudentResponseDto toResponseDto(Student student);

    List<StudentResponseDto> toResponseDtoList(List<Student> students);

    default Section mapSectionIdToSection(Long sectionId) {
        if (sectionId == null) return null;

        Section section = new Section();
        section.setId(sectionId);
        return section;
    }

    default Parent mapParentIdToParent(Long parentId) {
        if (parentId == null) return null;

        Parent parent = new Parent();
        parent.setId(parentId);
        return parent;
    }

    default AcademicYear mapAcademicYearIdToAcademicYear(Long academicYearId) {
        if (academicYearId == null) return null;

        AcademicYear academicYear = new AcademicYear();
        academicYear.setId(academicYearId);
        return academicYear;
    }
}