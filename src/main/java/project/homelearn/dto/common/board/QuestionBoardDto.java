package project.homelearn.dto.common.board;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class QuestionBoardDto {

    @NotNull
    private Long questionBoardId;

    private String subjectName; //기타 질문일수도 있으니

    @NotBlank
    private String title;

    @NotBlank
    private String name;

    @NotBlank
    private String content;

    @NotNull
    private LocalDateTime createDate;

    private int commentCount;

    @NotNull
    private boolean isCommentHere;

}