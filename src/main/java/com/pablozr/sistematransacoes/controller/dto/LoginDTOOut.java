package com.pablozr.sistematransacoes.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginDTOOut {
    private String token;
    private Long id;
    private String nome;
}
