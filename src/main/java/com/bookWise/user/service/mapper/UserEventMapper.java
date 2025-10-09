package com.bookWise.user.service.mapper;

import com.bookWise.user.service.model.entity.User;
import com.bookWise.user.service.model.enums.EventType;
import com.bookWise.user.service.model.event.UserEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserEventMapper {

    UserEventMapper INSTANCE = Mappers.getMapper(UserEventMapper.class);

    @Mapping(target = "userId", expression = "java(user.getId())")
    @Mapping(target = "userName", expression = "java(user.getName())")
    @Mapping(target = "userEmail", expression = "java(user.getEmail())")
    @Mapping(target = "userPassword", expression = "java(user.getPassword())")
    @Mapping(target = "eventType", source = "eventType")
    UserEvent toUserEvent(User user, EventType eventType);
}
