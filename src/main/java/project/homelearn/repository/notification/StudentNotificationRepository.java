package project.homelearn.repository.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import project.homelearn.entity.notification.student.StudentNotification;
import project.homelearn.entity.survey.Survey;
import project.homelearn.entity.user.User;

import java.util.List;

public interface StudentNotificationRepository extends JpaRepository<StudentNotification, Long> {

    @Modifying
    void deleteAllBySurvey(Survey survey);

    @Modifying
    void deleteAllByUser(User user);

    @Modifying
    void deleteByUserAndSurvey(User user, Survey survey);

    List<StudentNotification> findAllByUser(User user);
}