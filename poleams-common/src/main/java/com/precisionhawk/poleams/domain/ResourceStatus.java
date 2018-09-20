/*
 * All rights reserved.
 */

package com.precisionhawk.poleams.domain;

/**
 *
 * @author <a href="mailto:pchapman@pcsw.us">Philip A. Chapman</a>
 */
public enum ResourceStatus {
    /** Queued for upload */
    QueuedForUpload,
    /** Uploaded but not yet user-processed (Cropped and the like) */
    Uploaded,
    /** Processed by the user, but not yet ready to be displayed. */
    Processed,
    /** Processed by the user and ready to be displayed. */
    Released,
    /** No longer displayed.  Archived. */
    Archived,
    /** Not to b displayed. */
    NotForDisplay,
    // Unable to zoomify image
    ErrorProcessing
}
