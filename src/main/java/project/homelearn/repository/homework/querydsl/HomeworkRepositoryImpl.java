package project.homelearn.repository.homework.querydsl;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import project.homelearn.dto.student.dashboard.ViewHomeworkDto;
import project.homelearn.dto.teacher.dashboard.HomeworkStateDto;
import project.homelearn.dto.teacher.homework.HomeworkDetailDto;
import project.homelearn.dto.teacher.homework.HomeworkSubmitListDto;
import project.homelearn.dto.teacher.homework.HomeworkTabDto;
import project.homelearn.entity.curriculum.Curriculum;
import project.homelearn.entity.user.User;
import project.homelearn.repository.homework.StudentHomeworkRepository;
import project.homelearn.repository.user.StudentRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static project.homelearn.entity.homework.QHomework.homework;
import static project.homelearn.entity.homework.QStudentHomework.studentHomework;

@RequiredArgsConstructor
public class HomeworkRepositoryImpl implements HomeworkRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final StudentRepository studentRepository;
    private final StudentHomeworkRepository studentHomeworkRepository;

    @Override
    public HomeworkStateDto findHomeworkStateDto(Curriculum curriculum, Integer totalCount) {
        Tuple tuple = queryFactory
                .select(homework.id, homework.title)
                .from(homework)
                .where(homework.curriculum.eq(curriculum))
                .orderBy(homework.createdDate.desc())
                .limit(1)
                .fetchOne();

        if (tuple == null) {
            return null;
        }

        Long homeworkId = tuple.get(homework.id);
        String title = tuple.get(homework.title);

        Long submitCount = queryFactory
                .select(studentHomework.count())
                .from(studentHomework)
                .where(studentHomework.homework.id.eq(homeworkId))
                .fetchOne();

        // 제출률 계산 (정수로 변환)
        int submitRate = 0;
        if (submitCount != null && totalCount != null && totalCount > 0) {
            submitRate = Math.toIntExact((submitCount * 100) / totalCount);
        }

        return HomeworkStateDto
                .builder()
                .title(title)
                .submitRate(submitRate)
                .build();
    }

    @Override
    public Page<HomeworkTabDto> findHomeworks(Curriculum curriculum, Pageable pageable, String status) {
        List<Tuple> tuples = new ArrayList<>();

        if (status.equals("진행")) {
            tuples = queryFactory
                    .select(homework.id, homework.title, homework.description, homework.deadline)
                    .from(homework)
                    .where(homework.curriculum.eq(curriculum), homework.deadline.after(LocalDateTime.now()))
                    .orderBy(homework.createdDate.desc())
                    .fetch();
        }

        if (status.equals("마감")) {
            tuples = queryFactory
                    .select(homework.id, homework.title, homework.description, homework.deadline)
                    .from(homework)
                    .where(homework.curriculum.eq(curriculum), homework.deadline.before(LocalDateTime.now()))
                    .orderBy(homework.createdDate.desc())
                    .fetch();
        }

        List<HomeworkTabDto> homeworkTabDtos = new ArrayList<>();
        for (Tuple tuple : tuples) {
            Long homeworkId = tuple.get(homework.id);
            String title = tuple.get(homework.title);
            String description = tuple.get(homework.description);
            LocalDateTime deadline = tuple.get(homework.deadline);
            Long submitCount = findCompletedCount(homeworkId);

            HomeworkTabDto dto = new HomeworkTabDto();
            dto.setHomeworkId(homeworkId);
            dto.setTitle(title);
            dto.setDescription(description);
            dto.setDeadLine(deadline);
            dto.setSubmitCount(submitCount);

            homeworkTabDtos.add(dto);
        }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), homeworkTabDtos.size());

        return new PageImpl<>(homeworkTabDtos.subList(start, end), pageable, homeworkTabDtos.size());
    }

    @Override
    public Long findCompletedCount(Long homeworkId) {
        return queryFactory
                .select(studentHomework.count())
                .from(studentHomework)
                .where(studentHomework.homework.id.eq(homeworkId))
                .fetchOne();
    }

    @Override
    public HomeworkDetailDto findHomeworkDetail(Long homeworkId, Long unsubmittedCount, Curriculum curriculum) {
        Tuple tuple = queryFactory
                .select(homework.title,
                        homework.description,
                        homework.uploadFileName,
                        homework.filePath,
                        homework.createdDate,
                        homework.deadline,
                        homework.requiredFile,
                        homework.acceptFile)
                .from(homework)
                .where(homework.id.eq(homeworkId))
                .fetchOne();

        if (tuple == null) {
            return null;
        }

        List<Long> allStudentIds = studentRepository.findAllStudentIds(curriculum);
        List<Long> submitStudentIds = findSubmitStudentIds(homeworkId);

        List<Long> unsubmittedIds = allStudentIds.stream()
                .filter(id -> !submitStudentIds.contains(id))
                .toList();

        List<String> unsubmittedList = new ArrayList<>();
        for (Long id : unsubmittedIds) {
            String name = studentRepository.findStudentName(id);
            unsubmittedList.add(name);
        }

        return HomeworkDetailDto
                .builder()
                .title(tuple.get(homework.title))
                .description(tuple.get(homework.description))
                .fileName(tuple.get(homework.uploadFileName))
                .filePath(tuple.get(homework.filePath))
                .enrollDate(tuple.get(homework.createdDate).toLocalDate())
                .deadLine(tuple.get(homework.deadline))
                .unsubmittedCount(unsubmittedCount)
                .unsubmittedList(unsubmittedList)
                .requiredFile(tuple.get(homework.requiredFile))
                .acceptFile(tuple.get(homework.acceptFile))
                .build();
    }

    @Override
    public List<Long> findSubmitStudentIds(Long homeworkId) {
        return queryFactory
                .select(studentHomework.user.id)
                .from(studentHomework)
                .where(studentHomework.homework.id.eq(homeworkId))
                .fetch();
    }

    @Override
    public List<HomeworkSubmitListDto> findHomeworkSubmitList(Long homeworkId) {
        List<Tuple> tuples = queryFactory
                .select(studentHomework.id,
                        studentHomework.user.name,
                        studentHomework.description,
                        studentHomework.createdDate,
                        studentHomework.uploadFileName,
                        studentHomework.filePath,
                        studentHomework.response,
                        studentHomework.responseDate)
                .from(studentHomework)
                .where(studentHomework.homework.id.eq(homeworkId))
                .orderBy(studentHomework.createdDate.asc())
                .fetch();

        return tuples.stream()
                .map(tuple -> HomeworkSubmitListDto.builder()
                        .studentHomeworkId(tuple.get(studentHomework.id))
                        .name(tuple.get(studentHomework.user.name))
                        .description(tuple.get(studentHomework.description))
                        .submitDate(tuple.get(studentHomework.createdDate))
                        .fileName(tuple.get(studentHomework.uploadFileName))
                        .filePath(tuple.get(studentHomework.filePath))
                        .response(tuple.get(studentHomework.response))
                        .responseDate(tuple.get(studentHomework.responseDate))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<ViewHomeworkDto> findHomeworkTop2(Curriculum curriculum, User student) {
        List<ViewHomeworkDto> result = new ArrayList<>();

        List<Tuple> tuples = queryFactory
                .select(homework.id, homework.title, homework.description, homework.deadline)
                .from(homework)
                .where(homework.curriculum.eq(curriculum))
                .orderBy(homework.createdDate.desc())
                .limit(2)
                .fetch();

        for (Tuple tuple : tuples) {
            Long homeworkId = tuple.get(homework.id);
            ViewHomeworkDto homeworkDto = new ViewHomeworkDto(
                    homeworkId,
                    tuple.get(homework.title),
                    tuple.get(homework.description),
                    tuple.get(homework.deadline)
            );

            boolean exists = studentHomeworkRepository.existsByStudentAndHomeworkId(student, homeworkId);
            homeworkDto.setSubmit(exists);
            result.add(homeworkDto);
        }

        return result;
    }
}