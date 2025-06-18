package io.bridgekit.nats.sampleapp.analytics;

/**
 * AnalyticsService pretends to ingest a ton of user/usage data for reporting
 * and privacy invading purposes.
 */
public interface AnalyticsService {
    /**
     * Called whenever a customer/order interaction is performed. This will store the event,
     * making it available for reporting.
     */
    void trackEvent(TrackEventRequest req);

    class TrackEventRequest {
        public String event;
        public String json;
    }
}
