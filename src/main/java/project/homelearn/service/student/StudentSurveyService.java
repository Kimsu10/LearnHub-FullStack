package project.homelearn.service.student;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.homelearn.dto.student.survey.SurveyModalDto;
import project.homelearn.entity.student.Student;
import project.homelearn.entity.survey.QuestionType;
import project.homelearn.entity.survey.Survey;
import project.homelearn.entity.survey.SurveyAnswer;
import project.homelearn.entity.survey.SurveyContent;
import project.homelearn.repository.survey.SurveyAnswerRepository;
import project.homelearn.repository.survey.SurveyContentRepository;
import project.homelearn.repository.survey.SurveyRepository;
import project.homelearn.repository.user.StudentRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static project.homelearn.entity.survey.QuestionType.RATING;
import static project.homelearn.entity.survey.QuestionType.TEXT;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class StudentSurveyService {

    private final SurveyRepository surveyRepository;
    private final StudentRepository studentRepository;
    private final SurveyAnswerRepository surveyAnswerRepository;
    private final SurveyContentRepository surveyContentRepository;
    private final StudentNotificationService notificationService;

    public SurveyModalDto getSurveyModal(Long surveyId) {
        boolean finished = surveyRepository.isSurveyFinished(surveyId);
        if (finished) {
            return null;
        }
        return surveyRepository.findSurveyModal(surveyId);
    }

    public boolean participateSurvey(Long surveyId, Map<Long, String> surveyResponses, String username) {
        try {
            Student student = studentRepository.findByUsername(username);
            Survey survey = surveyRepository.findById(surveyId).orElseThrow();
            if (survey.isFinished()) {
                return false;
            }

            List<SurveyAnswer> answers = new ArrayList<>();
            for (Map.Entry<Long, String> entry : surveyResponses.entrySet()) {
                Long surveyContentId = entry.getKey();
                String answerText = entry.getValue();

                SurveyContent surveyContent = surveyContentRepository.findById(surveyContentId).orElseThrow();

                SurveyAnswer surveyAnswer = new SurveyAnswer();
                surveyAnswer.setSurvey(survey);
                surveyAnswer.setSurveyContent(surveyContent);
                surveyAnswer.setUser(student);

                QuestionType questionType = surveyContent.getQuestionType();
                if (questionType.equals(RATING)) {
                    surveyAnswer.setRating(Integer.valueOf(answerText));
                }

                if (questionType.equals(TEXT)) {
                    surveyAnswer.setTextAnswer(answerText);
                }
                answers.add(surveyAnswer);
            }

            surveyAnswerRepository.saveAll(answers);
            notificationService.deleteSurveyNotification(student, survey);
            student.setSurveyCompleted(true);
            return true;
        } catch (Exception e) {
            log.error("Error participating survey : ", e);
            return false;
        }
    }
}