package com.bookWise.user.service.publisher;

public interface EventPublisher<T> {
    void publish(T event);
}
