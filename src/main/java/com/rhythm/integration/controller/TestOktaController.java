package com.rhythm.integration.controller;

import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("v1")
public class TestOktaController {

	@GetMapping("/helloworld")
	//@PreAuthorize("#oauth2.hasScope('custom_mod')")
	public Object helloWorld(HttpServletRequest httpServletRequest,Principal principal) {
		
		return "Hello World "+principal.getName()+"!";
	}

}