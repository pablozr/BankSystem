package com.pablozr.sistematransacoes.service;

import com.pablozr.sistematransacoes.enums.OperacaoSaldo;
import com.pablozr.sistematransacoes.exception.EmailJaRegistradoException;
import com.pablozr.sistematransacoes.exception.SenhaFracaException;
import com.pablozr.sistematransacoes.exception.UsuarioNaoEncontradoException;
import com.pablozr.sistematransacoes.model.Usuario;
import com.pablozr.sistematransacoes.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder){
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario salvarUsuario(Usuario usuario){
        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new EmailJaRegistradoException("Este email já está registrado.");
        }

        if (usuario.getSenha().length() < 6) {
            throw new SenhaFracaException("A senha deve ter pelo menos 6 caracteres.");
        }


        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        return usuarioRepository.save(usuario);
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
