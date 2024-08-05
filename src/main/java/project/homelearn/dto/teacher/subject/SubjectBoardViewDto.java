package project.homelearn.dto.teacher.subject;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.querydsl.core.annotations.QueryProjection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

// response
@Data
public class SubjectBoardViewDto {

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    private int viewCount;

    @NotBlank
    private String filePath;

    @NotBlank
    private String fileName;

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDateTime writeDate;

    @QueryProjection
    public SubjectBoardViewDto(String title, String content, int viewCount, String filePath, String fileName, LocalDateTime writeDate) {
        this.title = title;
        this.content = content;
        this.viewCount = viewCount;
        this.filePath = filePath;
        this.fileName = fileName;
        this.writeDate = writeDate;
    }
}