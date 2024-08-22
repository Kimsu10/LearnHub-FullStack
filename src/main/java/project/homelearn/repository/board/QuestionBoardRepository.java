package project.homelearn.repository.board;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.homelearn.entity.board.QuestionBoard;
import project.homelearn.entity.curriculum.Curriculum;
import project.homelearn.entity.user.User;
import project.homelearn.repository.board.querydsl.QuestionBoardRepositoryCustom;

import java.time.LocalDateTime;
import java.util.List;

public interface QuestionBoardRepository extends JpaRepository<QuestionBoard, Long>, QuestionBoardRepositoryCustom {

    List<QuestionBoard> findByCreatedDateBeforeAndCommentsIsNull(LocalDateTime dateTime);

    //질문 게시판 페이지 리스트 -> 최신순
    @Query("select q from QuestionBoard q where q.user.curriculum = :curriculum order by q.createdDate desc")
    Page<QuestionBoard> findByCreatedDateDesc(@Param("curriculum") Curriculum curriculum, Pageable pageable);

    // 질문 게시판 필터링 1 : 커리큘럼 내에서 과목명 기준으로 최신순
    @Query("select q from QuestionBoard q join q.subject s where s.name = :subjectName and q.user.curriculum = :curriculum order by q.createdDate desc")
    Page<QuestionBoard> findBySubjectNameAndCurriculum(@Param("subjectName") String subjectName, @Param("curriculum") Curriculum curriculum, Pageable pageable);

    // 질문 게시판 필터링 2 : 커리큘럼 내에서 답변 없는 순
    @Query("select q from QuestionBoard q left join q.comments c where q.user.curriculum = :curriculum and c.id is null order by q.createdDate desc")
    Page<QuestionBoard> findByCommentsIsNullAndCurriculum(@Param("curriculum")Curriculum curriculum, Pageable pageable);

    // 질문 게시판 필터링 3 : 커리큘럼 내에서 과목명 + 답변 없는 순
    @Query("select q from QuestionBoard q left join q.comments c join q.subject s where s.name = :subjectName and q.user.curriculum = :curriculum and c.id is null order by q.createdDate desc")
    Page<QuestionBoard> findBySubjectNameAndCommentsIsNullAndCurriculum(@Param("subjectName") String subjectName, @Param("curriculum") Curriculum curriculum, Pageable pageable);

    // 강사님의 댓글이 있는지 없는지
    @Query("select case when count(c) > 0 then true else false end from QuestionBoardComment c where c.questionBoard = :questionBoard and c.user.role = project.homelearn.entity.user.Role.ROLE_TEACHER")
    boolean hasTeacherComment(@Param("questionBoard") QuestionBoard questionBoard);

    @Query("select q from QuestionBoard q join fetch q.user where q.id =:questionId")
    QuestionBoard findQuestionBoardAndWriter(@Param("questionId") Long questionId);

    long countByUser(User user);

    @Modifying
    @Query("update QuestionBoard q set q.viewCount = q.viewCount + 1 where q.id = :id")
    void incrementViewCountById(@Param("id") Long id);

    @Modifying
    @Query("update QuestionBoard q set q.commentCount = q.commentCount + 1 where q.id = :id")
    void incrementCommentCountById(@Param("id")Long id);

    @Modifying
    @Query("update QuestionBoard q set q.commentCount = q.commentCount - 1 where q.id = :id")
    void decrementCommentCountById(@Param("id")Long id);
}