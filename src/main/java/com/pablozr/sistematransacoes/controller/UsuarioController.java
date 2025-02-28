package com.pablozr.sistematransacoes.controller;

import com.pablozr.sistematransacoes.controller.dto.UsuarioDTOIn;
import com.pablozr.sistematransacoes.controller.dto.UsuarioDTOOut;
import com.pablozr.sistematransacoes.model.Usuario;
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
import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Autowired
    public UsuarioController(UsuarioService usuarioService){
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @Operation(summary = "Cria um novo usuário", description = "Permite o cadastro de um novo usuário sem necessidade de autenticação")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou email já registrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    public ResponseEntity<UsuarioDTOOut> criarUsuario(@Valid @RequestBody UsuarioDTOIn usuarioDTO) {
        Usuario usuario = new Usuario();
        usuario.setNome(usuarioDTO.getNome());
        usuario.setEmail(usuarioDTO.getEmail());
        usuario.setSenha(usuarioDTO.getSenha());
        Usuario usuarioSalvo = usuarioService.salvarUsuario(usuario);
        return ResponseEntity.created(URI.create("/usuarios/" + usuarioSalvo.getId()))
                .body(converterParaDTO(usuarioSalvo));
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
    public ResponseEntity<List<UsuarioDTOOut>> listarUsuarios() {
        return ResponseEntity.ok(usuarioService.buscarTodos().stream().map(this::converterParaDTO).toList());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Atualiza os dados do usuário autenticado", description = "Permite que o usuário autenticado atualize seu próprio nome e email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "403", description = "Acesso negado (tentativa de atualizar outro usuário)"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    public ResponseEntity<UsuarioDTOOut> atualizarUsuario(@PathVariable Long id,@Valid @RequestBody UsuarioDTOIn usuarioDTO) {
        Usuario usuarioAtualizado = new Usuario();
        usuarioAtualizado.setNome(usuarioDTO.getNome());
        usuarioAtualizado.setEmail(usuarioDTO.getEmail());
        Usuario usuario = usuarioService.atualizarUsuario(id, usuarioAtualizado);
        return ResponseEntity.ok(converterParaDTO(usuario));
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
