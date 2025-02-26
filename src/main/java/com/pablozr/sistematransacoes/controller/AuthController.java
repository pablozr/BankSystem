package com.pablozr.sistematransacoes.controller;

import com.pablozr.sistematransacoes.controller.dto.LoginDTOIn;
import com.pablozr.sistematransacoes.controller.dto.LoginDTOOut;
import com.pablozr.sistematransacoes.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UsuarioService usuarioService;

    @Autowired
    public AuthController(UsuarioService usuarioService){
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginDTOOut> login(@Valid @RequestBody LoginDTOIn loginDTO){
        return ResponseEntity.ok(usuarioService.login(loginDTO));
    }
}
