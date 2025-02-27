package com.pablozr.sistematransacoes.service;

import com.pablozr.sistematransacoes.controller.dto.LoginDTOIn;
import com.pablozr.sistematransacoes.controller.dto.LoginDTOOut;
import com.pablozr.sistematransacoes.enums.OperacaoSaldo;
import com.pablozr.sistematransacoes.exception.EmailJaRegistradoException;
import com.pablozr.sistematransacoes.exception.SenhaFracaException;
import com.pablozr.sistematransacoes.exception.UsuarioNaoEncontradoException;
import com.pablozr.sistematransacoes.model.Usuario;
import com.pablozr.sistematransacoes.repository.UsuarioRepository;
import com.pablozr.sistematransacoes.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider){
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public Usuario salvarUsuario(Usuario usuario){
        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new EmailJaRegistradoException("Este email já está registrado.");
        }

        if (usuario.getSenha().length() < 6) {
            throw new SenhaFracaException("A senha deve ter pelo menos 6 caracteres.");
        }


        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        usuario.setRoles(Set.of("ROLE_USER"));
        return usuarioRepository.save(usuario);
    }

    public LoginDTOOut login(LoginDTOIn loginDTO) {
        Usuario usuario = usuarioRepository.findByEmail(loginDTO.getEmail())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Credenciais inválidas"));
        if (!passwordEncoder.matches(loginDTO.getSenha(), usuario.getSenha())) {
            throw new UsuarioNaoEncontradoException("Credenciais inválidas");
        }
        String token = jwtTokenProvider.generateToken(usuario.getEmail(), usuario.getId(), usuario.getRoles());
        return new LoginDTOOut(token, usuario.getId(), usuario.getNome());
    }

    public Usuario atualizarUsuario (Long id, Usuario usuarioAtualizado){
        Usuario usuarioExistente = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado"));

        usuarioExistente.setNome(usuarioAtualizado.getNome());
        usuarioExistente.setEmail(usuarioAtualizado.getEmail());
        // Can´t update password here, because it needs to be hashed

        return usuarioRepository.save(usuarioExistente);
    }

    public void deletarUsuario (Long id){
        if (!usuarioRepository.existsById(id)){
            throw new UsuarioNaoEncontradoException("Usuário não encontrado");
        }
        usuarioRepository.deleteById(id);
    }

    public Usuario alterarSenha(Long id, String novaSenha){
        Usuario usuarioExistente = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado"));
        usuarioExistente.setSenha(passwordEncoder.encode(novaSenha));
        return usuarioRepository.save(usuarioExistente);
    }

    public void atualizarSaldo(Usuario usuario, BigDecimal valor, OperacaoSaldo operacao) {
        if (operacao == OperacaoSaldo.ADICAO){
            usuario.setSaldo(usuario.getSaldo().add(valor));
        } else if (operacao == OperacaoSaldo.SUBTRACAO) {
            usuario.setSaldo(usuario.getSaldo().subtract(valor));
        }
        usuarioRepository.save(usuario);
    }

    public Optional<Usuario> buscarPorEmail(String email){
        return usuarioRepository.findByEmail(email);
    }

    public Optional<Usuario> buscarPorId(Long id){
        return usuarioRepository.findById(id);
    }
}
