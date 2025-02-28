package com.pablozr.sistematransacoes.repository;

import com.pablozr.sistematransacoes.model.Usuario;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    @NonNull
    List<Usuario> findAll();
}
