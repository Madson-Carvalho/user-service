package com.bookWise.user.service.model.event;

import com.bookWise.user.service.model.enums.EventType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.io.Serializable;
import java.util.UUID;

public record UserEvent(UUID userId, String userName, String userEmail, String userPassword,
                        @Enumerated(EnumType.STRING) EventType eventType) implements Serializable {
}
