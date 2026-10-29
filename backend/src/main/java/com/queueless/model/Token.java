package com.queueless.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One place in a queue. Token numbers restart at 1 every day for each service,
 * so (service, queueDate, tokenNumber) is unique.
 */
@Entity
@Table(
        name = "tokens",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_token_service_day_number",
                columnNames = {"service_id", "queue_date", "token_number"}),
        indexes = {
                @Index(name = "idx_tokens_service_day_status", columnList = "service_id, queue_date, status"),
                @Index(name = "idx_tokens_service_created", columnList = "service_id, created_time"),
                @Index(name = "idx_tokens_user", columnList = "user_id")
        })
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_number", nullable = false)
    private int tokenNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private CampusService service;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TokenStatus status = TokenStatus.WAITING;

    @Column(name = "queue_date", nullable = false)
    private LocalDate queueDate;

    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;

    /** When the admin called this token to the counter. */
    @Column(name = "called_time")
    private LocalDateTime calledTime;

    @Column(name = "completed_time")
    private LocalDateTime completedTime;

    /** Seconds spent at the counter; used to learn the real average service time. */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getTokenNumber() { return tokenNumber; }
    public void setTokenNumber(int tokenNumber) { this.tokenNumber = tokenNumber; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public CampusService getService() { return service; }
    public void setService(CampusService service) { this.service = service; }

    public TokenStatus getStatus() { return status; }
    public void setStatus(TokenStatus status) { this.status = status; }

    public LocalDate getQueueDate() { return queueDate; }
    public void setQueueDate(LocalDate queueDate) { this.queueDate = queueDate; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public LocalDateTime getCalledTime() { return calledTime; }
    public void setCalledTime(LocalDateTime calledTime) { this.calledTime = calledTime; }

    public LocalDateTime getCompletedTime() { return completedTime; }
    public void setCompletedTime(LocalDateTime completedTime) { this.completedTime = completedTime; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
}
