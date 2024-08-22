package project.homelearn.repository.curriculum;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.homelearn.dto.manager.manage.curriculum.CurriculumBasicDto;
import project.homelearn.dto.manager.survey.CurriculumSimpleDto;
import project.homelearn.entity.curriculum.Curriculum;
import project.homelearn.entity.curriculum.CurriculumType;
import project.homelearn.repository.curriculum.querydsl.CurriculumRepositoryCustom;

import java.util.List;

public interface CurriculumRepository extends JpaRepository<Curriculum, Long>, CurriculumRepositoryCustom {

    Curriculum findByFullName(String fullName);

    @Query("select new project.homelearn.dto.manager.manage.curriculum.CurriculumBasicDto(c.id, c.name, c.th, c.startDate, c.endDate) from Curriculum c where c.id =:id")
    CurriculumBasicDto findCurriculumBasic(@Param("id") Long id);

    @Query("select c from Curriculum c where c.type = :type order by c.th asc")
    List<Curriculum> findByCurriculumType(@Param("type") CurriculumType type);

    @Query("select c.th, c.type from Curriculum c where c.id =:id")
    List<Object> findThById(@Param("id") Long id);

    @Query("select new project.homelearn.dto.manager.survey.CurriculumSimpleDto(c.name, c.th) from Curriculum c where c.id =:curriculumId")
    CurriculumSimpleDto findCurriculumSimple(@Param("curriculumId") Long curriculumId);

    @Query("select c.color from Curriculum c")
    List<String> findAllColor();
}