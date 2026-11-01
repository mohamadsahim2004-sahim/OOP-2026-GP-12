package com.facultyams.util;

import java.util.List;

/** A finished, display-ready report: title, descriptive lines (filters, totals), column headers and text rows. */
public record ReportTable(String title, List<String> info, List<String> headers, List<List<String>> rows) {
}