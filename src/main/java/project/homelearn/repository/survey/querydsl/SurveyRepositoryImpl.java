package project.homelearn.repository.survey.querydsl;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import project.homelearn.dto.manager.dashboard.SurveyDashboardDto;
import project.homelearn.dto.manager.manage.curriculum.CurriculumSurveyDto;
import project.homelearn.dto.manager.survey.CurriculumAndSurveyDto;
import project.homelearn.dto.manager.survey.SurveyChoiceStatisticsDto;
import project.homelearn.dto.student.survey.SurveyModalDto;
import project.homelearn.dto.student.survey.SurveyModalSubDto;
import project.homelearn.entity.curriculum.Curriculum;
import project.homelearn.entity.survey.Survey;
import project.homelearn.entity.survey.SurveyContent;
import project.homelearn.repository.user.StudentRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static project.homelearn.entity.curriculum.QCurriculum.curriculum;
import static project.homelearn.entity.student.QStudent.student;
import static project.homelearn.entity.survey.QSurvey.survey;
import static project.homelearn.entity.survey.QSurveyAnswer.surveyAnswer;
import static project.homelearn.entity.survey.QSurveyContent.surveyContent;
import static project.homelearn.entity.survey.QuestionType.RATING;

@RequiredArgsConstructor
public class SurveyRepositoryImpl implements SurveyRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final StudentRepository studentRepository;

    @Override
    public CurriculumSurveyDto findProgressSurvey(Long curriculumId) {
        Tuple tuple = queryFactory
                .select(survey.id, survey.title, survey.curriculum.th)
                .from(survey)
                .where(survey.curriculum.id.eq(curriculumId), survey.isFinished.eq(false))
                .fetchOne();
        if (tuple == null) {
            return null;
        }

        Long surveyId = tuple.get(survey.id);
        String title = tuple.get(survey.title);
        Long th = tuple.get(survey.curriculum.th);

        Long completed = findSurveyCompleteCount(surveyId);

        Long total = queryFactory
                .select(student.count())
                .from(student)
                .where(student.curriculum.id.eq(curriculumId))
                .fetchOne();
        return new CurriculumSurveyDto(surveyId, title, th, completed, total);
    }

    @Override
    public List<CurriculumSurveyDto> findEndSurvey(Long curriculumId) {
        List<Tuple> tuples = queryFactory
                .select(survey.id, survey.title, survey.curriculum.th)
                .from(survey)
                .where(survey.curriculum.id.eq(curriculumId), survey.isFinished.eq(true))
                .fetch();
        if (tuples == null) {
            return null;
        }

        List<CurriculumSurveyDto> result = new ArrayList<>();
        for (Tuple tuple : tuples) {
            Long surveyId = tuple.get(survey.id);
            Long completed = findSurveyCompleteCount(surveyId);
            Long total = queryFactory
                    .select(student.count())
                    .from(student)
                    .where(student.curriculum.id.eq(curriculumId))
                    .fetchOne();
            CurriculumSurveyDto surveyDto = new CurriculumSurveyDto(surveyId, tuple.get(survey.title), tuple.get(survey.curriculum.th), completed, total);
            result.add(surveyDto);
        }
        return result;
    }

    @Override
    public List<SurveyDashboardDto> findSurveyTop2Dto() {
        List<Tuple> tuples = queryFactory
                .select(survey.id, survey.curriculum, survey.curriculum.th, survey.title, survey.isFinished)
                .from(survey)
                .join(survey.curriculum, curriculum)
                .orderBy(survey.id.desc())
                .limit(2)
                .fetch();

        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }

        List<SurveyDashboardDto> result = new ArrayList<>();

        for (Tuple tuple : tuples) {
            Long surveyId = tuple.get(survey.id);
            Curriculum curriculum = tuple.get(survey.curriculum);

            Long participants = findSurveyCompleteCount(surveyId);
            Integer total = studentRepository.findStudentCountByCurriculum(curriculum);

            SurveyDashboardDto surveyDashboardDto = new SurveyDashboardDto();
            surveyDashboardDto.setId(surveyId);
            surveyDashboardDto.setTitle(tuple.get(survey.title));
            surveyDashboardDto.setTh(tuple.get(survey.curriculum.th));
            surveyDashboardDto.setIsCompleted(tuple.get(survey.isFinished));
            surveyDashboardDto.setParticipants(participants);
            surveyDashboardDto.setTotal((long) total);

            result.add(surveyDashboardDto);
        }

        return result;
    }

    @Override
    public Long findSurveyCompleteCount(Long surveyId) {
        List<Long> userIds = queryFactory
                .select(surveyAnswer.user.id)
                .from(surveyAnswer)
                .where(surveyAnswer.survey.id.eq(surveyId))
                .distinct()
                .fetch();
        return (long) userIds.size();
    }

    @Override
    public Map<Integer, Integer> findSurveyBasicTrend(Long curriculumId) {
        List<Survey> fetch = queryFactory
                .select(survey)
                .from(survey)
                .where(survey.curriculum.id.eq(curriculumId), survey.isFinished.eq(true))
                .fetch();

        if (fetch == null) {
            return null;
        }

        Map<Integer, Integer> result = new ConcurrentHashMap<>();
        int round = 1;

        for (Survey survey : fetch) {
            Long surveyId = survey.getId();
            Integer sum = queryFactory
                    .select(surveyAnswer.rating.sum())
                    .from(surveyAnswer)
                    .join(surveyAnswer.surveyContent, surveyContent)
                    .where(surveyAnswer.survey.id.eq(surveyId), surveyContent.questionType.eq(RATING))
                    .fetchOne();
            if (sum != null) {
                Long completeCount = findSurveyCompleteCount(surveyId);
                Integer average = (completeCount != null && completeCount > 0) ? (int) (sum / completeCount) : 0;
                result.put(round++, average);
            }
        }
        return result;
    }

    @Override
    public CurriculumAndSurveyDto findCurriculumAndSurvey(Long curriculumId, Long surveyId) {
        Tuple tuple = queryFactory
                .select(curriculum.name, curriculum.th, survey.round)
                .from(curriculum)
                .join(survey).on(curriculum.id.eq(survey.curriculum.id))
                .where(curriculum.id.eq(curriculumId), survey.id.eq(surveyId))
                .fetchOne();

        if (tuple == null) {
            return null;
        }

        Long completeCount = findSurveyCompleteCount(surveyId);
        Long total = queryFactory
                .select(student.count())
                .from(student)
                .where(student.curriculum.id.eq(curriculumId))
                .fetchOne();

        return CurriculumAndSurveyDto
                .builder()
                .curriculumName(tuple.get(curriculum.name))
                .th(tuple.get(curriculum.th))
                .round(tuple.get(survey.round))
                .completed(completeCount)
                .total(total)
                .build();
    }

    @Override
    public List<SurveyChoiceStatisticsDto> findSurveyChoiceStatistics(Long surveyId) {
        List<SurveyContent> fetch = queryFactory
                .selectFrom(surveyContent)
                .where(surveyContent.questionType.eq(RATING))
                .fetch();

        List<SurveyChoiceStatisticsDto> result = new ArrayList<>();
        int scoreSize = 5;

        for (SurveyContent content : fetch) {
            String surveyContent = content.getContent();

            Map<Integer, Integer> map = new ConcurrentHashMap<>();

            for (int i = 1; i <= scoreSize; i++) {
                Long count = queryFactory
                        .select(surveyAnswer.count())
                        .from(surveyAnswer)
                        .where(surveyAnswer.survey.id.eq(surveyId), surveyAnswer.surveyContent.id.eq(content.getId()), surveyAnswer.rating.eq(i))
                        .fetchOne();

                map.put(i, count != null ? count.intValue() : 0);
            }

            SurveyChoiceStatisticsDto statisticsDto = new SurveyChoiceStatisticsDto();
            statisticsDto.setContent(surveyContent);
            statisticsDto.setScoreResponseCount(map);
            result.add(statisticsDto);
        }
        return result;
    }

    @Override
    public Page<String> findSurveyTextResponse(Long surveyId, Pageable pageable) {
        List<String> textAnswers = queryFactory
                .select(surveyAnswer.textAnswer)
                .from(surveyAnswer)
                .where(surveyAnswer.survey.id.eq(surveyId), surveyAnswer.rating.isNull())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(surveyAnswer.count())
                .from(surveyAnswer)
                .where(surveyAnswer.survey.id.eq(surveyId))
                .fetchOne();

        if (total == null) {
            total = 0L;
        }
        return new PageImpl<>(textAnswers, pageable, total);
    }

    @Override
    public SurveyModalDto findSurveyModal(Long surveyId) {
        String title = queryFactory
                .select(survey.title)
                .from(survey)
                .where(survey.id.eq(surveyId))
                .fetchOne();

        List<SurveyModalSubDto> subDtos = new ArrayList<>();

        List<Tuple> tuples = queryFactory
                .select(surveyContent.id, surveyContent.content, surveyContent.questionType)
                .from(surveyContent)
                .fetch();

        for (Tuple tuple : tuples) {
            SurveyModalSubDto subDto = new SurveyModalSubDto(tuple.get(surveyContent.id), tuple.get(surveyContent.content), tuple.get(surveyContent.questionType));
            subDtos.add(subDto);
        }

        SurveyModalDto result = new SurveyModalDto();
        result.setTitle(title);
        result.setContents(subDtos);
        return result;
    }
}