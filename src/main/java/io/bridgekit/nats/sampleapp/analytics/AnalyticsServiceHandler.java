package io.bridgekit.nats.sampleapp.analytics;

import io.bridgekit.nats.Logger;

public class AnalyticsServiceHandler implements AnalyticsService {
    private final Logger logger = Logger.instance(AnalyticsService.class);

    /**
     * @inheritDoc
     */
    @Override
    public void trackEvent(TrackEventRequest req) {
        logger.info("I spy with my event-based eye: %s", req.event);
    }
}
