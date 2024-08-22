package project.homelearn.controller.student.lecture;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import project.homelearn.dto.student.lecture.StudentLectureDto;
import project.homelearn.service.student.StudentLectureService;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/students/lectures")
@RequiredArgsConstructor
public class StudentLectureController {

    private final StudentLectureService studentLectureService;

    @PatchMapping("/last-view")
    public ResponseEntity<StudentLectureDto> patchLastView(
            @RequestBody StudentLectureDto studentLectureDto) {

        Optional<StudentLectureDto> result = studentLectureService.patchLastView(studentLectureDto);

        if (result.isPresent()) {
            return ResponseEntity.ok(result.get());
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(null);
        }
    }
}
