package com.queueless.service;

import com.queueless.dto.Dtos.AdminQueue;
import com.queueless.dto.Dtos.QueueEntry;
import com.queueless.dto.Dtos.ServiceRequest;
import com.queueless.dto.Dtos.ServiceStatus;
import com.queueless.dto.Dtos.TokenView;
import com.queueless.exception.ApiException;
import com.queueless.model.CampusService;
import com.queueless.model.Token;
import com.queueless.model.TokenStatus;
import com.queueless.model.User;
import com.queueless.repository.CampusServiceRepository;
import com.queueless.repository.TokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** All queue rules live here: taking tokens, calling the next one, wait estimates, load and best time. */
@Service
public class QueueService {

    private static final List<TokenStatus> ACTIVE = List.of(TokenStatus.WAITING, TokenStatus.SERVING);
    private static final int HISTORY_DAYS = 30;
    private static final int MIN_SAMPLES_FOR_LEARNED_AVERAGE = 5;
    private static final int MIN_TOKENS_FOR_BEST_TIME = 20;
    private static final DateTimeFormatter HOUR_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private final TokenRepository tokens;
    private final CampusServiceRepository services;
    private final Clock clock;
    private final int openingHour;
    private final int closingHour;

    public QueueService(TokenRepository tokens,
                        CampusServiceRepository services,
                        Clock clock,
                        @Value("${queueless.opening-hour}") int openingHour,
                        @Value("${queueless.closing-hour}") int closingHour) {
        this.tokens = tokens;
        this.services = services;
        this.clock = clock;
        this.openingHour = openingHour;
        this.closingHour = closingHour;
    }

    // =====================================================================
    // Student side
    // =====================================================================

    @Transactional(readOnly = true)
    public List<ServiceStatus> listServices() {
        LocalDate today = today();
        return services.findByActiveTrueOrderByServiceNameAsc().stream()
                .map(s -> toServiceStatus(s, today))
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceStatus getService(Long id) {
        CampusService service = services.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Counter not found."));
        return toServiceStatus(service, today());
    }

    @Transactional
    public TokenView takeToken(User user, Long serviceId) {
        CampusService service = services.findByIdForUpdate(serviceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Counter not found."));
        if (!service.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    service.getServiceName() + " is not taking tokens right now.");
        }
        LocalDate today = today();
        if (tokens.existsByUser_IdAndService_IdAndQueueDateAndStatusIn(user.getId(), serviceId, today, ACTIVE)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "You already have an active token for " + service.getServiceName() + ".");
        }

        Integer max = tokens.findMaxTokenNumber(serviceId, today);
        Token token = new Token();
        token.setTokenNumber((max == null ? 0 : max) + 1);
        token.setUser(user);
        token.setService(service);
        token.setStatus(TokenStatus.WAITING);
        token.setQueueDate(today);
        token.setCreatedTime(now());
        tokens.save(token);
        return toTokenView(token, today);
    }

    @Transactional(readOnly = true)
    public List<TokenView> myTokens(User user) {
        LocalDate today = today();
        return tokens.findForUser(user.getId(), PageRequest.of(0, 50)).stream()
                .map(t -> toTokenView(t, today))
                .toList();
    }

