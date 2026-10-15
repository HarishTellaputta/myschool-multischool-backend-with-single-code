package com.harish.student_management.repository;

import com.harish.student_management.entity.TeacherTimetable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

@Repository
public interface TeacherTimetableRepository extends JpaRepository<TeacherTimetable, Long> {

    List<TeacherTimetable> findByTeacherId(Long teacherId);

    List<TeacherTimetable> findByTeacherIdAndDayOfWeek(Long teacherId, DayOfWeek dayOfWeek);

    @Query("select t from TeacherTimetable t where t.classEntity.id = :classId")
    List<TeacherTimetable> findByClassId(@Param("classId") Long classId);


    void deleteByTeacherId(Long teacherId);

    @Query("""
    select t
    from TeacherTimetable t
    where t.teacher.id = :teacherId
      and t.dayOfWeek = :dayOfWeek
      and t.startTime < :endTime
      and t.endTime > :startTime
""")
    List<TeacherTimetable> findTeacherConflicts(
            @Param("teacherId") Long teacherId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("endTime") java.time.LocalTime endTime
    );

    @Query("""
    select t
    from TeacherTimetable t
    where t.classEntity.id = :classId
      and (
          (:sectionId is null and t.section is null)
          or
          (:sectionId is not null and t.section.id = :sectionId)
      )
      and t.dayOfWeek = :dayOfWeek
      and t.startTime < :endTime
      and t.endTime > :startTime
""")
    List<TeacherTimetable> findClassConflicts(
            @Param("classId") Long classId,
            @Param("sectionId") Long sectionId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("endTime") java.time.LocalTime endTime
    );
}
