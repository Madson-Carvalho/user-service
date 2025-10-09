package com.bookWise.user.service.publisher;

import com.bookWise.user.service.config.RabbitConfig;
import com.bookWise.user.service.exception.EventPublishException;
import com.bookWise.user.service.model.event.UserEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventPublisher implements EventPublisher<UserEvent> {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(UserEvent event) {
        try {
            CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());

            rabbitTemplate.convertAndSend(
                    RabbitConfig.USER_EXCHANGE,
                    "user.event",
                    event,
                    message -> {
                        message.getMessageProperties().setCorrelationId(correlationData.getId());
                        return message;
                    },
                    correlationData
            );

            log.info("Evento de usuário publicado com sucesso. ID de correlação: {}", correlationData.getId());

        } catch (Exception e) {
            log.error("Erro ao publicar evento de usuário: {}", event.userId(), e);
            throw new EventPublishException("Falha ao publicar evento de usuário", e);
        }
    }
}
