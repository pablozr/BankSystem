package com.pablozr.sistematransacoes.repository;

import com.pablozr.sistematransacoes.model.Usuario;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    Page<Usuario> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
    Page<Usuario> findByEmailContainingIgnoreCase(String email, Pageable pageable);
    Page<Usuario> findBySaldoGreaterThanEqual(Double saldo, Pageable pageable);

    Page<Usuario> findAll(Specification<Usuario> spec, Pageable pageable);
}
