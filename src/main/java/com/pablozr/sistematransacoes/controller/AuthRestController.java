package com.pablozr.sistematransacoes.controller;

import com.pablozr.sistematransacoes.controller.dto.*;
import com.pablozr.sistematransacoes.model.Usuario;
import com.pablozr.sistematransacoes.security.CurrentUser;
import com.pablozr.sistematransacoes.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {
    private final UsuarioService usuarioService;
    private final JavaMailSender mailSender;

    @Autowired
    public AuthRestController(UsuarioService usuarioService, JavaMailSender mailSender){
        this.usuarioService = usuarioService;
        this.mailSender = mailSender;
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
        usuario.setAtivo(false);
        usuario.setSaldo(BigDecimal.ZERO);
        Usuario usuarioSalvo = usuarioService.salvarUsuario(usuario);

        String token = usuarioService.gerarTokenConfirmacaoEmail(usuarioSalvo.getEmail());
        enviarEmailConfirmacao(usuarioSalvo.getEmail(), token);

        return ResponseEntity.created(URI.create("/usuarios/" + usuarioSalvo.getId()))
                .body(new UsuarioDTOOut(
                        usuarioSalvo.getId(),
                        usuarioSalvo.getNome(),
                        usuarioSalvo.getEmail(),
                        usuarioSalvo.getSaldo(),
                        usuarioSalvo.getDataCriacao()
                ));
    }

    @PostMapping("/confirm-email")
    @Operation(summary = "Confirma o email do usuário (API)", description = "Ativa a conta do usuário com o token de confirmação")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email confirmado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Token inválido ou expirado")
    })
    public ResponseEntity<Void> confirmarEmail(@RequestBody ConfirmEmailDTO dto) {
        usuarioService.confirmarEmail(dto.getToken());
        return ResponseEntity.ok().build();
    }

    private void enviarEmailConfirmacao(String email, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Confirmação de Email - Banco Digital");
            message.setText("Confirme seu email com este token: " + token + "\nVálido por 24 horas.");
            mailSender.send(message);
        } catch (MailAuthenticationException e) {
            throw new RuntimeException("Falha na autenticação de email: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao enviar email de confirmação: " + e.getMessage());
        }
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

    @GetMapping("/profile")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Busca perfil do usuário autenticado (API)", description = "Retorna os dados do usuário logado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil retornado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<UsuarioDTOOut> getPerfil(@CurrentUser Usuario usuario) {
        return ResponseEntity.ok(new UsuarioDTOOut(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getSaldo()));
    }

    @PostMapping("/logout")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Realiza logout (API)", description = "Invalida o token do usuário autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logout bem-sucedido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Token de autorização inválido");
        }
        token = token.substring(7);
        usuarioService.adicionarTokenBlacklist(token); // Adiciona o token na blacklist, quando for passar no filtro jwt será barrado
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Solicita recuperação de senha (API)", description = "Envia um token de redefinição por email")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Solicitação aceita"),
            @ApiResponse(responseCode = "400", description = "Email inválido"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    public ResponseEntity<Void> forgotPassword(@RequestBody @Valid ForgotPasswordDTO dto) {
        try {
            String token = usuarioService.gerarTokenResetSenha(dto.getEmail());
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(dto.getEmail());
            message.setSubject("Redefinição de Senha - Banco Digital");
            message.setText("Use este token para redefinir sua senha: " + token + "\nVálido por 30 minutos.");
            mailSender.send(message);
            return ResponseEntity.accepted().build();
        } catch (MailAuthenticationException e) {
            throw new RuntimeException("Falha na autenticação de email: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao enviar email: " + e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Redefine a senha (API)", description = "Redefine a senha com token temporário")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Senha redefinida"),
            @ApiResponse(responseCode = "400", description = "Token inválido ou expirado")
    })
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid ResetPasswordDTO dto) {
        usuarioService.resetarSenha(dto.getToken(), dto.getNovaSenha());
        return ResponseEntity.ok().build();
    }
}
