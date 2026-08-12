package com.bhgroup.pms.domain;

/**
 * Which system is the source of truth for a property's availability.
 * Exactly one applies at a time - never MANUAL edits and an active iCal
 * feed at once, never iCal and a channel manager at once. Switching modes
 * is an explicit administrator action (see PropertyController), audited,
 * with no automatic fallback between modes.
 */
public enum IntegrationMode {
    /** Availability is managed by hand in BH Group; no external sync. */
    MANUAL,
    /** The hourly IcalSyncScheduler imports this property's registered feeds. */
    ICAL,
    /**
     * Reserved for when a channel manager integration exists (not yet
     * built). iCal import is disabled for the property in this mode even
     * though existing feed rows aren't deleted, so switching back to ICAL
     * doesn't require re-registering them.
     */
    CHANNEL_MANAGER
}
