package project.homelearn.controller.student;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import project.homelearn.dto.student.dashboard.*;
import project.homelearn.dto.teacher.board.TeacherBoardDto;
import project.homelearn.dto.teacher.dashboard.ManagerBoardDto;
import project.homelearn.dto.teacher.dashboard.TeacherScheduleDto;
import project.homelearn.service.manager.ManagerBoardService;
import project.homelearn.service.manager.ManagerCalendarService;
import project.homelearn.service.student.StudentBadgeService;
import project.homelearn.service.student.StudentHomeworkService;
import project.homelearn.service.student.StudentLectureService;
import project.homelearn.service.student.StudentQuestionBoardService;
import project.homelearn.service.teacher.TeacherBoardService;
import project.homelearn.service.teacher.TeacherCalendarService;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

/**
 * Author : 김승민
 */
@Slf4j
@RestController
@RequestMapping("/students/dash-boards")
@RequiredArgsConstructor
public class StudentDashBoardController {

    private final ManagerBoardService managerNoticeService;
    private final TeacherBoardService teacherNoticeService;
    private final ManagerCalendarService managerCalendarService;
    private final TeacherCalendarService teacherCalendarService;
    private final StudentQuestionBoardService questionBoardService;
    private final StudentBadgeService studentBadgeService;
    private final StudentHomeworkService studentHomeworkService;
    private final StudentLectureService lectureService;

    // 매니저 공지사항 최신순 4개
    @GetMapping("/manager-boards")
    public List<ManagerBoardDto> viewManagerBoard() {
        return managerNoticeService.viewManagerBoardRecent();
    }

    // 강사 공지사항 최신순 4개
    @GetMapping("/teacher-boards")
    public List<TeacherBoardDto> viewTeacherBoard(){
        return teacherNoticeService.viewTeacherBoardRecent();
    }

    // 매니저가 등록한 일정 중 학생 교육과정에 대한 일정
    @GetMapping("/calendar/manager")
    public List<ViewScheduleDto> viewManagerSchedule(Principal principal) {
        return managerCalendarService.getAllManagerSchedulesOfStudent(principal.getName());
    }

    // 강사가 등록한 일정
    @GetMapping("/calendar/teacher")
    public TeacherScheduleDto viewTeacherSchedule(Principal principal) {
        return teacherCalendarService.getScheduleToStudent(principal.getName());
    }

    // 질문 게시판 최근 2개
    @GetMapping("/questions")
    public ResponseEntity<?> viewQuestionTop2(Principal principal) {

        List<ViewQuestionBoardDto> questionBoards = questionBoardService.getQuestionTop2(principal.getName());

        if (questionBoards != null) {
            return new ResponseEntity<>(questionBoards, HttpStatus.OK);
        }
        else {
            return new ResponseEntity<>("게시된 글이 없습니다.",HttpStatus.NOT_FOUND);
        }
    }

    // 최근 학습 강의
    @GetMapping("/recent-lecture")
    public ResponseEntity<?> viewRecentLecture(Principal principal) {
        Optional<ViewRecentStudyLectureDto> myRecentLecture = lectureService.getRecentLecture(principal.getName());

        if (myRecentLecture.isPresent()) {
            return new ResponseEntity<>(myRecentLecture, HttpStatus.OK);
        }
        else {
            return new ResponseEntity<>("최근 학습한 강의가 없습니다.", HttpStatus.NOT_FOUND);
        }
    }

    // 보충 강의
    @GetMapping("/lectures")
    public Optional<ViewLectureDto> viewLecture(Principal principal) {
        return lectureService.getLecture(principal.getName());
    }

    // 과제 목록
    @GetMapping("/homeworks")
    public ResponseEntity<?> viewHomeworkTop2(Principal principal) {
        List<ViewHomeworkDto> myHomeworks = studentHomeworkService.getHomeworkTop2(principal.getName());
        if (myHomeworks != null) {
            return new ResponseEntity<>(myHomeworks, HttpStatus.OK);
        }
        else {
            return new ResponseEntity<>("과제가 없습니다.",HttpStatus.NOT_FOUND);
        }
    }

    // 배지
    @GetMapping("/badges")
    public ResponseEntity<?> viewMyBadgesTop4(Principal principal) {

        List<ViewMyBadgeDto> myBadges = studentBadgeService.getMyBadgesTop4(principal.getName());

        if (myBadges != null) {
            return new ResponseEntity<>(myBadges, HttpStatus.OK);
        }
        else {
            return new ResponseEntity<>("배지를 보유하고 있지 않습니다.",HttpStatus.NOT_FOUND);
        }
    }
}