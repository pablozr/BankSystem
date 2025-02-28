package com.pablozr.sistematransacoes.repository;

import com.pablozr.sistematransacoes.enums.TipoTransacao;
import com.pablozr.sistematransacoes.model.Transacao;
import com.pablozr.sistematransacoes.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {
    List<Transacao> findByUsuario(Usuario usuario);
    List<Transacao> findByUsuarioAndTipo(Usuario usuario, TipoTransacao tipo);
    List<Transacao> findByUsuarioAndDataTransacaoBetween(Usuario usuario, LocalDateTime start, LocalDateTime end);
}
