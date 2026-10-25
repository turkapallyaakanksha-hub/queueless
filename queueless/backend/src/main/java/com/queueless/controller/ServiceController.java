package com.queueless.controller;

import com.queueless.dto.Dtos.ServiceStatus;
import com.queueless.service.QueueService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public: anyone can see the counters and how busy they are. */
@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final QueueService queueService;

    public ServiceController(QueueService queueService) {
        this.queueService = queueService;
    }

    @GetMapping
    public List<ServiceStatus> list() {
        return queueService.listServices();
    }

    @GetMapping("/{id}")
    public ServiceStatus get(@PathVariable Long id) {
        return queueService.getService(id);
    }
}
