package project.homelearn.repository.homework;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.homelearn.entity.homework.Homework;
import project.homelearn.entity.homework.StudentHomework;
import project.homelearn.entity.student.Student;
import project.homelearn.entity.user.User;

import java.util.List;

public interface StudentHomeworkRepository extends JpaRepository<StudentHomework, Long> {

    @Query("SELECT COUNT(sh) > 0 FROM StudentHomework sh WHERE sh.user =:student AND sh.homework =:homework")
    boolean existsByStudentAndHomework(@Param("student") Student student, @Param("homework") Homework homework);

    @Query("SELECT COUNT(sh) > 0 FROM StudentHomework sh WHERE sh.user =:student AND sh.homework.id =:homeworkId")
    boolean existsByStudentAndHomeworkId(@Param("student") User student, @Param("homeworkId") Long homeworkId);

    @Query("select sh from StudentHomework sh where sh.homework.id =:homeworkId")
    StudentHomework findByHomeworkId(@Param("homeworkId") Long homeworkId);

    long countByHomework(Homework homework);

    @Query("select sh from StudentHomework sh join fetch sh.user where sh.homework =:homework")
    List<StudentHomework> findAllByHomework(@Param("homework") Homework homework);
}