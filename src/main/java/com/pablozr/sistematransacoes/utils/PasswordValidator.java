package com.pablozr.sistematransacoes.utils;

import com.pablozr.sistematransacoes.exception.SenhaFracaException;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public final class PasswordValidator {
    public static void validate(String password){
        if (password == null || password.isBlank()) {
            throw new SenhaFracaException("A senha não pode ser nula ou vazia.");
        }

        if (password.length() < 8) {
            throw new SenhaFracaException("A senha deve ter pelo menos 8 caracteres.");
        }

        if (!password.matches(".*[A-Z].*")) {
            throw new SenhaFracaException("A senha deve conter pelo menos uma letra maiúscula.");
        }

        if (!password.matches(".*[0-9].*")) {
            throw new SenhaFracaException("A senha deve conter pelo menos um número.");
        }

        if (!password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
            throw new SenhaFracaException("A senha deve conter pelo menos um caractere especial (ex.: !@#$%).");
        }
    }
}
