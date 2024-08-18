package project.homelearn.service.student;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import project.homelearn.dto.common.FileDto;
import project.homelearn.dto.common.board.QuestionBoardCommentDto;
import project.homelearn.dto.common.board.QuestionBoardDetailDto;
import project.homelearn.dto.common.board.QuestionBoardDto;
import project.homelearn.dto.student.board.CommentWriteDto;
import project.homelearn.dto.student.board.QuestionBoardWriteDto;
import project.homelearn.dto.student.dashboard.ViewQuestionBoardDto;
import project.homelearn.entity.board.QuestionBoard;
import project.homelearn.entity.board.comment.QuestionBoardComment;
import project.homelearn.entity.board.scrap.QuestionScrap;
import project.homelearn.entity.curriculum.Curriculum;
import project.homelearn.entity.curriculum.Subject;
import project.homelearn.entity.student.Student;
import project.homelearn.entity.student.badge.BadgeConstants;
import project.homelearn.entity.teacher.Teacher;
import project.homelearn.entity.user.User;
import project.homelearn.repository.board.QuestionBoardCommentRepository;
import project.homelearn.repository.board.QuestionBoardRepository;
import project.homelearn.repository.board.QuestionScrapRepository;
import project.homelearn.repository.curriculum.CurriculumRepository;
import project.homelearn.repository.curriculum.SubjectRepository;
import project.homelearn.repository.user.StudentRepository;
import project.homelearn.repository.user.TeacherRepository;
import project.homelearn.repository.user.UserRepository;
import project.homelearn.service.common.StorageService;
import project.homelearn.service.teacher.TeacherNotificationService;

import java.util.List;
import java.util.stream.Collectors;

