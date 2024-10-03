package com.springboot.actuator.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.beans.factory.annotation.Value;

@RestController
public class HelloController {

    @Value("${deployment}")
    String deployment;

    @GetMapping("/hello")
    public String hello() {
        return "Hello Developer Sandbox for Red Hat OpenShift, this has been deployed with : " + deployment;
    }
}