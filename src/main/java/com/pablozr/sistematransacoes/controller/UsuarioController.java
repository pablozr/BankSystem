package com.pablozr.sistematransacoes.controller;

import com.pablozr.sistematransacoes.controller.dto.UsuarioDTOOut;
import com.pablozr.sistematransacoes.model.Usuario;
import com.pablozr.sistematransacoes.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Autowired
    public UsuarioController(UsuarioService usuarioService){
        this.usuarioService = usuarioService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    @Operation(summary = "Busca um usuário por ID", description = "Retorna os detalhes de um usuário específico, restrito a administradores")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário encontrado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado (somente administradores)"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    public ResponseEntity<UsuarioDTOOut> buscarUsuario(@PathVariable Long id) {
        return usuarioService.buscarPorId(id)
                .map(this::converterParaDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lista todos os usuários", description = "Retorna a lista de todos os usuários cadastrados, restrito a administradores")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de usuários retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado (somente administradores)"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    public ResponseEntity<Page<UsuarioDTOOut>> listarUsuarios(@PageableDefault(sort = "id") Pageable pageable,
                                                              @Parameter(description = "Filtro por nome (contém)") @RequestParam(required = false) String nome,
                                                              @Parameter(description = "Filtro por email (contém)") @RequestParam(required = false) String email,
                                                              @Parameter(description = "Filtro por saldo mínimo") @RequestParam(required = false) Double saldo) {
        return ResponseEntity.ok(
                usuarioService.buscarComFiltros(pageable, nome, email, saldo)
                        .map(this::converterParaDTO)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deleta um usuário por ID", description = "Remove um usuário do sistema, restrito a administradores")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Usuário deletado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado (somente administradores)"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    public ResponseEntity<Void> deletarUsuario(@PathVariable Long id){
        usuarioService.deletarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    private UsuarioDTOOut converterParaDTO(Usuario usuario) {
        return new UsuarioDTOOut(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getSaldo());
    }

}
