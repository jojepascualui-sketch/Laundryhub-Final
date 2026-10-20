package com.laundryhub.model;

/**
 * State of a rider job (rider_jobs.status).
 */
public enum JobStatus {
    /** Return delivery waiting for the laundry to be READY. */
    WAITING,
    AVAILABLE,
    ACCEPTED,
    IN_PROGRESS,
    COMPLETED,
    DECLINED,
    CANCELLED;

    /**
     * @param code database value
     * @return the status, WAITING when unknown
     */
    public static JobStatus fromCode(String code) {
        for (JobStatus s : values()) {
            if (s.name().equals(code)) {
                return s;
            }
        }
        return WAITING;
    }

    /** @return true while a rider is working on it */
    public boolean isActive() {
        return this == ACCEPTED || this == IN_PROGRESS;
    }
}
