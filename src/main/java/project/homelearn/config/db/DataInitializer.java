package project.homelearn.config.db;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import project.homelearn.entity.manager.Manager;
import project.homelearn.entity.student.badge.Badge;
import project.homelearn.entity.survey.SurveyContent;
import project.homelearn.repository.badge.BadgeRepository;
import project.homelearn.repository.survey.SurveyContentRepository;
import project.homelearn.repository.user.UserRepository;

import static project.homelearn.entity.student.badge.BadgeConstants.*;
import static project.homelearn.entity.survey.QuestionType.RATING;
import static project.homelearn.entity.survey.QuestionType.TEXT;
import static project.homelearn.entity.user.Role.ROLE_MANAGER;

@Component
public class DataInitializer implements CommandLineRunner {

    private final String password;
    private final UserRepository userRepository;
    private final SurveyContentRepository surveyContentRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final BadgeRepository badgeRepository;

    public DataInitializer(@Value("${initial.password}") String password, UserRepository userRepository, SurveyContentRepository surveyContentRepository, BCryptPasswordEncoder passwordEncoder, BadgeRepository badgeRepository) {
        this.password = password;
        this.userRepository = userRepository;
        this.surveyContentRepository = surveyContentRepository;
        this.passwordEncoder = passwordEncoder;
        this.badgeRepository = badgeRepository;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername("manager")) {
            Manager manager = new Manager();
            manager.setUsername("manager");
            manager.setPassword(passwordEncoder.encode(password));
            manager.setRole(ROLE_MANAGER);
            userRepository.save(manager);
        }

        if (surveyContentRepository.count() == 0) {
            SurveyContent survey1 = new SurveyContent("교육 내용에 대한 이해도", RATING);
            SurveyContent survey2 = new SurveyContent("강사님의 교육 열의 및 준비성의 만족도", RATING);
            SurveyContent survey4 = new SurveyContent("강사님의 질의응답 만족도", RATING);
            SurveyContent survey3 = new SurveyContent("교육장 환경 및 시설에 대한 만족도", RATING);
            SurveyContent survey5 = new SurveyContent("본 과정을 주위 사람에게 추천하고 싶은 정도", RATING);
            SurveyContent survey6 = new SurveyContent("교육 전반에 대한 소감 및 개선사항에 대한 의견을 적어주세요.", TEXT);

            surveyContentRepository.save(survey1);
            surveyContentRepository.save(survey2);
            surveyContentRepository.save(survey3);
            surveyContentRepository.save(survey4);
            surveyContentRepository.save(survey5);
            surveyContentRepository.save(survey6);
        }

        if (badgeRepository.count() == 0) {
            Badge footPrint = new Badge(FOOT_PRINT, "시작이 반이다.", "foot_print.png", "badge/foot_print.png"); // ✅
            Badge explain = new Badge(EXPLAIN, "쉿, 설명 그만!", "explain.png", "badge/explain.png"); // ✅
            Badge homework = new Badge(HOMEWORK, "오.. 상당히 빠르시군요?", "homework.png", "badge/homework.png"); // ✅
            Badge knowledge = new Badge(KNOWLEDGE, "언젠간 도움이 될 거예요!", "knowledge.png", "badge/knowledge.png"); // 🥹
            Badge login7 = new Badge(LOGIN_7, "이 정도는 가뿐하죠?", "login_7.png", "badge/login_7.png"); // ✅
            Badge login30 = new Badge(LOGIN_30, "쉬엄쉬엄 하세요..", "login_30.png", "badge/login_30.png"); // ✅
            Badge night = new Badge(NIGHT, "새벽에 같이 공부할 사람!", "night.png", "badge/night.png"); // 🥹
            Badge question = new Badge(QUESTION, "뭐였더라?", "question.png", "badge/question.png"); // ✅
            Badge reviewKing = new Badge(REVIEW_KING, "배움에는 끝이 없답니다.", "review_king.png", "badge/review_king.png"); // 🥹
            Badge security = new Badge(SECURITY, "이것도 뚫어 보시죠!", "security.png", "badge/security.png"); // ✅
            Badge talk = new Badge(TALK, "이제 공부합시다!", "talk.png", "badge/talk.png"); // ✅

            badgeRepository.save(footPrint);
            badgeRepository.save(explain);
            badgeRepository.save(homework);
            badgeRepository.save(knowledge);
            badgeRepository.save(login7);
            badgeRepository.save(login30);
            badgeRepository.save(night);
            badgeRepository.save(question);
            badgeRepository.save(reviewKing);
            badgeRepository.save(security);
            badgeRepository.save(talk);
        }
    }
}