package project.homelearn.service.student;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.homelearn.dto.student.vote.StudentVoteViewDto;
import project.homelearn.entity.curriculum.Curriculum;
import project.homelearn.entity.student.Student;
import project.homelearn.entity.vote.StudentVote;
import project.homelearn.entity.vote.Vote;
import project.homelearn.entity.vote.VoteContent;
import project.homelearn.repository.curriculum.CurriculumRepository;
import project.homelearn.repository.user.StudentRepository;
import project.homelearn.repository.vote.StudentVoteRepository;
import project.homelearn.repository.vote.VoteContentRepository;
import project.homelearn.repository.vote.VoteRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class StudentVoteService {

    private final VoteRepository voteRepository;
    private final StudentRepository studentRepository;
    private final CurriculumRepository curriculumRepository;
    private final VoteContentRepository voteContentRepository;
    private final StudentVoteRepository studentVoteRepository;

    public StudentVoteViewDto getStudentVoteView(Long voteId, String username) {
        Curriculum curriculum = curriculumRepository.findCurriculumByUsername(username);
        Integer total = studentRepository.findStudentCountByCurriculum(curriculum);

        StudentVoteViewDto result = voteRepository.findStudentVoteView(voteId, username);
        result.setTotal(total);
        return result;
    }

    // 투표 참여
    public boolean participateVote(Long voteId, String username, Map<Long, Boolean> voteResult) {
        Vote vote = voteRepository.findById(voteId).orElseThrow();
        if (validateParticipateVote(vote, voteResult)) {
            return false;
        }

        boolean participate = isParticipate(voteId, username);
        if (participate) {
            return false;
        }

        Student student = studentRepository.findByUsername(username);
        saveVote(voteResult, student, vote);
        return true;
    }

    private boolean validateParticipateVote(Vote vote, Map<Long, Boolean> voteResult) {
        LocalDateTime endTime = vote.getEndTime();
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(endTime)) {
            return true;
        }

        long trueCount = voteResult.values().stream().filter(Boolean::booleanValue).count();
        if (trueCount == 0) {
            return true;
        }

        boolean isMultipleChoice = vote.getIsMultipleChoice();
        return !isMultipleChoice && trueCount > 1;
    }

    public boolean isParticipate(Long voteId, String username) {
        Student student = studentRepository.findByUsername(username);
        return studentVoteRepository.isParticipate(voteId, student);
    }

    private void saveVote(Map<Long, Boolean> voteResult, Student student, Vote vote) {
        for (Map.Entry<Long, Boolean> entry : voteResult.entrySet()) {
            Long contentId = entry.getKey();
            Boolean voted = entry.getValue();
            if (voted) {
                VoteContent voteContent = voteContentRepository.findById(contentId).orElseThrow();
                studentVoteRepository.save(new StudentVote(student, voteContent, vote));
                voteContent.setVoteCount(voteContent.getVoteCount() + 1);
            }
        }
    }

    // 투표 수정
    public boolean modifyVote(Long voteId, String username, Map<Long, Boolean> voteResult) {
        boolean participate = isParticipate(voteId, username);
        if (!participate) {
            return false;
        }

        Vote vote = voteRepository.findById(voteId).orElseThrow();
        long trueCount = voteResult.values().stream().filter(Boolean::booleanValue).count();
        if (validateModifyVote(vote, trueCount)) {
            return false;
        }

        Student student = studentRepository.findByUsername(username);
        List<StudentVote> studentVotes = studentVoteRepository.findAllByVoteAndUser(vote, student);

        deleteVote(studentVotes); // 기존 투표 삭제

        // true count가 0이면 투표를 삭제하는 것으로 간주하고 바로 return
        if (trueCount == 0) {
            return true;
        }

        // 새로운 투표 저장
        saveVote(voteResult, student, vote);
        return true;
    }

    private boolean validateModifyVote(Vote vote, long trueCount) {
        LocalDateTime endTime = vote.getEndTime();
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(endTime)) {
            return true;
        }

        boolean isMultipleChoice = vote.getIsMultipleChoice();
        return !isMultipleChoice && trueCount > 1;
    }

    private void deleteVote(List<StudentVote> studentVotes) {
        for (StudentVote studentVote : studentVotes) {
            VoteContent voteContent = studentVote.getVoteContent();
            voteContent.setVoteCount(voteContent.getVoteCount() - 1);
            studentVoteRepository.delete(studentVote);
        }
    }
}