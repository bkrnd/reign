package io.reign.controller;

import io.reign.service.CycleSchedulerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @Autowired
    private CycleSchedulerService cycleSchedulerService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/cycles")
    public List<Map<String, Object>> getScheduledCycles() {
        return cycleSchedulerService.getScheduledWorldsInfo();
    }
}
