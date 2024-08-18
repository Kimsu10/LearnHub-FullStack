package project.homelearn.service.student;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import project.homelearn.dto.common.FileDto;
import project.homelearn.dto.student.homework.*;
import project.homelearn.entity.curriculum.Curriculum;
import project.homelearn.entity.homework.Homework;
import project.homelearn.entity.homework.StudentHomework;
import project.homelearn.entity.student.Student;
import project.homelearn.dto.student.dashboard.ViewHomeworkDto;
import project.homelearn.entity.student.badge.BadgeConstants;
import project.homelearn.repository.curriculum.CurriculumRepository;
import project.homelearn.repository.homework.HomeworkRepository;
import project.homelearn.repository.homework.StudentHomeworkRepository;
import project.homelearn.repository.user.StudentRepository;
import project.homelearn.service.common.StorageService;

import java.util.List;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

import static project.homelearn.config.storage.FolderType.HOMEWORK;

/**
 * Author : 동재완
 */
@Slf4j
@Transactional
@Service
@RequiredArgsConstructor
public class StudentHomeworkService {

    private final BadgeService badgeService;
    private final StorageService storageService;
    private final StudentRepository studentRepository;
    private final HomeworkRepository homeworkRepository;
    private final CurriculumRepository curriculumRepository;
    private final StudentHomeworkRepository studentHomeworkRepository;

    public boolean submitHomework(String username, HomeworkSubmitDto homeWorkSubmitDto) {
        try {
            Student student = studentRepository.findByUsername(username);
            if (student == null) {
                throw new RuntimeException("Student not found");
            }

            Homework homework = homeworkRepository.findHomeworkAndCurriculum(homeWorkSubmitDto.getHomeworkId());
            if (homework == null) {
                throw new RuntimeException("Homework not found");
            }

            LocalDateTime deadline = homework.getDeadline();
            if (LocalDateTime.now().isAfter(deadline)) {
                return false;
            }

            boolean isDuplicate = studentHomeworkRepository.existsByStudentAndHomework(student, homework);
            if (isDuplicate) {
                throw new RuntimeException("Homework duplicate submit");
            }

            Boolean requiredFile = homework.getRequiredFile();
            MultipartFile file = homeWorkSubmitDto.getFile();
            if (requiredFile && file == null) {
                log.info("첨부파일이 필수인데 첨부하지 않았음!");
                return false;
            }

            StudentHomework studentHomework = new StudentHomework();
            studentHomework.setUser(student);
            studentHomework.setDescription(homeWorkSubmitDto.getDescription());
            studentHomework.setHomework(homework);

            if (file != null && !file.isEmpty()) {
                String folderPath = storageService.getFolderPath(student, HOMEWORK);
                FileDto fileDto = storageService.uploadFile(file, folderPath);
                studentHomework.setUploadFileName(fileDto.getOriginalFileName());
                studentHomework.setStoreFileName(fileDto.getUploadFileName());
                studentHomework.setFilePath(fileDto.getFilePath());
            }

            if (studentHomeworkRepository.countByHomework(homework) == 0) {
                badgeService.getBadge(student, BadgeConstants.HOMEWORK);
            }
            studentHomeworkRepository.save(studentHomework);
            return true;
        } catch (Exception e) {
            log.error("Error creating homework", e);
            return false;
        }
    }

    public boolean updateHomework(Long homeworkId, String username, HomeworkUpdateDto homeWorkUpdateDto) {
        try {
            StudentHomework studentHomework = studentHomeworkRepository.findById(homeworkId)
                    .orElseThrow(() -> new RuntimeException("Error getting homework" + homeworkId));

            Homework homework = studentHomework.getHomework();
            if (LocalDateTime.now().isAfter(homework.getDeadline())) {
                throw new RuntimeException("Cannot update homework after the deadline:" + homeworkId);
            }

            Boolean requiredFile = homework.getRequiredFile();
            MultipartFile file = homeWorkUpdateDto.getFile();
            if (requiredFile && file == null) {
                return false;
            }
            studentHomework.setDescription(homeWorkUpdateDto.getDescription());

            String previousFilePath = studentHomework.getFilePath();

            if (homeWorkUpdateDto.isUseDefaultFile() || file != null && !file.isEmpty()) {
                if (previousFilePath != null) {
                    storageService.deleteFile(previousFilePath);
                    studentHomework.setUploadFileName(null);
                    studentHomework.setStoreFileName(null);
                    studentHomework.setFilePath(null);
                }
            }

            Student student = studentRepository.findByUsername(username);
            if (file != null && !file.isEmpty()) {
                String folderPath = storageService.getFolderPath(student, HOMEWORK);
                FileDto fileDto = storageService.uploadFile(file, folderPath);
                studentHomework.setUploadFileName(fileDto.getOriginalFileName());
                studentHomework.setStoreFileName(fileDto.getUploadFileName());
                studentHomework.setFilePath(fileDto.getFilePath());
            }

            studentHomeworkRepository.save(studentHomework);
            return true;
        } catch (Exception e) {
            log.error("Error updating homework", e);
            return false;
        }
    }

