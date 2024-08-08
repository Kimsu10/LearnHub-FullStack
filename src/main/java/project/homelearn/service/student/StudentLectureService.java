package project.homelearn.service.student;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.homelearn.dto.student.dashboard.ViewLectureDto;
import project.homelearn.dto.student.dashboard.ViewRecentStudyLectureDto;
import project.homelearn.dto.student.lecture.StudentLectureViewDto;
import project.homelearn.dto.teacher.lecture.LectureListDto;
import project.homelearn.dto.teacher.lecture.TeacherLectureViewDto;
import project.homelearn.entity.curriculum.Curriculum;
import project.homelearn.repository.curriculum.CurriculumRepository;
import project.homelearn.repository.curriculum.LectureRepository;
import project.homelearn.repository.curriculum.StudentLectureRepository;

import java.util.Optional;

/**
 * Author : 김승민
 */
@Slf4j
@Transactional
@Service
@RequiredArgsConstructor
public class StudentLectureService {

    private final LectureRepository lectureRepository;
    private final StudentLectureRepository myLectureRepository;
    private final CurriculumRepository curriculumRepository;

    // 최근 영상 or 최근 영상 중 안본 영상
    public Optional<ViewLectureDto> getLecture(String username){
        return lectureRepository.findLatestUnwatchedOrRecentLecture(username);
    }

    // 최근 내가 학습한 강의 상태 보기
    public Optional<ViewRecentStudyLectureDto> getRecentLecture(String username){
        return myLectureRepository.findRecentStudyLectureByUsername(username);
    }

    // 강의 영상 15개 페이지 리스트
    public Page<LectureListDto> getLectureList(String username, int page, int size, Long subjectId) {
        PageRequest pageRequest = PageRequest.of(page, size);
        if (subjectId != null) {
            return lectureRepository.findSubjectLecturePage(subjectId, pageRequest);
        }

        Curriculum curriculum = curriculumRepository.findCurriculumByStudent(username);
        return lectureRepository.findLecturePage(curriculum, pageRequest);
    }

    // 사이즈 6개 강의 영상 페이지 리스트
    public Page<LectureListDto> getLectureList(String username, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);

        Curriculum curriculum = curriculumRepository.findCurriculumByTeacher(username);
        return lectureRepository.findLecturePage(curriculum, pageRequest);
    }

    // 강의 상세보기 페이지
    public StudentLectureViewDto getLecture(Long lectureId) {
        return lectureRepository.findStudentLectureView(lectureId);
    }

}
