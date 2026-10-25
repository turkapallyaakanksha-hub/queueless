package com.queueless.controller;

import com.queueless.dto.Dtos.AdminQueue;
import com.queueless.dto.Dtos.ServiceRequest;
import com.queueless.service.QueueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Everything under /api/admin requires the ADMIN role (see SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final QueueService queueService;

    public AdminController(QueueService queueService) {
        this.queueService = queueService;
    }

    @GetMapping("/queues")
    public List<AdminQueue> queues() {
        return queueService.adminQueues();
    }

    @PostMapping("/services/{id}/call-next")
    public AdminQueue callNext(@PathVariable Long id) {
        return queueService.callNext(id);
    }

    @PostMapping("/services/{id}/complete")
    public AdminQueue complete(@PathVariable Long id) {
        return queueService.completeCurrent(id);
    }

    @PostMapping("/tokens/{id}/cancel")
    public AdminQueue cancelToken(@PathVariable Long id) {
        return queueService.adminCancelToken(id);
    }

    @PostMapping("/services")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminQueue createService(@Valid @RequestBody ServiceRequest request) {
        return queueService.createService(request);
    }

    @PutMapping("/services/{id}")
    public AdminQueue updateService(@PathVariable Long id, @Valid @RequestBody ServiceRequest request) {
        return queueService.updateService(id, request);
    }
}
