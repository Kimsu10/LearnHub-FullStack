package project.homelearn.dto.common.board;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class QuestionBoardCommentDto {

    @NotNull
    private Long commentId;

    @NotBlank
    private String author;

    @NotBlank
    private String profileImageName;

    @NotBlank
    private String content;

    @NotBlank
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime createTime;

    private List<QuestionBoardCommentDto> replies;

    public QuestionBoardCommentDto(Long commentId, String author, String content, LocalDateTime createTime) {
        this.commentId = commentId;
        this.author = author;
        this.content = content;
        this.createTime = createTime;
    }
}