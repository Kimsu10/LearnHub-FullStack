package project.homelearn.service.student;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import project.homelearn.dto.common.FileDto;
import project.homelearn.dto.student.board.*;
import project.homelearn.entity.board.FreeBoard;
import project.homelearn.entity.board.comment.FreeBoardComment;
import project.homelearn.entity.curriculum.Curriculum;
import project.homelearn.entity.student.Student;
import project.homelearn.entity.student.badge.BadgeConstants;
import project.homelearn.entity.user.User;
import project.homelearn.repository.board.FreeBoardCommentRepository;
import project.homelearn.repository.board.FreeBoardRepository;
import project.homelearn.repository.user.StudentRepository;
import project.homelearn.repository.user.UserRepository;
import project.homelearn.service.common.StorageService;

import java.util.List;
import java.util.stream.Collectors;

import static project.homelearn.config.storage.FolderType.FREE_BOARD;

/**
 * Author : 정성진
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class StudentBoardService {

    private final StorageService storageService;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final FreeBoardRepository boardRepository;
    private final FreeBoardCommentRepository commentRepository;
    private final BadgeService badgeService;

    public void writeBoard(String username, FreeBoardWriteDto boardDto) {
        Student student = studentRepository.findByUsername(username);

        FreeBoard board = new FreeBoard();
        board.setUser(student);
        board.setTitle(boardDto.getTitle());

        MultipartFile image = boardDto.getImage();
        if (image != null) {
            String folderPath = storageService.getFolderPath(student, FREE_BOARD);
            FileDto fileDto = storageService.uploadFile(image, folderPath);
            board.setImageName(fileDto.getUploadFileName());

            String filePath = fileDto.getFilePath();
            board.setImagePath(filePath);

            String contentWithImagePath = boardDto.getContent().replace("<img src=\"\" />", "<img src=\"/image/" + filePath + "\" />");
            board.setContent(contentWithImagePath);
        } else {
            board.setContent(boardDto.getContent());
        }
        boardRepository.save(board);

        long count = boardRepository.countByUser(student);
        if (count == 10) {
            badgeService.getBadge(student, BadgeConstants.TALK);
        }
    }

    public boolean modifyBoard(Long boardId, String username, FreeBoardWriteDto boardDto) {
        FreeBoard board = boardRepository.findById(boardId).orElseThrow();
        User student = board.getUser();
        if (!student.getUsername().equals(username)) {
            return false;
        }
        board.setTitle(boardDto.getTitle());
        board.setContent(boardDto.getContent());

        MultipartFile image = boardDto.getImage();

        // 사진 첨부 O
        if (image != null) {
            // 사진 수정 시 기존 사진이 있다면 삭제
            String previousImage = board.getImagePath();
            if (previousImage != null) {
                storageService.deleteFile(previousImage);
            }

            String folderPath = storageService.getFolderPath(student, FREE_BOARD);
            FileDto fileDto = storageService.uploadFile(image, folderPath);
            board.setImageName(fileDto.getUploadFileName());
            board.setImagePath(fileDto.getFilePath());
        }

        /*
        기존 사진을 삭제하는 로직 필요
         */
        return true;
    }

    public boolean deleteBoard(Long boardId, String username) {
        FreeBoard board = boardRepository.findById(boardId).orElseThrow();
        String writer = board.getUser().getUsername();

        if (!writer.equals(username)) {
            return false;
        }

        String image = board.getImagePath();
        if (image != null) {
            storageService.deleteFile(image);
        }

        boardRepository.deleteById(boardId);
        return true;
    }

    public void writeComment(Long boardId, String username, CommentWriteDto commentDto) {
        User user = userRepository.findByUsername(username);
        FreeBoard board = boardRepository.findById(boardId).orElseThrow();

        FreeBoardComment comment = new FreeBoardComment();
        comment.setUser(user);
        comment.setFreeBoard(board);
        comment.setContent(commentDto.getContent());
        commentRepository.save(comment);
    }

    public boolean modifyComment(Long commentId, String username, CommentWriteDto commentDto) {
        FreeBoardComment comment = commentRepository.findById(commentId).orElseThrow();
        String writer = comment.getUser().getUsername();
        if (!writer.equals(username)) {
            return false;
        }
        comment.setContent(commentDto.getContent());
        return true;
    }

    public boolean deleteComment(Long boardId, Long commentId, String username) {
        FreeBoard board = boardRepository.findById(boardId).orElseThrow();
        String boardWriter = board.getUser().getUsername();

        // 게시글 주인은 모든 댓글 삭제 가능
        if (boardWriter.equals(username)) {
            commentRepository.deleteById(commentId);
            return true;
        }

        FreeBoardComment comment = commentRepository.findById(commentId).orElseThrow();
        String commentWriter = comment.getUser().getUsername();
        if (!commentWriter.equals(username)) {
            return false;
        }
        commentRepository.deleteById(commentId);
        return true;
    }

    public void writeReplyComment(Long commentId, String username, CommentWriteDto commentDto) {
        User user = userRepository.findByUsername(username);
        FreeBoardComment parentComment = commentRepository.findById(commentId).orElseThrow();

        // 대댓글 깊이 확인
        if (parentComment.getParentComment() != null) {
            throw new IllegalArgumentException("대댓글의 깊이는 1로 제한됩니다.");
        }

        FreeBoardComment reply = new FreeBoardComment();
        reply.setUser(user);
        reply.setContent(commentDto.getContent());
        reply.setFreeBoard(parentComment.getFreeBoard());
        reply.setParentComment(parentComment);
        commentRepository.save(reply);
    }

    public boolean modifyReplyComment(Long replyId, String username, CommentWriteDto commentDto) {
        FreeBoardComment reply = commentRepository.findById(replyId).orElseThrow();
        String writer = reply.getUser().getUsername();
        if (!writer.equals(username)) {
            return false;
        }

        reply.setContent(commentDto.getContent());
        return true;
    }

    // 리스트
    public Page<FreeBoardDto> getBoardList(Curriculum curriculum, Pageable pageable) {

        Page<FreeBoard> freeBoardPage = boardRepository.findByCreatedDateDesc(curriculum,pageable);

        return freeBoardPage.map(this::convertToListDto);
    }

    private FreeBoardDto convertToListDto(FreeBoard freeBoard) {
        return new FreeBoardDto(
                freeBoard.getId(),
                freeBoard.getTitle(),
                freeBoard.getUser().getName(),
                freeBoard.getCreatedDate(),
                freeBoard.getCommentCount()
        );
    }

    // 상세보기
    public FreeBoardDetailDto getBoard(Long boardId) {
        FreeBoard freeBoard = boardRepository.findById(boardId).orElseThrow();

        return new FreeBoardDetailDto(
                freeBoard.getId(),
                freeBoard.getTitle(),
                freeBoard.getContent(),
                freeBoard.getViewCount(),
                freeBoard.getUser().getName(),
                freeBoard.getUser().getUsername(),
                freeBoard.getCreatedDate(),
                freeBoard.getCommentCount()
        );
    }

    // 댓글 추출
    public List<FreeBoardCommentDto> getBoardComment(Long boardId) {
        List<FreeBoardComment> comments = commentRepository.findByFreeBoardCommentIdAndParentCommentIsNull(boardId);
        // 댓글을 가져오는데 -> 최상위 댓글만 = 대댓글이 아닌 댓글만

        return comments.stream()
                .map(this::convertToCommentDto)
                .collect(Collectors.toList());
    }

    // 댓글 Dto 반환
    public FreeBoardCommentDto convertToCommentDto(FreeBoardComment comment) {
        return new FreeBoardCommentDto(
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

    // 조회수 증가
    public void incrementViewCount(Long boardId) {
        boardRepository.increaseViewCount(boardId);
    }
}