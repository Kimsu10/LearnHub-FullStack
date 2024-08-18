package project.homelearn.repository.board;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.homelearn.entity.board.FreeBoard;
import project.homelearn.entity.curriculum.Curriculum;
import project.homelearn.entity.user.User;

public interface FreeBoardRepository extends JpaRepository<FreeBoard, Long> {

    // 자유 게시판 페이지 리스트 -> 최신순
    @Query("select f from FreeBoard f where f.user.curriculum =:curriculum order by f.createdDate desc")
    Page<FreeBoard> findByCreatedDateDesc(@Param("curriculum") Curriculum curriculum, Pageable pageable);

    long countByUser(User user);

    @Modifying
    @Query("update FreeBoard fb set fb.viewCount = fb.viewCount + 1 where fb.id =:id")
    void increaseViewCount(@Param("id") Long id);
}