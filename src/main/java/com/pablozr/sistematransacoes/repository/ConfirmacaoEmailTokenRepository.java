package com.pablozr.sistematransacoes.repository;

import com.pablozr.sistematransacoes.model.ConfirmacaoEmailToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfirmacaoEmailTokenRepository extends JpaRepository<ConfirmacaoEmailToken, Long> {
    Optional<ConfirmacaoEmailToken> findByToken(String token);
}
