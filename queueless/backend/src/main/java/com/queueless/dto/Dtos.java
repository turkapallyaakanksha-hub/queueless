package com.queueless.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/** Request and response shapes for the REST API. */
public final class Dtos {

    private Dtos() {
    }

    // ---------- Auth ----------

    public record RegisterRequest(
            @NotBlank(message = "Enter your name.") @Size(max = 100, message = "Name is too long.") String name,
            @NotBlank(message = "Enter your email.") @Email(message = "Enter a valid email address.") String email,
            @NotBlank(message = "Choose a password.")
            @Size(min = 6, max = 100, message = "Password must be at least 6 characters.") String password) {
    }

    public record LoginRequest(
            @NotBlank(message = "Enter your email.") @Email(message = "Enter a valid email address.") String email,
            @NotBlank(message = "Enter your password.") String password) {
    }

    public record UserView(Long id, String name, String email, String role) {
    }

    public record AuthResponse(String token, UserView user) {
    }

    // ---------- Services (counters) ----------

    public record ServiceRequest(
            @NotBlank(message = "Enter a counter name.") @Size(max = 100, message = "Counter name is too long.")
            String serviceName,
            @Size(max = 500, message = "Description is too long.") String description,
            @NotNull(message = "Enter the average time per person.")
            @Min(value = 1, message = "Average time must be at least 1 minute.")
            @Max(value = 120, message = "Average time must be 120 minutes or less.") Integer averageTime,
            Boolean active) {
    }

    /** What students see for a counter. averageTime is the effective value (learned from history when available). */
    public record ServiceStatus(
            Long id,
            String serviceName,
            String description,
            int averageTime,
            long waitingCount,
            Integer currentlyServing,
            String load,
            int estimatedWaitMinutes,
            String bestTime) {
    }

    // ---------- Tokens ----------

    public record TakeTokenRequest(@NotNull(message = "Choose a counter.") Long serviceId) {
    }

    public record TokenView(
            Long id,
            int tokenNumber,
            Long serviceId,
            String serviceName,
            String status,
            LocalDateTime createdTime,
            LocalDateTime calledTime,
            LocalDateTime completedTime,
            Integer currentlyServing,
            Integer peopleAhead,
            Integer estimatedWaitMinutes) {
    }

    // ---------- Admin ----------

    public record QueueEntry(
            Long id,
            int tokenNumber,
            String userName,
            LocalDateTime createdTime,
            LocalDateTime calledTime) {
    }

    public record AdminQueue(
            Long serviceId,
            String serviceName,
            String description,
            int averageTime,
            boolean active,
            QueueEntry serving,
            List<QueueEntry> waiting,
            long servedToday,
            String load,
            String bestTime) {
    }

    // ---------- Errors ----------

    public record ErrorResponse(String message) {
    }
}
