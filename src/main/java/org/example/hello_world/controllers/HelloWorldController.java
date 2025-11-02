package org.example.hello_world.controllers;

import org.example.hello_world.bll.MessageService;
import org.example.hello_world.dto.MessageDTO;
import org.example.hello_world.model.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorldController {
    @Autowired
    private MessageService messageService;

    @GetMapping("/hello")
    public MessageDTO helloWorld() {
        return messageService.getMessageById();
    }
}
