package com.devops.justos.hellospring.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/message")
public class HelloController {
    @GetMapping
    public String returnMessage(){
        return "<h1> Hello Devops Justos </h1>";
    }
}
