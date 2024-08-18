package project.homelearn.controller.student.question;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.homelearn.dto.common.board.QuestionBoardCommentDto;
import project.homelearn.dto.common.board.QuestionBoardDetailDto;
import project.homelearn.dto.common.board.QuestionBoardDto;
import project.homelearn.entity.curriculum.Curriculum;
import project.homelearn.repository.curriculum.CurriculumRepository;
import project.homelearn.service.student.StudentQuestionBoardService;

import java.security.Principal;
import java.util.List;

/**
 * Author : 김승민
 * */
@Slf4j
@RestController
@RequestMapping("/students/question-boards")
@RequiredArgsConstructor
public class StudentQuestionBoardViewController {

    private final CurriculumRepository curriculumRepository;
    private final StudentQuestionBoardService studentQuestionBoardService;

    // 게시글 리스트
    @GetMapping
    public ResponseEntity<?> getQuestionBoardList(@RequestParam(required = false) String filterType,
                                                  @RequestParam(required = false) String subjectName,
                                                  Principal principal,
                                                  @RequestParam(name = "page", defaultValue = "0") int page) {
        int size = 15;
        String username = principal.getName();
        Curriculum curriculum = curriculumRepository.findCurriculumByUsername(username);

        if (curriculum == null) {
            return new ResponseEntity<>("해당 학생의 커리큘럼을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<QuestionBoardDto> questionBoardList = studentQuestionBoardService.getQuestionBoardList(filterType, subjectName, curriculum, pageable);

        if (questionBoardList.getTotalElements() > 0) {
            return new ResponseEntity<>(questionBoardList, HttpStatus.OK);
        } else {
            return new ResponseEntity<>("등록된 질문이 없습니다.", HttpStatus.NOT_FOUND);
        }
    }

    // 글 상세 조회
    @GetMapping("/{questionBoardId}")
    public ResponseEntity<?> viewQuestionBoard(@PathVariable("questionBoardId") Long questionBoardId) {
        QuestionBoardDetailDto viewQuestionBoard = studentQuestionBoardService.getQuestionBoard(questionBoardId);

        if (viewQuestionBoard != null) {
            // 조회수 증가
            studentQuestionBoardService.incrementViewCount(questionBoardId);
            return new ResponseEntity<>(viewQuestionBoard, HttpStatus.OK);
        }
        else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // 글 상세 조회 중 댓글만 추출
    @GetMapping("/{questionBoardId}/comments")
    public ResponseEntity<?> viewComments(@PathVariable("questionBoardId") Long questionBoardId) {
        List<QuestionBoardCommentDto> viewComments = studentQuestionBoardService.getQuestionBoardComment(questionBoardId);

        if (viewComments != null) {
            return new ResponseEntity<>(viewComments, HttpStatus.OK);
        }
        else {
            return new ResponseEntity<>("작성된 댓글이 없습니다.",HttpStatus.NOT_FOUND);
        }
    }
}