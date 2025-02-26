package com.pablozr.sistematransacoes.exception;

public class SaldoInsuficienteException extends  RuntimeException{
    public SaldoInsuficienteException(String message){
        super(message);
    }
}
