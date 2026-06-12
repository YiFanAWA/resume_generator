package com.daemonsets.resumeportal.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Education {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String college;
    private String qualification;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @Column(columnDefinition = "TEXT")
    private String summary;

    public String getFormattedStartDate() {
        return startDate != null ? startDate.format(DateTimeFormatter.ofPattern("MMM yyyy")) : null;
    }

    public String getFormattedEndDate() {
        return endDate != null ? endDate.format(DateTimeFormatter.ofPattern("MMM yyyy")) : null;
    }
}
