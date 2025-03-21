package com.pablozr.sistematransacoes.service;

import com.pablozr.sistematransacoes.controller.dto.LoginDTOIn;
import com.pablozr.sistematransacoes.controller.dto.LoginDTOOut;
import com.pablozr.sistematransacoes.enums.OperacaoSaldo;
import com.pablozr.sistematransacoes.exception.EmailJaRegistradoException;
import com.pablozr.sistematransacoes.exception.UsuarioNaoEncontradoException;
import com.pablozr.sistematransacoes.model.ConfirmacaoEmailToken;
import com.pablozr.sistematransacoes.model.ResetPasswordToken;
import com.pablozr.sistematransacoes.model.TokenBlackList;
import com.pablozr.sistematransacoes.model.Usuario;
import com.pablozr.sistematransacoes.repository.ConfirmacaoEmailTokenRepository;
import com.pablozr.sistematransacoes.repository.ResetPasswordTokenRepository;
import com.pablozr.sistematransacoes.repository.TokenBlackListRepository;
import com.pablozr.sistematransacoes.repository.UsuarioRepository;
import com.pablozr.sistematransacoes.security.JwtTokenProvider;
import com.pablozr.sistematransacoes.utils.PasswordValidator;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlackListRepository tokenBlacklistRepository;
    private final ResetPasswordTokenRepository resetPasswordTokenRepository;
    private final Cache<String, Boolean> tokenBlacklistCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();
    private final ConfirmacaoEmailTokenRepository confirmacaoEmailTokenRepository;

    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider
    , TokenBlackListRepository tokenBlackListRepository, ResetPasswordTokenRepository resetPasswordTokenRepository,
                          ConfirmacaoEmailTokenRepository confirmacaoEmailTokenRepository){

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.resetPasswordTokenRepository = resetPasswordTokenRepository;
        this.tokenBlacklistRepository = tokenBlackListRepository;
        this.confirmacaoEmailTokenRepository = confirmacaoEmailTokenRepository;
    }

    public Usuario salvarUsuario(Usuario usuario){
        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new EmailJaRegistradoException("Este email já está registrado.");
        }

        PasswordValidator.validate(usuario.getSenha());

        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        usuario.setRoles(Set.of("ROLE_USER"));
        return usuarioRepository.save(usuario);
    }

    public LoginDTOOut login(LoginDTOIn loginDTO) {
        Usuario usuario = usuarioRepository.findByEmail(loginDTO.getEmail())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Email incorreto"));
        if (!passwordEncoder.matches(loginDTO.getSenha(), usuario.getSenha())) {
            throw new UsuarioNaoEncontradoException("Senha incorreta");
        }
        String token = jwtTokenProvider.generateToken(usuario.getEmail(), usuario.getId(), usuario.getRoles());
        return new LoginDTOOut(token, usuario.getId(), usuario.getNome(), usuario.getRoles());
    }

    @PreAuthorize("#id == authentication.principal.id")
    public Usuario atualizarUsuario (Long id, Usuario usuarioAtualizado){

        Usuario usuarioLogado = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário autenticado não encontrado"));
        if (!usuarioLogado.getId().equals(id)) {
            throw new AccessDeniedException("Você só pode atualizar seu próprio usuário");
        }
        if (usuarioAtualizado.getNome() != null && !usuarioAtualizado.getNome().isBlank()) {
            usuarioLogado.setNome(usuarioAtualizado.getNome());
        }
        if (usuarioAtualizado.getEmail() != null && !usuarioAtualizado.getEmail().isBlank()) {
            if (!usuarioAtualizado.getEmail().equals(usuarioLogado.getEmail())) {
                if (usuarioRepository.existsByEmail(usuarioAtualizado.getEmail())) {
                    throw new EmailJaRegistradoException("Este email já está registrado.");
                }
                usuarioLogado.setEmail(usuarioAtualizado.getEmail());
            }
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
        PasswordValidator.validate(novaSenha);
        usuarioExistente.setSenha(passwordEncoder.encode(novaSenha));
        return usuarioRepository.save(usuarioExistente);
    }

    @Transactional
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

    public Page<Usuario> buscarTodos(Pageable pageable) {
        return usuarioRepository.findAll(pageable);
    }

    public void adicionarTokenBlacklist(String token) {
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(1);
        TokenBlackList blacklistedToken = new TokenBlackList();
        blacklistedToken.setToken(token);
        blacklistedToken.setExpiryDate(expiryDate);
        tokenBlacklistRepository.save(blacklistedToken);
        tokenBlacklistCache.put(token, true);
    }

    public boolean isTokenBlacklisted(String token) {
        Boolean cached = tokenBlacklistCache.getIfPresent(token);
        return cached != null || tokenBlacklistRepository.existsByToken(token);
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

    public String gerarTokenConfirmacaoEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado"));
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(24); // Token válido por 24 horas

        ConfirmacaoEmailToken confirmacaoToken = new ConfirmacaoEmailToken();
        confirmacaoToken.setToken(token);
        confirmacaoToken.setUsuario(usuario);
        confirmacaoToken.setExpiryDate(expiryDate);
        confirmacaoEmailTokenRepository.save(confirmacaoToken);
        return token;
    }

    public void confirmarEmail(String token) {
        ConfirmacaoEmailToken confirmacaoToken = confirmacaoEmailTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido ou expirado"));
        if (confirmacaoToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token expirado");
        }
        Usuario usuario = confirmacaoToken.getUsuario();
        usuario.setAtivo(true);
        usuarioRepository.save(usuario);
        confirmacaoEmailTokenRepository.delete(confirmacaoToken);
    }

    public void resetarSenha(String token, String novaSenha) {
        ResetPasswordToken resetToken = resetPasswordTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido ou expirado"));
        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token expirado");
        }
        PasswordValidator.validate(novaSenha);
        Usuario usuario = resetToken.getUsuario();
        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);
        resetPasswordTokenRepository.delete(resetToken);
    }

    public Page<Usuario> buscarComFiltros(Pageable pageable, String nome, String email, Double saldo) {
        Specification<Usuario> spec = Specification.where(null);
        if (nome != null && !nome.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("nome")), "%" + nome.toLowerCase() + "%"));
        }
        if (email != null && !email.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
        }
        if (saldo != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("saldo"), saldo));
        }
        return usuarioRepository.findAll(spec, pageable);
    }
}

