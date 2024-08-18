package project.homelearn.repository.homework.querydsl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import project.homelearn.dto.student.dashboard.ViewHomeworkDto;
import project.homelearn.dto.teacher.dashboard.HomeworkStateDto;
import project.homelearn.dto.teacher.homework.HomeworkDetailDto;
import project.homelearn.dto.teacher.homework.HomeworkSubmitListDto;
import project.homelearn.dto.teacher.homework.HomeworkTabDto;
import project.homelearn.entity.curriculum.Curriculum;
import project.homelearn.entity.user.User;

import java.util.List;

public interface HomeworkRepositoryCustom {

    HomeworkStateDto findHomeworkStateDto(Curriculum curriculum, Integer totalCount);

    Page<HomeworkTabDto> findHomeworks(Curriculum curriculum, Pageable pageable, String status);

    Long findCompletedCount(Long homeworkId);

    HomeworkDetailDto findHomeworkDetail(Long homeworkId, Long unsubmittedCount, Curriculum curriculum);

    List<Long> findSubmitStudentIds(Long homeworkId);

    List<HomeworkSubmitListDto> findHomeworkSubmitList(Long homeworkId);

    List<ViewHomeworkDto> findHomeworkTop2(Curriculum curriculum, User student);
}