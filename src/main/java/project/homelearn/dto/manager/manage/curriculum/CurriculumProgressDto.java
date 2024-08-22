package project.homelearn.dto.manager.manage.curriculum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CurriculumProgressDto {

    private Long curriculumId;

    @NotBlank
    private String name;

    @NotNull
    private Long th;

    @NotNull
    private Double progress;

    private LocalDate startDate;

    private LocalDate endDate;

    public CurriculumProgressDto(String name, Long th, Double progress, LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.th = th;
        this.progress = progress;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public CurriculumProgressDto(Long curriculumId, String name, Long th, Double progress, LocalDate startDate, LocalDate endDate) {
        this.curriculumId = curriculumId;
        this.name = name;
        this.th = th;
        this.progress = progress;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}