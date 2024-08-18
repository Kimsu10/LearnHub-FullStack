package project.homelearn.dto.student.dashboard;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

// response
@Data
public class ViewHomeworkDto {

    @NotNull
    private Long homeworkId;

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotBlank
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime deadLine;

    @NotNull
    private boolean isSubmit;

    public ViewHomeworkDto(Long homeworkId, String title, String description, LocalDateTime deadLine) {
        this.homeworkId = homeworkId;
        this.title = title;
        this.description = description;
        this.deadLine = deadLine;
    }
}
/*
List로 반환
[
    {
        "homeworkId": 6,
        "title": "title2",
        "description": "description2",
        "deadLine": "2024-08-30 10:15",
        "submit": false
    },
    {
        "homeworkId": 5,
        "title": "title",
        "description": "description",
        "deadLine": "2024-08-30 10:15",
        "submit": false
    }
]
 */