package com.portfolio.realtime.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Exposes which instance is serving the caller. The demo client shows this so you can connect
 * two browser tabs to two ports and watch a single publish fan out across both instances.
 */
@RestController
public class InstanceController {

    @Value("${app.instance-id}")
    private String instanceId;

    @GetMapping("/api/instance")
    public Map<String, String> instance() {
        return Map.of("instanceId", instanceId);
    }
}
