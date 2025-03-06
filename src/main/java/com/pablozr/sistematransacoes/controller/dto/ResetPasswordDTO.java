package com.pablozr.sistematransacoes.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordDTO {
    @NotBlank
    private String token;
    @NotBlank
    private String novaSenha;
}
