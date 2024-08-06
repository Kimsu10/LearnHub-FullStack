package project.homelearn.repository.curriculum.querydsl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import project.homelearn.dto.teacher.lecture.LectureListDto;
import project.homelearn.dto.teacher.lecture.TeacherLectureViewDto;
import project.homelearn.dto.teacher.subject.SubjectBoardListDto;
import project.homelearn.entity.curriculum.Curriculum;

public interface LectureRepositoryCustom {

    Page<LectureListDto> findSubjectLecturePage(Long subjectId, Pageable pageable);

    Page<LectureListDto> findLecturePage(Curriculum curriculum, Pageable pageable);

    Page<SubjectBoardListDto> findSubjectBoardPage(Long subjectId, Pageable pageable, String teacherName);

    TeacherLectureViewDto findTeacherLectureView(Long lectureId);
}