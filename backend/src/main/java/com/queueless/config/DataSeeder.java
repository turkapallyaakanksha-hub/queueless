package com.queueless.config;

import com.queueless.model.CampusService;
import com.queueless.model.Role;
import com.queueless.model.Token;
import com.queueless.model.TokenStatus;
import com.queueless.model.User;
import com.queueless.repository.CampusServiceRepository;
import com.queueless.repository.TokenRepository;
import com.queueless.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Creates the first admin, the sample counters and (optionally) demo data on an empty database. */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository users;
    private final CampusServiceRepository services;
    private final TokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    private final String adminName;
    private final String adminEmail;
    private final String adminPassword;
    private final boolean seedDemoData;
    private final int openingHour;
    private final int closingHour;

    public DataSeeder(UserRepository users,
                      CampusServiceRepository services,
                      TokenRepository tokens,
                      PasswordEncoder passwordEncoder,
                      Clock clock,
                      @Value("${queueless.admin.name}") String adminName,
                      @Value("${queueless.admin.email}") String adminEmail,
                      @Value("${queueless.admin.password}") String adminPassword,
                      @Value("${queueless.seed.demo-data}") boolean seedDemoData,
                      @Value("${queueless.opening-hour}") int openingHour,
                      @Value("${queueless.closing-hour}") int closingHour) {
        this.users = users;
        this.services = services;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.adminName = adminName;
        this.adminEmail = adminEmail.trim().toLowerCase(Locale.ROOT);
        this.adminPassword = adminPassword;
        this.seedDemoData = seedDemoData;
        this.openingHour = openingHour;
        this.closingHour = closingHour;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        LocalDate today = LocalDate.now(clock);
        tokens.expireOlderThan(today);

        if (!users.existsByEmail(adminEmail)) {
            User admin = new User();
            admin.setName(adminName);
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);
            users.save(admin);
            log.info("Created admin account {}", adminEmail);
        }

        if (services.count() == 0) {
            services.saveAll(List.of(
                    counter("College Office", "Admissions, fee queries and general office work.", 4),
                    counter("Stationery", "Notebooks, record books, printouts and lab manuals.", 2),
                    counter("Canteen", "Order and pick up meals without standing in line.", 3),
                    counter("ID Card Section", "New, duplicate and renewed student ID cards.", 5),
                    counter("Certificate Counter", "Bonafide, transfer and course completion certificates.", 6),
                    counter("Lab Equipment", "Issue and return of lab kits and instruments.", 4),
                    counter("Event Registration", "Sign up for fests, workshops and club events.", 2)));
            log.info("Created sample counters");
        }

        if (seedDemoData && tokens.count() == 0) {
            seedDemoHistory(today);
        }
    }

    private CampusService counter(String name, String description, int averageMinutes) {
        CampusService service = new CampusService();
        service.setServiceName(name);
        service.setDescription(description);
        service.setAverageTime(averageMinutes);
        service.setActive(true);
        return service;
    }

    /** Two weeks of completed tokens so "Best time to visit" and learned wait times have data on day one. */
    private void seedDemoHistory(LocalDate today) {
        String demoEmail = "student@queueless.edu";
        User student = users.findByEmail(demoEmail).orElseGet(() -> {
            User u = new User();
            u.setName("Demo Student");
            u.setEmail(demoEmail);
            u.setPassword(passwordEncoder.encode("Student@123"));
            u.setRole(Role.STUDENT);
            return users.save(u);
        });

        // Relative busyness for each opening hour; each counter gets its own rush-hour shape.
        int span = Math.max(1, closingHour - openingHour);
        int[][] shapes = {
                {2, 5, 8, 10, 6, 4, 6, 4, 2},
                {1, 3, 6, 12, 16, 9, 4, 3, 2},
                {3, 6, 9, 8, 5, 4, 7, 6, 3}
        };

        Random random = new Random(42);
        int shapeIndex = 0;
        for (CampusService service : services.findAllByOrderByServiceNameAsc()) {
            int[] shape = shapes[shapeIndex++ % shapes.length];
            int[] weights = new int[span];
            int totalWeight = 0;
            for (int i = 0; i < span; i++) {
                weights[i] = shape[i % shape.length];
                totalWeight += weights[i];
            }

            for (int daysAgo = 14; daysAgo >= 1; daysAgo--) {
                LocalDate day = today.minusDays(daysAgo);
                if (day.getDayOfWeek() == DayOfWeek.SUNDAY) {
                    continue;
                }
                int visitors = 18 + random.nextInt(18);
                List<LocalDateTime> arrivals = new ArrayList<>();
                for (int i = 0; i < visitors; i++) {
                    int hour = openingHour + pickIndex(weights, totalWeight, random);
                    arrivals.add(day.atTime(hour, random.nextInt(60), random.nextInt(60)));
                }
                Collections.sort(arrivals);

                List<Token> batch = new ArrayList<>();
                LocalDateTime counterFreeAt = null;
                int number = 1;
                for (LocalDateTime arrived : arrivals) {
                    LocalDateTime start = (counterFreeAt == null || counterFreeAt.isBefore(arrived))
                            ? arrived.plusMinutes(1 + random.nextInt(3))
                            : counterFreeAt;
                    int seconds = (int) (service.getAverageTime() * 60 * (0.6 + random.nextDouble() * 0.8));

                    Token token = new Token();
                    token.setTokenNumber(number++);
                    token.setUser(student);
                    token.setService(service);
                    token.setStatus(TokenStatus.COMPLETED);
                    token.setQueueDate(day);
                    token.setCreatedTime(arrived);
                    token.setCalledTime(start);
                    token.setDurationSeconds(seconds);
                    token.setCompletedTime(start.plusSeconds(seconds));
                    counterFreeAt = token.getCompletedTime();
                    batch.add(token);
                }
                tokens.saveAll(batch);
            }
        }
        log.info("Seeded demo history. Demo student login: {} / Student@123", demoEmail);
    }

    private int pickIndex(int[] weights, int totalWeight, Random random) {
        int roll = random.nextInt(totalWeight);
        for (int i = 0; i < weights.length; i++) {
            roll -= weights[i];
            if (roll < 0) {
                return i;
            }
        }
        return weights.length - 1;
    }
}
