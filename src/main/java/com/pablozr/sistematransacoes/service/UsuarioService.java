package com.pablozr.sistematransacoes.service;

import com.pablozr.sistematransacoes.controller.dto.LoginDTOIn;
import com.pablozr.sistematransacoes.controller.dto.LoginDTOOut;
import com.pablozr.sistematransacoes.enums.OperacaoSaldo;
import com.pablozr.sistematransacoes.exception.EmailJaRegistradoException;
import com.pablozr.sistematransacoes.exception.SenhaFracaException;
import com.pablozr.sistematransacoes.exception.UsuarioNaoEncontradoException;
import com.pablozr.sistematransacoes.model.ResetPasswordToken;
import com.pablozr.sistematransacoes.model.TokenBlackList;
import com.pablozr.sistematransacoes.model.Usuario;
import com.pablozr.sistematransacoes.repository.ResetPasswordTokenRepository;
import com.pablozr.sistematransacoes.repository.TokenBlackListRepository;
import com.pablozr.sistematransacoes.repository.UsuarioRepository;
import com.pablozr.sistematransacoes.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlackListRepository tokenBlacklistRepository;
    private final ResetPasswordTokenRepository resetPasswordTokenRepository;

    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider
    , TokenBlackListRepository tokenBlackListRepository, ResetPasswordTokenRepository resetPasswordTokenRepository){

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.resetPasswordTokenRepository = resetPasswordTokenRepository;
        this.tokenBlacklistRepository = tokenBlackListRepository;
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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String emailUsuarioLogado = auth.getName();
        Usuario usuarioLogado = usuarioRepository.findByEmail(emailUsuarioLogado)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário autenticado não encontrado"));
        if (!usuarioLogado.getId().equals(id)) {
            throw new AccessDeniedException("Você só pode atualizar seu próprio usuário");
        }
        if (usuarioAtualizado.getNome() != null && !usuarioAtualizado.getNome().isBlank()) {
            usuarioLogado.setNome(usuarioAtualizado.getNome());
        }
        if (usuarioAtualizado.getEmail() != null && !usuarioAtualizado.getEmail().isBlank() &&
                !usuarioAtualizado.getEmail().equals(usuarioLogado.getEmail())) {
            throw new IllegalArgumentException("O email não pode ser alterado por este endpoint");
        }
        return usuarioRepository.save(usuarioLogado);
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

    public List<Usuario> buscarTodos(){
        return usuarioRepository.findAll();
    }

    public void adicionarTokenBlacklist(String token){
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(1);
        TokenBlackList blacklistedToken = new TokenBlackList();
        blacklistedToken.setToken(token);
        blacklistedToken.setExpiryDate(expiryDate);
        tokenBlacklistRepository.save(blacklistedToken);
    }

    public boolean isTokenBlacklisted(String token){
        return tokenBlacklistRepository.existsByToken(token);
    }

    public String gerarTokenResetSenha(String email){
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado"));
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(30);

        ResetPasswordToken resetToken = new ResetPasswordToken();
        resetToken.setToken(token);
        resetToken.setUsuario(usuario);
        resetToken.setExpiryDate(expiryDate);
        resetPasswordTokenRepository.save(resetToken);
        return token;
    }

    public void resetarSenha(String token, String novaSenha) {
        ResetPasswordToken resetToken = resetPasswordTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido ou expirado"));
        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token expirado");
        }
        Usuario usuario = resetToken.getUsuario();
        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);
        resetPasswordTokenRepository.delete(resetToken);
    }
}

