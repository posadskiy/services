package com.posadskiy.email.template.core.mapper;

import com.posadskiy.email.template.api.EmailFormDto;
import com.posadskiy.email.template.core.model.*;
import com.posadskiy.user.api.UserDto;
import jakarta.inject.Singleton;

@Singleton
public class EmailFormMapper {

    public EmailForm toModel(EmailFormDto sendEmailDto, UserDto userDto) {
        return new EmailForm(
            new Email(sendEmailDto.email().subject()),
            new Recipient(userDto.username(), userDto.email()),
            new Content(sendEmailDto.content().name(), 
                sendEmailDto.content().header(), 
                sendEmailDto.content().text(), 
                new Button(sendEmailDto.content().button().text(), sendEmailDto.content().button().link()))
        );
    }
}
