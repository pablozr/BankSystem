package com.pablozr.sistematransacoes.controller;

import com.pablozr.sistematransacoes.controller.dto.LoginDTOIn;
import com.pablozr.sistematransacoes.controller.dto.LoginDTOOut;
import com.pablozr.sistematransacoes.controller.dto.UsuarioDTOIn;
import com.pablozr.sistematransacoes.controller.dto.UsuarioDTOOut;
import com.pablozr.sistematransacoes.model.Usuario;
import com.pablozr.sistematransacoes.security.CurrentUser;
import com.pablozr.sistematransacoes.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {
    private final UsuarioService usuarioService;

    @Autowired
    public AuthRestController(UsuarioService usuarioService){
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

    @PostMapping("/signup")
    @Operation(summary = "Cadastra um usuário (API)", description = "Cria um novo usuário no sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou email já registrado")
    })
    public ResponseEntity<UsuarioDTOOut> signup(@Valid @RequestBody UsuarioDTOIn usuarioDTO) {
        Usuario usuario = new Usuario();
        usuario.setNome(usuarioDTO.getNome());
        usuario.setEmail(usuarioDTO.getEmail());
        usuario.setSenha(usuarioDTO.getSenha());
        Usuario usuarioSalvo = usuarioService.salvarUsuario(usuario);
        return ResponseEntity.created(URI.create("/usuarios/" + usuarioSalvo.getId()))
                .body(new UsuarioDTOOut(usuarioSalvo.getId(), usuarioSalvo.getNome(), usuarioSalvo.getEmail(), usuarioSalvo.getSaldo()));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Atualiza o perfil do usuário autenticado (API)", description = "Permite ao usuário autenticado atualizar seus dados")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    public ResponseEntity<UsuarioDTOOut> atualizarPerfil(@CurrentUser Usuario usuario, @Valid @RequestBody UsuarioDTOIn usuarioDTO) {
        Usuario usuarioAtualizado = new Usuario();
        usuarioAtualizado.setNome(usuarioDTO.getNome());
        usuarioAtualizado.setEmail(usuarioDTO.getEmail());
        Usuario atualizado = usuarioService.atualizarUsuario(usuario.getId(), usuarioAtualizado);
        return ResponseEntity.ok(new UsuarioDTOOut(atualizado.getId(), atualizado.getNome(), atualizado.getEmail(), atualizado.getSaldo()));
    }
}
