package com.pablozr.sistematransacoes.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@Getter
@AllArgsConstructor
public class LoginDTOOut {
    private String token;
    private Long id;
    private String nome;
    private Set<String> roles;
}
