package project.homelearn.dto.teacher;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiCommentWriteDto {

    @NotNull
    private Long questionBoardId;

    @NotBlank
    private String author;

    @NotBlank
    private String content;
}