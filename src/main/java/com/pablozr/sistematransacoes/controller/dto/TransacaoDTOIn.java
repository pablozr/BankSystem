package com.pablozr.sistematransacoes.controller.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
@Getter
@Setter
public class TransacaoDTOIn {
    @NotNull(message = "O valor é obrigatório")
    @DecimalMin(value = "0.01", message = "O valor deve ser maior que 0")
    private BigDecimal valor;

    @NotNull(message = "O ID do destinatário é obrigatório")
    private Long destinatarioId;
}