import static project.homelearn.config.storage.FolderType.QUESTION_BOARD;
import static project.homelearn.entity.user.Role.ROLE_TEACHER;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class StudentQuestionBoardService {

    private final UserRepository userRepository;
    private final StorageService storageService;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final QuestionBoardRepository questionBoardRepository;
    private final QuestionBoardCommentRepository commentRepository;
    private final CurriculumRepository curriculumRepository;
    private final QuestionScrapRepository questionScrapRepository;
    private final TeacherNotificationService teacherNotificationService;
    private final TeacherRepository teacherRepository;
    private final StudentNotificationService studentNotificationService;
    private final BadgeService badgeService;

    // 글 작성
    public void writeQuestionBoard(String username, QuestionBoardWriteDto questionBoardWriteDto) {
        Student student = studentRepository.findByUsername(username);

        QuestionBoard questionBoard = new QuestionBoard();

        questionBoard.setUser(student);
        questionBoard.setTitle(questionBoardWriteDto.getTitle());
        questionBoard.setContent(questionBoardWriteDto.getContent());

        // 성진 수정
        Long subjectId = questionBoardWriteDto.getSubjectId();
        if (subjectId != null) {
            Subject subject = subjectRepository.findById(subjectId).orElseThrow();
            questionBoard.setSubject(subject);
        }

        MultipartFile image = questionBoardWriteDto.getImage();
        if (image != null) {
            String folderPath = storageService.getFolderPath(student, QUESTION_BOARD);
            FileDto fileDto = storageService.uploadFile(image, folderPath);
            questionBoard.setImageName(fileDto.getUploadFileName());
            questionBoard.setImagePath(fileDto.getFilePath());
        }
        questionBoardRepository.save(questionBoard);

        // 강사에게 알림
        Teacher teacher = teacherRepository.findByStudentUsername(username);
        teacherNotificationService.questionNotify(teacher, questionBoard);

        long count = questionBoardRepository.countByUser(student);
        if (count == 10) {
            badgeService.getBadge(student, BadgeConstants.QUESTION);
        }
    }

    // 글 삭제
    public boolean deleteQuestionBoard(Long questionBoardId, String username) {
        QuestionBoard questionBoard = questionBoardRepository.findById(questionBoardId).orElseThrow();
        String writer = questionBoard.getUser().getUsername();

        if (!writer.equals(username)) {
            return false;
        }

        String image = questionBoard.getImagePath();
        if (image != null) {
            storageService.deleteFile(image);
        }

        questionBoardRepository.deleteById(questionBoardId);
        return true;
    }

    // 글 수정 (과목 변경 추가해야 함!)
    public boolean modifyBoard(Long questionBoardId, String username, QuestionBoardWriteDto questionBoardWriteDto) {
        QuestionBoard questionBoard = questionBoardRepository.findById(questionBoardId).orElseThrow();
        User student = questionBoard.getUser();
        if (!student.getUsername().equals(username)) {
            return false;
        }
        questionBoard.setTitle(questionBoardWriteDto.getTitle());
        questionBoard.setContent(questionBoardWriteDto.getContent());

        MultipartFile image = questionBoardWriteDto.getImage();
        if (image != null) {
            // 사진 수정 시 기존 사진이 있다면 삭제
            String previousImage = questionBoard.getImagePath();
            if (previousImage != null) {
                storageService.deleteFile(previousImage);
            }

            String folderPath = storageService.getFolderPath(student, QUESTION_BOARD);
            FileDto fileDto = storageService.uploadFile(image, folderPath);
            questionBoard.setImageName(fileDto.getUploadFileName());
            questionBoard.setImagePath(fileDto.getFilePath());
        }

        return true;
    }

    // 댓글 작성 (질문 답변)
    public void writeComment(Long questionBoardId, String username, CommentWriteDto commentDto) {
        User commentWriter = userRepository.findByUsername(username);
        QuestionBoard questionBoard = questionBoardRepository.findQuestionBoardAndWriter(questionBoardId);

        QuestionBoardComment comment = new QuestionBoardComment();
        comment.setUser(commentWriter);
        comment.setQuestionBoard(questionBoard);
        comment.setContent(commentDto.getContent());
        commentRepository.save(comment);

        // 질문 작성한 학생에게 알림
        User boardWriter = questionBoard.getUser();
        if (boardWriter.equals(commentWriter)) {
            return;
        }
        studentNotificationService.questionResponseNotify(boardWriter, questionBoard, comment);

        long count = commentRepository.countDistinctQuestionCommentByUser(commentWriter);
        if (count == 10) {
            badgeService.getBadge(commentWriter, BadgeConstants.EXPLAIN);
        }
    }

    // 댓글 수정
    public boolean modifyComment(Long commentId, String username, CommentWriteDto commentDto) {
        QuestionBoardComment comment = commentRepository.findById(commentId).orElseThrow();
        String writer = comment.getUser().getUsername();
        if (!writer.equals(username)) {
            return false;
        }
        comment.setContent(commentDto.getContent());
        return true;
    }

    // 댓글 삭제
    public boolean deleteComment(Long questionBoardId, Long commentId, String username) {
        QuestionBoard board = questionBoardRepository.findById(questionBoardId).orElseThrow();
        String boardWriter = board.getUser().getUsername();

        // 게시글 주인은 모든 댓글 삭제 가능
        if (boardWriter.equals(username)) {
            commentRepository.deleteById(commentId);
            return true;
        }

        QuestionBoardComment comment = commentRepository.findById(commentId).orElseThrow();
        String commentWriter = comment.getUser().getUsername();
        if (!commentWriter.equals(username)) {
            return false;
        }
        commentRepository.deleteById(commentId);
        return true;
    }

    // 대댓글 작성
    public void writeReplyComment(Long commentId, String username, CommentWriteDto commentDto) {
        User writer = userRepository.findByUsername(username);
        QuestionBoardComment parentComment = commentRepository.findById(commentId).orElseThrow();

        // 대댓글 깊이 확인
        if (parentComment.getParentComment() != null) {
            throw new IllegalArgumentException("대댓글의 깊이는 1로 제한됩니다.");
        }

        QuestionBoardComment reply = new QuestionBoardComment();
        reply.setUser(writer);
        reply.setContent(commentDto.getContent());
        reply.setQuestionBoard(parentComment.getQuestionBoard());
        reply.setParentComment(parentComment);
        commentRepository.save(reply);

        // 부모 댓글의 작성자가 강사면 강사에게 알림
        User parent = parentComment.getUser();
        if (parent.getRole().equals(ROLE_TEACHER) && !writer.getRole().equals(ROLE_TEACHER)) {
            Teacher teacher = teacherRepository.findByStudentUsername(username);
            QuestionBoard questionBoard = parentComment.getQuestionBoard();
            teacherNotificationService.questionReplyNotify(teacher, questionBoard, reply);
        }
    }

    // 대댓글 수정
    public boolean modifyReplyComment(Long replyId, String username, CommentWriteDto commentDto) {
        QuestionBoardComment reply = commentRepository.findById(replyId).orElseThrow();
        String writer = reply.getUser().getUsername();
        if (!writer.equals(username)) {
            return false;
        }
        reply.setContent(commentDto.getContent());
        return true;
    }

    // 댓글 수 증가
    public void incrementCommentCount(Long questionBoardId) {
        QuestionBoard questionBoard = questionBoardRepository.findById(questionBoardId).orElseThrow();
        questionBoard.setCommentCount(questionBoard.getCommentCount() + 1);
    }

    // 댓글 수 감소
    public void decrementCommentCount(Long questionBoardId) {
        QuestionBoard questionBoard = questionBoardRepository.findById(questionBoardId).orElseThrow();
        questionBoard.setCommentCount(questionBoard.getCommentCount() - 1);
    }

    // 질문 게시판 글 스크랩 = 나도 궁금해
    public boolean addScrap(String username, Long questionBoardId) {
        QuestionBoard questionBoard = questionBoardRepository.findById(questionBoardId).orElseThrow();
        Student student = studentRepository.findByUsername(username);

        QuestionScrap myScrap = new QuestionScrap(
                student,questionBoard
        );

        if (questionScrapRepository.existByUserNameAndQuestionBoardId(username,questionBoardId)) {
            return false;
        }
        else {
            questionScrapRepository.save(myScrap);
            return true;
        }
    }

    // 질문 게시판 글 스크랩 지우기
    public boolean deleteScrap(String username, Long questionBoardId) {

        if (questionScrapRepository.existByUserNameAndQuestionBoardId(username,questionBoardId)) {
            questionScrapRepository.deleteByUserNameAndQuestionBoardId(username,questionBoardId);
            return true;
        }
        else {
            return false;
        }
    }

    // 조회수 증가
    public void incrementViewCount(Long questionBoardId) {
        questionBoardRepository.incrementViewCountById(questionBoardId);
    }

    // 글 상세 조회
    public QuestionBoardDetailDto getQuestionBoard(Long questionBoardId) {
        QuestionBoard questionBoard = questionBoardRepository.findById(questionBoardId).orElseThrow();

        return new QuestionBoardDetailDto(
                questionBoard.getId(),
                questionBoard.getTitle(),
                questionBoard.getContent(),
                questionBoard.getViewCount(),
                questionBoard.getQuestionScraps().size(),
                questionBoard.getUser().getName(),
                questionBoard.getCreatedDate(),
                questionBoard.getCommentCount()
        );
    }

    // 댓글 뽑아오기
    public List<QuestionBoardCommentDto> getQuestionBoardComment(Long questionBoardId) {
        List<QuestionBoardComment> comments = commentRepository.findByQuestionBoardIdAndParentCommentIsNull(questionBoardId);

        return comments.stream()
                .map(this::convertToCommentDto)
                .collect(Collectors.toList());
    }

    // 댓글 Dto 변환
    public QuestionBoardCommentDto convertToCommentDto(QuestionBoardComment comment) {
        User user = comment.getUser();
        if (user == null) {
            return new QuestionBoardCommentDto(
                    comment.getId(),
                    "ChatGPT",
                    comment.getContent(),
                    comment.getCreatedDate()
            );
        }

        return new QuestionBoardCommentDto(
                comment.getId(),
                comment.getUser().getName(),
                comment.getUser().getImageName(),
                comment.getContent(),
                comment.getCreatedDate(),
                comment.getReplies().stream()
                        .map(this::convertToCommentDto)
                        .collect(Collectors.toList())
        );
    }

    // 게시글 리스트
    public Page<QuestionBoardDto> getQuestionBoardList(String filterType, String subjectName, Curriculum curriculum, Pageable pageable) {
        if (filterType == null) {
            filterType = "default";
        }

        Page<QuestionBoard> questionBoards;

        // 필터링 타입에 따라 다른 쿼리 메소드 호출
        switch (filterType) {
            case "subject":
                questionBoards = questionBoardRepository.findBySubjectNameAndCurriculum(subjectName, curriculum, pageable);
                break;

            case "unanswered":
                questionBoards = questionBoardRepository.findByCommentsIsNullAndCurriculum(curriculum, pageable);
                break;

            case "subjectUnanswered":
                questionBoards = questionBoardRepository.findBySubjectNameAndCommentsIsNullAndCurriculum(subjectName, curriculum, pageable);
                break;

            default:
                questionBoards = questionBoardRepository.findByCreatedDateDesc(curriculum, pageable);
                break;
        }

        // Entity -> DTO 변환
        return questionBoards.map(this::convertToListDto);
    }

    private QuestionBoardDto convertToListDto(QuestionBoard questionBoard) {
        boolean isCommentHere = questionBoardRepository.hasTeacherComment(questionBoard);
        return new QuestionBoardDto(
                questionBoard.getId(),
                questionBoard.getSubject().getName(),
                questionBoard.getTitle(),
                questionBoard.getUser().getName(),
                questionBoard.getContent(),
                questionBoard.getCreatedDate(),
                questionBoard.getCommentCount(),
                isCommentHere
        );
    }

    // 최근 질문 2개
    public List<ViewQuestionBoardDto> getQuestionTop2(String username) {
        Curriculum curriculum = curriculumRepository.findCurriculumByUsername(username);
        return questionBoardRepository.findQuestionTop2(curriculum);
    }
}