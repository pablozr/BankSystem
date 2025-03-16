package com.pablozr.sistematransacoes.controller.dto;

import com.pablozr.sistematransacoes.enums.TipoTransacao;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class TransacaoDTOOut {
    private Long id;
    private TipoTransacao tipo;
    private BigDecimal valor;
    private LocalDateTime dataTransacao;
    private Long usuarioId;
    private Long destinatarioId;
    private String usuarioNome;
    private String destinatarioNome;
}
