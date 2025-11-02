package org.example.hello_world.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.hello_world.model.Message;

@Getter
@Setter
public class MessageDTO {
    private Integer id;
    private String message;
    public MessageDTO(Message message) {
        this.id = message.getId();
        this.message = message.getMessage();
    }
}
