package com.security.application.service;

import com.security.domain.model.Alert;
import com.security.shared.dto.Dtos.AlertResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class AlertService {

    public List<AlertResponse> getAll() {
        return Alert.findAllOrderedByDate().stream()
                .map(AlertResponse::from).toList();
    }

    public List<AlertResponse> getPending() {
        return Alert.findPending().stream()
                .map(AlertResponse::from).toList();
    }

    public List<AlertResponse> getByUser(Long userId) {
        return Alert.findByUser(userId).stream()
                .map(AlertResponse::from).toList();
    }

    @Transactional
    public AlertResponse acknowledge(Long alertId) {
        Alert alert = Alert.findById(alertId);
        if (alert == null) throw new NotFoundException("Alerta no encontrada");
        alert.acknowledged = true;
        alert.acknowledgedAt = LocalDateTime.now();
        alert.persist();
        return AlertResponse.from(alert);
    }
}