    public boolean deleteHomework(Long homeworkId) {
        try {
            StudentHomework homework = studentHomeworkRepository.findById(homeworkId)
                    .orElseThrow(() -> new RuntimeException("Error getting homework" + homeworkId));

            LocalDateTime deadline = homework.getHomework().getDeadline();
            if (LocalDateTime.now().isAfter(deadline)) {
                throw new RuntimeException("Cannot delete homework after the deadline:" + homeworkId);
            }

            String file = homework.getFilePath();
            if (file != null) {
                storageService.deleteFile(file);
            }

            studentHomeworkRepository.deleteById(homeworkId);
            return true;
        } catch (Exception e) {
            log.error("Error deleting homework", e);
            return false;
        }
    }

    // 최근 2개 과제
    public List<ViewHomeworkDto> getHomeworkTop2(String username) {
        Curriculum curriculum = curriculumRepository.findCurriculumByUsername(username);
        Student student = studentRepository.findByUsername(username);
        return homeworkRepository.findHomeworkTop2(curriculum, student);
    }

    // 진행중인 과제 페이지 리스트 - 마감기한 기준 오름 차순
    public Page<HomeworkListDto> getHomeworkProceeding(String username, Pageable pageable) {
        Curriculum curriculum = curriculumRepository.findCurriculumByUsername(username);

        Page<Homework> homeworks = homeworkRepository.findProceedingHomework(curriculum, LocalDateTime.now(), pageable);

        List<HomeworkListDto> homeworkList = getHomeworkListDtos(homeworks);

        return new PageImpl<>(homeworkList, pageable, homeworks.getTotalElements());
    }

    // 마감된 과제들
    public Page<HomeworkListDto> getHomeworkClosed(String username, Pageable pageable) {
        Curriculum curriculum = curriculumRepository.findCurriculumByUsername(username);

        Page<Homework> homeworks = homeworkRepository.findClosedHomework(curriculum, LocalDateTime.now(), pageable);

        List<HomeworkListDto> homeworkList = getHomeworkListDtos(homeworks);

        return new PageImpl<>(homeworkList, pageable, homeworks.getTotalElements());
    }

    // 페이지 DTO 변환
    private List<HomeworkListDto> getHomeworkListDtos(Page<Homework> homeworks) {
        return homeworks.stream()
                .map(homework -> new HomeworkListDto(
                        homework.getId(),
                        homework.getTitle(),
                        homework.getDescription(),
                        homework.getDeadline(),
                        homework.getStudentHomeworks().size()))
                .collect(Collectors.toList());
    }

    // 과제 상세보기
    public StudentHomeworkDetailDto getHomeworkDetail(Long homeworkId) {
        Homework homework = homeworkRepository.findById(homeworkId).orElseThrow();

        return new StudentHomeworkDetailDto(
                homework.getId(),
                homework.getTitle(),
                homework.getDescription(),
                homework.getRequiredFile(),
                homework.getAcceptFile(),
                homework.getDeadline(),
                homework.getCreatedDate(),
                homework.getUploadFileName(),
                homework.getFilePath()
        );
    }

    // 내 제출 내역 확인
    public MySubmitDetailDto getMySubmit(Long homeworkId) {
        StudentHomework myHomework = studentHomeworkRepository.findByHomeworkId(homeworkId);
        return new MySubmitDetailDto(
                myHomework.getId(),
                myHomework.getDescription(),
                myHomework.getUploadFileName(),
                myHomework.getFilePath(),
                myHomework.getCreatedDate(),
                myHomework.getResponse(),
                myHomework.getResponseDate()
        );
    }
}