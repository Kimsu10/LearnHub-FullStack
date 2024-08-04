package project.homelearn.service.teacher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.homelearn.dto.teacher.vote.VoteTabDto;
import project.homelearn.entity.curriculum.Curriculum;
import project.homelearn.repository.curriculum.CurriculumRepository;
import project.homelearn.repository.vote.StudentVoteRepository;
import project.homelearn.repository.vote.VoteContentRepository;
import project.homelearn.repository.vote.VoteRepository;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TeacherVoteService {

    private final VoteRepository voteRepository;
    private final VoteContentRepository voteContentRepository;
    private final StudentVoteRepository studentVoteRepository;
    private final CurriculumRepository curriculumRepository;

    public Page<VoteTabDto> getProgressVotes(String username, int page, int size, String status) {
        Curriculum curriculum = curriculumRepository.findCurriculumByTeacher(username);
        PageRequest pageRequest = PageRequest.of(page, size);

        return null;
    }

    public Page<VoteTabDto> getCompletedVotes(String username, int page, int size, String status) {
        Curriculum curriculum = curriculumRepository.findCurriculumByTeacher(username);
        PageRequest pageRequest = PageRequest.of(page, size);

        return null;
    }
}