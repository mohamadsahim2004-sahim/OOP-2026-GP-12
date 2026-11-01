package com.facultyams.model;

import com.facultyams.util.ReportTable;

import java.util.Map;

/** A generated report: the printable/exportable table, a one-line summary and the values for the chart. */
public record ReportResult(ReportTable table, String summary, Map<String, Integer> chart, String chartTitle) {
}