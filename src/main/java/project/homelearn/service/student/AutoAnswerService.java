package project.homelearn.service.student;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.homelearn.entity.board.QuestionBoard;
import project.homelearn.entity.board.comment.QuestionBoardComment;
import project.homelearn.repository.board.QuestionBoardCommentRepository;
import project.homelearn.repository.board.QuestionBoardRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AutoAnswerService {

    private final ChatClient chatClient;
    private final QuestionBoardRepository questionRepository;
    private final QuestionBoardCommentRepository commentRepository;

    // 답변없는 게시물 탐색 스케줄링
    @Scheduled(fixedRate = 3600000) // 1시간마다 실행
    public void checkForUnansweredQuestions() {
        List<QuestionBoard> unansweredQuestions = findUnansweredQuestionsWithin12Hours();
        if (unansweredQuestions == null) {
            return;
        }

        for (QuestionBoard question : unansweredQuestions) {
            try {
                // AI 답변 요청
                String response = requestToChatGpt(question.getTitle() + "\n" + question.getContent());

                QuestionBoardComment comment = new QuestionBoardComment();
                comment.setUser(null);
                comment.setQuestionBoard(question);
                comment.setContent(response);
                commentRepository.save(comment);
                questionRepository.incrementCommentCountById(comment.getId());
            } catch (Exception e) {
                log.error("게시글 ID {}에 대한 AI 응답 처리 중 오류 발생.", question.getId(), e);
            }
        }
    }

    // 답변없는 게시글 불러오기
    private List<QuestionBoard> findUnansweredQuestionsWithin12Hours() {
        LocalDateTime twelveHoursAgo = LocalDateTime.now().minusHours(12);
        //LocalDateTime now  = LocalDateTime.now();
        return questionRepository.findByCreatedDateBeforeAndCommentsIsNull(twelveHoursAgo);
    }

    private String requestToChatGpt(String question) {
        return chatClient
                .prompt()
                .system("개발자가 되기 위해 공부하는 학생들이 한 질문이야. 이해하기 쉽게 잘 알려줘.")
                .user(question)
                .call()
                .content();
    }
}