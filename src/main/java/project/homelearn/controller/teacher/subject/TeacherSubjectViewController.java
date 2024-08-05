package project.homelearn.controller.teacher.subject;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import project.homelearn.dto.teacher.dashboard.QuestionTop5Dto;
import project.homelearn.dto.teacher.lecture.LectureListDto;
import project.homelearn.dto.teacher.subject.SubjectBasicDto;
import project.homelearn.dto.teacher.subject.SubjectBoardTop5Dto;
import project.homelearn.service.teacher.TeacherSubjectService;

import java.util.List;

/**
 * Author : 정성진
 */
@RestController
@RequestMapping("/teachers/subjects/{subjectId}")
@RequiredArgsConstructor
public class TeacherSubjectViewController {

    private final TeacherSubjectService subjectService;

    /**
     * 특정 과목 상세 페이지
     * 1. 과목 일반 정보 - ✅
     * 2. 과목 게시판 최신 5개 - ✅
     * 3. 질문 게시판 최신 5개 - ✅
     * 4. 과목 강의 영상 페이징 (size = 6) - ✅
     */
    @GetMapping
    public SubjectBasicDto viewSubjectBasic(@PathVariable("subjectId") Long subjectId) {
        return subjectService.getSubjectBasic(subjectId);
    }

    @GetMapping("/boards-recent")
    public List<SubjectBoardTop5Dto> viewSubjectBoardTop5(@PathVariable("subjectId") Long subjectId) {
        return subjectService.getSubjectBoardTop5(subjectId);
    }

    @GetMapping("/questions-recent")
    public List<QuestionTop5Dto> viewQuestionTop5(@PathVariable("subjectId") Long subjectId) {
        return subjectService.getQuestionTop5(subjectId);
    }

    @GetMapping("/lectures")
    public Page<LectureListDto> viewSubjectLecturePage(@PathVariable("subjectId") Long subjectId,
                                                       @RequestParam(name = "page", defaultValue = "0") int page) {
        int size = 6;
        return subjectService.getSubjectLecturePage(subjectId, page, size);
    }
}