    @Transactional
    public TokenView cancelMyToken(User user, Long tokenId) {
        Token token = tokens.findDetailed(tokenId)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Token not found."));
        if (token.getStatus() != TokenStatus.WAITING) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Only a waiting token can be cancelled. Ask the counter staff if you are already being served.");
        }
        token.setStatus(TokenStatus.CANCELLED);
        token.setCompletedTime(now());
        return toTokenView(token, today());
    }

    // =====================================================================
    // Admin side
    // =====================================================================

    @Transactional(readOnly = true)
    public List<AdminQueue> adminQueues() {
        LocalDate today = today();
        return services.findAllByOrderByServiceNameAsc().stream()
                .map(s -> toAdminQueue(s, today))
                .toList();
    }

    /** Finishes whoever is at the counter (if anyone) and calls the next waiting token. */
    @Transactional
    public AdminQueue callNext(Long serviceId) {
        CampusService service = lockService(serviceId);
        LocalDate today = today();
        LocalDateTime now = now();

        boolean finishedSomeone = false;
        for (Token current : tokens.findQueue(serviceId, today, TokenStatus.SERVING)) {
            finish(current, now);
            finishedSomeone = true;
        }

        List<Token> waiting = tokens.findQueue(serviceId, today, TokenStatus.WAITING);
        if (waiting.isEmpty()) {
            if (!finishedSomeone) {
                throw new ApiException(HttpStatus.CONFLICT, "Nobody is waiting at " + service.getServiceName() + ".");
            }
            return toAdminQueue(service, today);
        }

        Token next = waiting.get(0);
        next.setStatus(TokenStatus.SERVING);
        next.setCalledTime(now);
        tokens.flush();
        return toAdminQueue(service, today);
    }

    @Transactional
    public AdminQueue completeCurrent(Long serviceId) {
        CampusService service = lockService(serviceId);
        LocalDate today = today();
        List<Token> serving = tokens.findQueue(serviceId, today, TokenStatus.SERVING);
        if (serving.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "Nobody is being served at " + service.getServiceName() + ".");
        }
        LocalDateTime now = now();
        serving.forEach(t -> finish(t, now));
        tokens.flush();
        return toAdminQueue(service, today);
    }

    @Transactional
    public AdminQueue adminCancelToken(Long tokenId) {
        Token token = tokens.findDetailed(tokenId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Token not found."));
        CampusService service = lockService(token.getService().getId());
        if (!ACTIVE.contains(token.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "That token is already " + token.getStatus().name().toLowerCase(Locale.ROOT) + ".");
        }
        token.setStatus(TokenStatus.CANCELLED);
        token.setCompletedTime(now());
        tokens.flush();
        return toAdminQueue(service, today());
    }

    @Transactional
    public AdminQueue createService(ServiceRequest request) {
        String name = request.serviceName().trim();
        if (services.existsByServiceNameIgnoreCase(name)) {
            throw new ApiException(HttpStatus.CONFLICT, "A counter named " + name + " already exists.");
        }
        CampusService service = new CampusService();
        apply(service, request);
        services.save(service);
        return toAdminQueue(service, today());
    }

    @Transactional
    public AdminQueue updateService(Long id, ServiceRequest request) {
        CampusService service = services.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Counter not found."));
        String name = request.serviceName().trim();
        if (!service.getServiceName().equalsIgnoreCase(name) && services.existsByServiceNameIgnoreCase(name)) {
            throw new ApiException(HttpStatus.CONFLICT, "A counter named " + name + " already exists.");
        }
        apply(service, request);
        return toAdminQueue(service, today());
    }

    // =====================================================================
    // Housekeeping
    // =====================================================================

    /** Runs a little after midnight: tokens from yesterday that were never served are closed. */
    @Scheduled(cron = "0 5 0 * * *", zone = "${queueless.timezone}")
    @Transactional
    public void closeStaleTokens() {
        tokens.expireOlderThan(today());
    }

    // =====================================================================
    // Internals
    // =====================================================================

    private void apply(CampusService service, ServiceRequest request) {
        service.setServiceName(request.serviceName().trim());
        service.setDescription(request.description() == null ? null : request.description().trim());
        service.setAverageTime(request.averageTime());
        if (request.active() != null) {
            service.setActive(request.active());
        }
    }

    private CampusService lockService(Long id) {
        return services.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Counter not found."));
    }

    private void finish(Token token, LocalDateTime now) {
        token.setStatus(TokenStatus.COMPLETED);
        token.setCompletedTime(now);
        if (token.getCalledTime() != null) {
            long seconds = java.time.Duration.between(token.getCalledTime(), now).getSeconds();
            token.setDurationSeconds((int) Math.max(0, Math.min(seconds, 4 * 60 * 60)));
        }
    }

    private ServiceStatus toServiceStatus(CampusService service, LocalDate today) {
        long waiting = tokens.countByService_IdAndQueueDateAndStatus(service.getId(), today, TokenStatus.WAITING);
        Integer serving = servingNumber(service.getId(), today);
        int average = effectiveAverageMinutes(service);
        int ahead = (int) waiting + (serving != null ? 1 : 0);
        return new ServiceStatus(
                service.getId(),
                service.getServiceName(),
                service.getDescription(),
                average,
                waiting,
                serving,
                loadFor(waiting),
                ahead * average,
                bestTime(service));
    }

    private AdminQueue toAdminQueue(CampusService service, LocalDate today) {
        List<Token> serving = tokens.findQueue(service.getId(), today, TokenStatus.SERVING);
        List<QueueEntry> waiting = tokens.findQueue(service.getId(), today, TokenStatus.WAITING).stream()
                .map(this::toEntry)
                .toList();
        long served = tokens.countByService_IdAndQueueDateAndStatus(service.getId(), today, TokenStatus.COMPLETED);
        return new AdminQueue(
                service.getId(),
                service.getServiceName(),
                service.getDescription(),
                service.getAverageTime(),
                service.isActive(),
                serving.isEmpty() ? null : toEntry(serving.get(0)),
                waiting,
                served,
                loadFor(waiting.size()),
                bestTime(service));
    }

    private QueueEntry toEntry(Token token) {
        return new QueueEntry(
                token.getId(),
                token.getTokenNumber(),
                token.getUser().getName(),
                token.getCreatedTime(),
                token.getCalledTime());
    }

    private TokenView toTokenView(Token token, LocalDate today) {
        Integer serving = null;
        Integer ahead = null;
        Integer wait = null;
        boolean active = ACTIVE.contains(token.getStatus()) && today.equals(token.getQueueDate());
        if (active) {
            Long serviceId = token.getService().getId();
            serving = servingNumber(serviceId, today);
            if (token.getStatus() == TokenStatus.SERVING) {
                ahead = 0;
                wait = 0;
            } else {
                long before = tokens.countWaitingBefore(serviceId, today, token.getTokenNumber());
                ahead = (int) before + (serving != null ? 1 : 0);
                wait = ahead * effectiveAverageMinutes(token.getService());
            }
        }
        return new TokenView(
                token.getId(),
                token.getTokenNumber(),
                token.getService().getId(),
                token.getService().getServiceName(),
                token.getStatus().name(),
                token.getCreatedTime(),
                token.getCalledTime(),
                token.getCompletedTime(),
                serving,
                ahead,
                wait);
    }

    private Integer servingNumber(Long serviceId, LocalDate today) {
        return tokens.findFirstByService_IdAndQueueDateAndStatusOrderByTokenNumberDesc(
                        serviceId, today, TokenStatus.SERVING)
                .map(Token::getTokenNumber)
                .orElse(null);
    }

    /** Queue load: how crowded a counter is, based on people waiting. */
    static String loadFor(long waiting) {
        if (waiting >= 12) {
            return "HIGH";
        }
        if (waiting >= 5) {
            return "MODERATE";
        }
        return "LOW";
    }

    /**
     * Minutes per person. Starts from the configured value and switches to the real average
     * (from the last 30 days of completed tokens) once there are enough samples.
     */
    private int effectiveAverageMinutes(CampusService service) {
        List<Object[]> rows = tokens.durationStats(service.getId(), now().minusDays(HISTORY_DAYS));
        if (!rows.isEmpty()) {
            Object[] row = rows.get(0);
            if (row[0] != null && row[1] != null && ((Number) row[1]).longValue() >= MIN_SAMPLES_FOR_LEARNED_AVERAGE) {
                double seconds = ((Number) row[0]).doubleValue();
                return Math.max(1, (int) Math.round(seconds / 60.0));
            }
        }
        return Math.max(1, service.getAverageTime());
    }

    /**
     * "Best time to visit": counts how many tokens were taken in each opening hour over the last
     * 30 days and returns the quietest hour. Returns null until there is enough history.
     */
    private String bestTime(CampusService service) {
        List<Object[]> rows = tokens.countByHour(service.getId(), now().minusDays(HISTORY_DAYS));
        Map<Integer, Long> perHour = new HashMap<>();
        long total = 0;
        for (Object[] row : rows) {
            int hour = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            perHour.put(hour, count);
            total += count;
        }
        if (total < MIN_TOKENS_FOR_BEST_TIME) {
            return null;
        }
        int bestHour = -1;
        long bestCount = Long.MAX_VALUE;
        for (int hour = openingHour; hour < closingHour; hour++) {
            long count = perHour.getOrDefault(hour, 0L);
            if (count < bestCount) {
                bestCount = count;
                bestHour = hour;
            }
        }
        if (bestHour < 0) {
            return null;
        }
        return LocalTime.of(bestHour, 0).format(HOUR_FORMAT) + " \u2013 " + LocalTime.of(bestHour, 0).plusHours(1).format(HOUR_FORMAT);
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
