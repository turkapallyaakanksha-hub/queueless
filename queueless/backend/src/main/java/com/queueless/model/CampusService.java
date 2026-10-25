package com.queueless.model;

import jakarta.persistence.*;

/** A counter students can queue at (canteen, stationery, college office...). Stored in the "services" table. */
@Entity
@Table(name = "services")
public class CampusService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_name", nullable = false, unique = true, length = 100)
    private String serviceName;

    @Column(length = 500)
    private String description;

    /** Configured average time, in minutes, to serve one person. */
    @Column(name = "average_time", nullable = false)
    private int averageTime = 3;

    @Column(nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getAverageTime() { return averageTime; }
    public void setAverageTime(int averageTime) { this.averageTime = averageTime; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
