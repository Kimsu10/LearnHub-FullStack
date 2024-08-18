package project.homelearn.service.student;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.homelearn.entity.board.QuestionBoard;
import project.homelearn.entity.board.comment.QuestionBoardComment;
import project.homelearn.entity.homework.Homework;
import project.homelearn.entity.notification.student.StudentNotification;
import project.homelearn.entity.student.Student;
import project.homelearn.entity.student.badge.Badge;
import project.homelearn.entity.survey.Survey;
import project.homelearn.entity.user.User;
import project.homelearn.repository.notification.StudentNotificationRepository;

import java.util.List;

import static project.homelearn.entity.notification.student.StudentNotificationType.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class StudentNotificationService {

    private final StudentNotificationRepository notificationRepository;
    private final StudentNotificationRepository studentNotificationRepository;

    // 매니저가 설문 등록 시 학생들에게 알림
    public void notifySurvey(Survey survey, List<Student> students) {
        for (Student student : students) {
            StudentNotification notification = new StudentNotification();
            notification.setType(SURVEY);
            notification.setUser(student);
            notification.setSurvey(survey);
            notificationRepository.save(notification);
        }
    }

    // 매니저가 설문 마감 시 알림 삭제
    public void deleteSurveyNotification(Survey survey) {
        notificationRepository.deleteAllBySurvey(survey);
    }

    // 설문 완료 시 알림 삭제
    public void deleteSurveyNotification(Student student, Survey survey) {
        notificationRepository.deleteByUserAndSurvey(student, survey);
    }

    // 과제 등록 시 학생들에게 알림
    public void homeworkNotify(Homework homework, List<Student> students) {
        for (Student student : students) {
            StudentNotification notification = new StudentNotification();
            notification.setType(HOMEWORK_UPLOADED);
            notification.setUser(student);
            notification.setHomework(homework);
            notificationRepository.save(notification);
        }
    }

    // 질문에 답변 등록 시 학생에게 알림
    public void questionResponseNotify(User student, QuestionBoard questionBoard, QuestionBoardComment questionBoardComment) {
        StudentNotification notification = new StudentNotification();
        notification.setQuestionBoard(questionBoard);
        notification.setUser(student);
        notification.setQuestionBoardComment(questionBoardComment);
        notification.setType(REPLY_TO_QUESTION);
        studentNotificationRepository.save(notification);
    }

    // 배지 얻을 시 알림
    public void badgeNotify(User student, Badge badge) {
        StudentNotification notification = new StudentNotification();
        notification.setUser(student);
        notification.setBadge(badge);
        notification.setType(BADGE);
        studentNotificationRepository.save(notification);
    }
}