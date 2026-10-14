package com.posadskiy.auth.core.service;

import com.posadskiy.auth.core.storage.db.AuditEventRepository;
import com.posadskiy.auth.core.storage.db.entity.AuditEventEntity;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class AuditService {

    private static final Logger LOG = LoggerFactory.getLogger(AuditService.class);

    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    public void record(Long userId, String eventType, String detail) {
        try {
            AuditEventEntity entity = new AuditEventEntity();
            entity.setUserId(userId);
            entity.setEventType(eventType);
            entity.setDetail(detail);
            repository.save(entity);
        } catch (Exception e) {
            LOG.warn("Failed to persist audit event type={}", eventType, e);
        }
        LOG.info("audit eventType={} userId={} detail={}", eventType, userId, detail);
    }
}
