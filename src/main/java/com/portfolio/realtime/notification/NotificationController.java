package com.portfolio.realtime.notification;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/**
 * Ingress for new notifications. Accepts the request, stamps an id/timestamp, and hands it to
 * the publisher. Returns 202 Accepted because delivery is asynchronous and best-effort at this
 * milestone (durable delivery: ADR-0003).
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationPublisher publisher;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public NotificationMessage publish(@Valid @RequestBody PublishRequest request) {
        NotificationMessage message = new NotificationMessage(
                UUID.randomUUID().toString(),
                request.userId(),
                request.type() == null || request.type().isBlank() ? "GENERIC" : request.type(),
                request.content(),
                Instant.now());
        publisher.publish(message);
        return message;
    }

    /** {@code userId} optional (blank = broadcast); {@code content} required. */
    public record PublishRequest(String userId, String type, @NotBlank String content) {
    }
}
