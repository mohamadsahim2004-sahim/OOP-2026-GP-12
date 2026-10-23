package com.facultyams.service;

import com.facultyams.model.results.ResultGrade;
import com.facultyams.model.results.ResultStatusFilter;
import com.facultyams.service.ResultService.CourseResult;
import java.util.Locale;

/** Immutable combined filter for course-code/name, exact grade, and status. */
public record ResultFilter(String searchText, ResultGrade selectedGrade,
                           ResultStatusFilter selectedStatus) {
    public ResultFilter {
        searchText = searchText == null ? "" : searchText.trim().toLowerCase(Locale.ROOT);
        selectedGrade = selectedGrade == null ? ResultGrade.ALL : selectedGrade;
        selectedStatus = selectedStatus == null ? ResultStatusFilter.ALL : selectedStatus;
    }

    /** Returns true when a result matches every active filter. */
    public boolean matches(CourseResult result) {
        if (!searchText.isEmpty()
                && !result.code().toLowerCase(Locale.ROOT).contains(searchText)
                && !result.name().toLowerCase(Locale.ROOT).contains(searchText)) {
            return false;
        }
        if (selectedGrade != ResultGrade.ALL && !selectedGrade.value().equals(result.grade())) {
            return false;
        }
        return selectedStatus == ResultStatusFilter.ALL
                || selectedStatus.status() == result.status();
    }

    public boolean isActive() {
        return !searchText.isEmpty() || selectedGrade != ResultGrade.ALL
                || selectedStatus != ResultStatusFilter.ALL;
    }
}
