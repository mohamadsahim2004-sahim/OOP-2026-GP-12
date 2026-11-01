package com.facultyams.model;

/**
 * Sealed base interface for course activities.
 */
public sealed interface Activity permits Announcement, Resource, Assignment {
    String id();
    String title();
}
