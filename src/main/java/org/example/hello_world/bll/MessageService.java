package org.example.hello_world.bll;

import org.example.hello_world.dao.MessageDAO;
import org.example.hello_world.dto.MessageDTO;
import org.example.hello_world.model.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MessageService {
    @Autowired
    private MessageDAO messageDAO;

    public MessageDTO getMessageById() {
        return new MessageDTO(messageDAO.findById(1).get());
    }
}
