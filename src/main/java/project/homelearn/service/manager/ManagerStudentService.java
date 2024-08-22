package project.homelearn.service.manager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.homelearn.dto.manager.enroll.StudentEnrollDto;
import project.homelearn.dto.manager.manage.curriculum.CurriculumBasicDto;
import project.homelearn.dto.manager.manage.curriculum.CurriculumProgressDto;
import project.homelearn.dto.manager.manage.student.ManagerStudentDto;
import project.homelearn.dto.manager.manage.student.SpecificStudentDto;
import project.homelearn.dto.manager.manage.student.StudentUpdateDto;
import project.homelearn.entity.curriculum.Curriculum;
import project.homelearn.entity.student.Student;
import project.homelearn.entity.user.EnrollList;
import project.homelearn.entity.user.LoginHistory;
import project.homelearn.repository.curriculum.CurriculumRepository;
import project.homelearn.repository.user.EnrollListRepository;
import project.homelearn.repository.user.LoginHistoryRepository;
import project.homelearn.repository.user.StudentRepository;
import project.homelearn.repository.user.UserRepository;
import project.homelearn.service.common.EmailService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static project.homelearn.config.common.MailType.ENROLL;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ManagerStudentService {

    private final EmailService emailService;

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final CurriculumRepository curriculumRepository;
    private final EnrollListRepository enrollListRepository;
    private final LoginHistoryRepository loginHistoryRepository;

    /**
    * 학생조회
    * Author : 김승민
    * */
    // 필터링 x : 전체 학생 조회
    public Page<ManagerStudentDto> getStudents(int size, int page) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Student> studentPage = studentRepository.findAllByOrderByCreatedDateDesc(pageable);

        List<LoginHistory> todayLoginHistory = getTodayLoginHistory();
        List<ManagerStudentDto> studentDto = getManagerStudentDto(studentPage, todayLoginHistory);

        return new PageImpl<>(studentDto, pageable, studentPage.getTotalElements());
    }

    // 필터링 o : 교육과정명 기준 학생 조회
    public Page<ManagerStudentDto> getStudentsWithCurriculumName(int size, int page, String curriculumName) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Student> studentPage = studentRepository.findByCurriculumName(pageable, curriculumName);

        List<LoginHistory> todayLoginHistory = getTodayLoginHistory();
        List<ManagerStudentDto> studentDto = getManagerStudentDto(studentPage, todayLoginHistory);

        return new PageImpl<>(studentDto, pageable, studentPage.getTotalElements());
    }


    // 필터링 o : 기수 + 교육과정명 기준 학생 조회
    public Page<ManagerStudentDto> getStudentsWithCurriculumNameAndCurriculumTh(int size, int page, String curriculumName, Long curriculumTh) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Student> studentPage = studentRepository.findByCurriculumThAndCurriculumName(pageable, curriculumName, curriculumTh);

        List<LoginHistory> todayLoginHistory = getTodayLoginHistory();
        List<ManagerStudentDto> studentDto = getManagerStudentDto(studentPage, todayLoginHistory);

        return new PageImpl<>(studentDto, pageable, studentPage.getTotalElements());
    }

    // 학생 DTO 매핑 메소드
    private List<ManagerStudentDto> getManagerStudentDto(Page<Student> studentPage, List<LoginHistory> todayLoginHistory) {
        // 오늘 로그인한 학생 ID 목록을 집합으로 생성
        Set<Long> studentIdsWithLoginToday = todayLoginHistory.stream()
                .map(loginHistory -> loginHistory.getUser().getId())
                .collect(Collectors.toSet());

        // 학생 정보를 DTO로 변환
        return studentPage.stream()
                .map(student -> new ManagerStudentDto(
                        student.getId(),
                        student.getName(),
                        student.getCurriculum().getTh(),
                        student.getCurriculum().getName(),
                        student.getPhone(),
                        student.getGender(),
                        student.getEmail(),
                        studentIdsWithLoginToday.contains(student.getId())// 출석 여부
                                ))
                .collect(Collectors.toList());
    }

    // 로그인 기록이 오늘이랑 일치하는지 판단
    private List<LoginHistory> getTodayLoginHistory() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        return loginHistoryRepository.findByLoginDateTimeBetween(startOfDay, endOfDay);
    }

    /**
     * 학생 등록
     * Author : 정성진
     */
    public boolean enrollStudent(StudentEnrollDto studentEnrollDto) {
        Curriculum curriculum = curriculumRepository.findByFullName(studentEnrollDto.getCurriculumFullName());
        if (curriculum == null) {
            log.error("curriculum is null");
            return false;
        }

        String email = studentEnrollDto.getEmail();
        boolean existsByEmail = userRepository.existsByEmail(email);
        if (existsByEmail) {
            return false;
        }

        String code = emailService.sendCode(email, ENROLL);
        if (code == null) {
            return false;
        }

        EnrollList enrollList = new EnrollList();
        enrollList.setName(studentEnrollDto.getName());
        enrollList.setGender(studentEnrollDto.getGender());
        enrollList.setEmail(email);
        enrollList.setCode(code);
        enrollList.setCurriculum(curriculum);
        enrollList.setPhone(studentEnrollDto.getPhone());
        enrollListRepository.save(enrollList);
        return true;
    }

    /**
     * 학생 정보 수정
     */
    public boolean updateStudent(Long id, StudentUpdateDto studentUpdateDto) {
        try {
            Student student = studentRepository.findById(id).orElseThrow();

            student.setName(studentUpdateDto.getName());
            student.setPhone(studentUpdateDto.getPhone());
            student.setEmail(studentUpdateDto.getEmail());
            student.setGender(studentUpdateDto.getGender());
            return true;
        } catch (Exception e) {
            log.error("Error update Student : ", e);
            return false;
        }
    }

    /**
     * 학생 1명 삭제
     */
    public void deleteStudent(Long id) {
        studentRepository.deleteById(id);
    }

    /**
     * 학생 여러 명 삭제
     */
    public void deleteStudents(List<Long> ids) {
        studentRepository.deleteAllById(ids);
    }

    public CurriculumProgressDto getStudentCurriculum(Long studentId) {
        CurriculumBasicDto basicDto = studentRepository.findStudentCurriculum(studentId);
        Double progress = basicDto.calculateProgress();

        return new CurriculumProgressDto(basicDto.getName(), basicDto.getTh(), progress, basicDto.getStartDate(), basicDto.getEndDate());
    }

    public SpecificStudentDto getStudentBasic(Long studentId) {
        return studentRepository.findSpecificStudentDto(studentId);
    }
}