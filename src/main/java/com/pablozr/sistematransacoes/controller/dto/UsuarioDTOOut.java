package com.pablozr.sistematransacoes.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
public class UsuarioDTOOut {
    private Long id;
    private String nome;
    private String email;
    private BigDecimal saldo;
    private LocalDateTime dataCriacao;
}
