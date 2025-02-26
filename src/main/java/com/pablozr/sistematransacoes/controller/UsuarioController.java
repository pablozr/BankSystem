package com.pablozr.sistematransacoes.controller;

import com.pablozr.sistematransacoes.controller.dto.UsuarioDTO;
import com.pablozr.sistematransacoes.exception.UsuarioNaoEncontradoException;
import com.pablozr.sistematransacoes.model.Usuario;
import com.pablozr.sistematransacoes.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Autowired
    public UsuarioController(UsuarioService usuarioService){
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<UsuarioDTO> criarUsuario(@RequestBody Usuario usuario){
        return ResponseEntity.created(URI.create("/usuarios/" + usuarioService.salvarUsuario(usuario).getId()))
                .body(converterParaDTO(usuario));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> buscarUsuario(@PathVariable Long id){
        return usuarioService.buscarPorId(id)
                .map(this::converterParaDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDTO> atualizarUsuario(@PathVariable Long id,@RequestBody Usuario usuarioAtualizado){
        return ResponseEntity.ok(converterParaDTO(usuarioService.atualizarUsuario(id, usuarioAtualizado)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarUsuario(@PathVariable Long id){
        usuarioService.deletarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    private UsuarioDTO converterParaDTO(Usuario usuario) {
        return new UsuarioDTO(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getSaldo());
    }

}
