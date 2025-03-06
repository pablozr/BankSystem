package com.pablozr.sistematransacoes.repository;

import com.pablozr.sistematransacoes.model.TokenBlackList;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenBlackListRepository extends JpaRepository<TokenBlackList, Long> {
    boolean existsByToken(String token);
}
