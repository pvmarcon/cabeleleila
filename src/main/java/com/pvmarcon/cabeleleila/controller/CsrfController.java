package com.pvmarcon.cabeleleila.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CsrfController {
    @GetMapping("/api/csrf")
    public ResponseEntity<Void> csrf(
            @RequestAttribute("_csrf") CsrfToken token
    ) {
        token.getToken();
        return ResponseEntity.noContent().build();
    }
}
