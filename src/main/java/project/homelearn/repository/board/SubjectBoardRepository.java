package project.homelearn.repository.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.homelearn.entity.curriculum.SubjectBoard;
import project.homelearn.repository.board.querydsl.SubjectBoardRepositoryCustom;

public interface SubjectBoardRepository extends JpaRepository<SubjectBoard, Long>, SubjectBoardRepositoryCustom {

    @Modifying
    @Query("update SubjectBoard sb set sb.viewCount = sb.viewCount + 1 where sb.id =:id")
    void increaseViewCount(@Param("id") Long id);
}