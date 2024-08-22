package project.homelearn.service.manager;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.homelearn.dto.manager.dashboard.CurriculumDashboardDto;
import project.homelearn.dto.manager.enroll.CurriculumEnrollDto;
import project.homelearn.dto.manager.enroll.CurriculumEnrollReadyDto;
import project.homelearn.dto.manager.enroll.TeacherIdAndName;
import project.homelearn.dto.manager.manage.curriculum.*;
import project.homelearn.dto.manager.survey.CurriculumSimpleDto;
import project.homelearn.entity.curriculum.Curriculum;
import project.homelearn.entity.curriculum.CurriculumType;
import project.homelearn.entity.teacher.Teacher;
import project.homelearn.repository.curriculum.CurriculumRepository;
import project.homelearn.repository.survey.SurveyRepository;
import project.homelearn.repository.user.*;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static project.homelearn.config.storage.StorageConstants.*;
import static project.homelearn.entity.curriculum.CurriculumType.AWS;
import static project.homelearn.entity.curriculum.CurriculumType.NCP;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ManagerCurriculumService {

    private final BCryptPasswordEncoder passwordEncoder;

    private final UserRepository userRepository;
    private final SurveyRepository surveyRepository;

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ManagerRepository managerRepository;

    private final CurriculumRepository curriculumRepository;
    private final AttendanceRepository attendanceRepository;

    private final AmazonS3Client amazonS3Client;

    @Value("${bucket.name}")
    private String bucketName;

    public boolean enrollCurriculum(CurriculumEnrollDto curriculumEnrollDto) {
        try {
            CurriculumType type = curriculumEnrollDto.getType();
            Long th = curriculumRepository.findCountByType(type) + 1;

            Curriculum curriculum = createCurriculum(curriculumEnrollDto, th, type);
            curriculumRepository.save(curriculum);

            createCurriculumStorage(th, type);
            return true;

        } catch (Exception e) {
            log.error("Error adding curriculum", e);
            return false;
        }
    }

    private Curriculum createCurriculum(CurriculumEnrollDto curriculumEnrollDto, Long th, CurriculumType type) {
        Curriculum curriculum = new Curriculum();
        curriculum.setTh(th);
        curriculum.setColor(curriculumEnrollDto.getColor());
        curriculum.setStartDate(curriculumEnrollDto.getStartDate());
        curriculum.setEndDate(curriculumEnrollDto.getEndDate());
        curriculum.setType(type);

        Long teacherId = curriculumEnrollDto.getTeacherId();
        if (teacherId != null) {
            Teacher teacher = teacherRepository.findById(teacherId).orElseThrow();
            teacher.setCurriculum(curriculum);
        }

        if (type.equals(NCP)) {
            String ncp = NCP.getDescription();
            curriculum.setName(ncp);
            curriculum.setFullName(ncp + " " + th + "기");
        } else {
            String aws = AWS.getDescription();
            curriculum.setName(aws);
            curriculum.setFullName(aws + " " + th + "기");
        }
        return curriculum;
    }

    private void createCurriculumStorage(Long count, CurriculumType type) {
        String baseFolder = "";
        if (type.equals(CurriculumType.NCP)) {
            baseFolder = NCP_STORAGE_PREFIX + count;
        } else if (type.equals(CurriculumType.AWS)) {
            baseFolder = AWS_STORAGE_PREFIX + count;
        }

        createFolder(baseFolder + FREE_BOARD_STORAGE);
        createFolder(baseFolder + QUESTION_BOARD_STORAGE);
        createFolder(baseFolder + SUBJECT_STORAGE);
        createFolder(baseFolder + HOMEWORK_STORAGE);
        createFolder(baseFolder + PROFILE_STORAGE);
    }

    private void createFolder(String folderName) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(0);
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, folderName, new ByteArrayInputStream(new byte[0]), metadata);

        amazonS3Client.putObject(putObjectRequest);
    }

    public boolean updateCurriculum(Long id, CurriculumUpdateDto curriculumUpdateDto) {
        try {
            Curriculum curriculum = curriculumRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Curriculum not found with id: " + id));

            Long teacherId = curriculumUpdateDto.getTeacherId();
            if (teacherId != null) {
                Teacher teacher = teacherRepository.findById(teacherId)
                        .orElseThrow(() -> new NoSuchElementException("Teacher not found with id: " + teacherId));
                teacher.setCurriculum(curriculum);
            }

            curriculum.setColor(curriculumUpdateDto.getColor());
            curriculum.setStartDate(curriculumUpdateDto.getStartDate());
            curriculum.setEndDate(curriculumUpdateDto.getEndDate());

            return true;

        } catch (NoSuchElementException e) {
            log.error("Entity not found: ", e);
            return false;
        } catch (Exception e) {
            log.error("Error updating curriculum: ", e);
            return false;
        }
    }

    public boolean checkPassword(String username, String rawPassword) {
        String encodedPassword = managerRepository.findPasswordByUsername(username);
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public void deleteCurriculum(Long id) {
        List<Object> result = curriculumRepository.findThById(id);
        curriculumRepository.deleteById(id);

        Object[] arr = (Object[]) result.get(0);
        Long th = (Long) arr[0];
        CurriculumType type = (CurriculumType) arr[1];

        String baseFolder;
        if (type.equals(NCP)) {
            baseFolder = NCP_STORAGE_PREFIX + th + "/";
        } else {
            baseFolder = AWS_STORAGE_PREFIX + th + "/";
        }
        deleteFolder(baseFolder);
    }

    private void deleteFolder(String folderPath) {
        ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request()
                .withBucketName(bucketName)
                .withPrefix(folderPath);

        ListObjectsV2Result listObjectsV2Result;
        List<String> keysToDelete = new ArrayList<>();

        do {
            listObjectsV2Result = amazonS3Client.listObjectsV2(listObjectsV2Request);
            for (S3ObjectSummary objectSummary : listObjectsV2Result.getObjectSummaries()) {
                keysToDelete.add(objectSummary.getKey());
            }
            listObjectsV2Request.setContinuationToken(listObjectsV2Result.getNextContinuationToken());
        } while (listObjectsV2Result.isTruncated());

        if (!keysToDelete.isEmpty()) {
            DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucketName)
                    .withKeys(keysToDelete.toArray(new String[0]));
            amazonS3Client.deleteObjects(deleteObjectsRequest);
        }
    }

    public CurriculumTeacherDto getCurriculumTeacherInfo(Long id) {
        return teacherRepository.findByCurriculumId(id);
    }

    public CurriculumProgressDto getCurriculumProgress(Long curriculumId) {
        CurriculumBasicDto basicDto = curriculumRepository.findCurriculumBasic(curriculumId);
        Double progress = basicDto.calculateProgress();

        return new CurriculumProgressDto(basicDto.getCurriculumId(), basicDto.getName(), basicDto.getTh(), progress, basicDto.getStartDate(), basicDto.getEndDate());
    }

    public CurriculumAttendanceDto getCurriculumAttendance(Long curriculumId) {
        Curriculum curriculum = curriculumRepository.findById(curriculumId).orElseThrow();
        Integer total = studentRepository.findStudentCountByCurriculum(curriculum);
        Long attendance = studentRepository.findAttendanceCount(curriculum);

        CurriculumAttendanceDto attendanceDto = new CurriculumAttendanceDto();
        attendanceDto.setTotal(total);
        attendanceDto.setAttendance(attendance);

        if (total != 0) {
            attendanceDto.setRatio(Math.floorDiv(attendance.intValue() * 100, total));
        } else {
            attendanceDto.setRatio(0);
        }
        return attendanceDto;
    }

    public CurriculumSurveyDto getProgressSurvey(Long curriculumId) {
        return surveyRepository.findProgressSurvey(curriculumId);
    }

    /**
     * 대시보드 교육과정 section
     * Author : 김승민
     * 0. NCP/AWS 따로 추출 ✅
     * 1. 교육과정이름 + 기수 ✅
     * 2. 강사명 ✅
     * 3. 학생총원 ✅
     * 4. 출석한 학생인원 ✅
     * */
    public List<CurriculumDashboardDto> getCurriculumList(CurriculumType type) {
        List<Curriculum> curriculums = curriculumRepository.findByCurriculumType(type);
        LocalDateTime now = LocalDateTime.now();

        return curriculums.stream()
                .map(curriculum -> {
                    CurriculumDashboardDto curriculumDashboardDto = new CurriculumDashboardDto();
                    curriculumDashboardDto.setId(curriculum.getId());
                    curriculumDashboardDto.setName(curriculum.getName());
                    curriculumDashboardDto.setTh(curriculum.getTh());
                    curriculumDashboardDto.setTeacherName(userRepository.findTeacherNameByCurriculumId(curriculum.getId()));
                    curriculumDashboardDto.setAttendance(attendanceRepository.countAttendanceByCurriculumId(curriculum.getId(), now.toLocalDate()));
                    curriculumDashboardDto.setTotal(userRepository.countTotalStudentsByCurriculumId(curriculum.getId()));

                    return curriculumDashboardDto;
                })
                .collect(Collectors.toList());
    }

    public List<CurriculumTypeAndTh> getCurriculumTypeAndTh() {
        return curriculumRepository.findCurriculumTypeAndTh();
    }

    public CurriculumSimpleDto getCurriculumSimple(Long curriculumId) {
        return curriculumRepository.findCurriculumSimple(curriculumId);
    }

    public List<String> getCurriculumColor() {
        return curriculumRepository.findAllColor();
    }

    public CurriculumEnrollReadyDto getCurriculumEnrollReady() {
        CurriculumEnrollReadyDto result = new CurriculumEnrollReadyDto();
        List<String> curriculumColor = getCurriculumColor();
        result.setColors(curriculumColor);

        List<TeacherIdAndName> teacherIdsAndNames = teacherRepository.findTeacherIdsAndNames();
        result.setTeachers(teacherIdsAndNames);
        return result;
    }
}