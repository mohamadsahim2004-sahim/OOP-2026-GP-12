package com.srms.model;

/**
 * Course learning resource (PDF, DOCX, PPTX, ZIP, LINK).
 */
public record Resource(
        String id,
        String title,
        String fileType,
        String path
) implements Activity {}
