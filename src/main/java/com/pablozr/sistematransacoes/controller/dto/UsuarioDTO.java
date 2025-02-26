package com.pablozr.sistematransacoes.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
@AllArgsConstructor
@Getter
public class UsuarioDTO {
    private Long id;
    private String nome;
    private String email;
    private BigDecimal saldo;
}
