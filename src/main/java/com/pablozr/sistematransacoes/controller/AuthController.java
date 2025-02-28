package com.pablozr.sistematransacoes.controller;

import com.pablozr.sistematransacoes.controller.dto.LoginDTOIn;
import com.pablozr.sistematransacoes.controller.dto.LoginDTOOut;
import com.pablozr.sistematransacoes.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(summary = "Autentica um usuário", description = "Realiza o login de um usuário com email e senha, retornando um token JWT para autenticação futura")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login bem-sucedido, token retornado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos no corpo da requisição"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas (email ou senha incorretos)"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    public ResponseEntity<LoginDTOOut> login(@Valid @RequestBody LoginDTOIn loginDTO){
        return ResponseEntity.ok(usuarioService.login(loginDTO));
    }
}
