package ru.irkoms.medieval.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.irkoms.medieval.controller.dto.SingleMessageDto;

@RestController
public class HomeController {

    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SingleMessageDto> home() {
        return ResponseEntity.ok(new SingleMessageDto("Hello!"));
    }
}
