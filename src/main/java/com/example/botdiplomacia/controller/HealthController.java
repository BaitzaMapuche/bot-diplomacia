package com.example.botdiplomacia.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint publico y liviano para que un servicio externo (UptimeRobot,
 * cron-job.org, etc.) le haga ping al bot y evite que Render lo duerma por
 * inactividad. No toca la base de datos ni requiere autenticacion.
 */
@RestController
public class HealthController {
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
