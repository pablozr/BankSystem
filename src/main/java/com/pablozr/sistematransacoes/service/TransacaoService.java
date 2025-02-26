package com.pablozr.sistematransacoes.service;

import com.pablozr.sistematransacoes.enums.OperacaoSaldo;
import com.pablozr.sistematransacoes.enums.TipoTransacao;
import com.pablozr.sistematransacoes.exception.SaldoInsuficienteException;
import com.pablozr.sistematransacoes.exception.ValorNegativoException;
import com.pablozr.sistematransacoes.model.Transacao;
import com.pablozr.sistematransacoes.model.Usuario;
import com.pablozr.sistematransacoes.repository.TransacaoRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
@Service
public class TransacaoService {
    private final TransacaoRepository transacaoRepository;
    private final UsuarioService usuarioService;

    @Autowired
    public TransacaoService(TransacaoRepository transacaoRepository, UsuarioService usuarioService){
        this.transacaoRepository = transacaoRepository;
        this.usuarioService = usuarioService;
    }

    public Transacao registrarTransacao(Transacao transacao){
        return transacaoRepository.save(transacao);
    }

    public List<Transacao> listarTransacoes(Usuario usuario){
        return transacaoRepository.findByUsuario(usuario);
    }

    public Transacao deposito(Usuario usuario, BigDecimal valor){
        if (valor.compareTo(BigDecimal.ZERO) < 0){
            throw new ValorNegativoException("O valor do depósito deve ser positivo");
        }
        Transacao transacao = new Transacao();
        transacao.setTipo(TipoTransacao.DEPOSITO);
        transacao.setValor(valor);
        transacao.setUsuario(usuario);
        transacao.setDataTransacao(LocalDateTime.now());

        usuarioService.atualizarSaldo(usuario, valor, OperacaoSaldo.ADICAO);

        return transacaoRepository.save(transacao);
    }
    @Transactional(rollbackOn = Exception.class)
    public Transacao tranferencia(Usuario remetente, Usuario destinatario, BigDecimal valor){
        if (valor.compareTo(BigDecimal.ZERO) <= 0){
            throw new ValorNegativoException("O valor da transferência deve ser positivo");
        }

        if (remetente.getSaldo().compareTo(valor) < 0){
            throw new SaldoInsuficienteException("Saldo insuficiente");
        }

        Transacao transacao = new Transacao();
        transacao.setTipo(TipoTransacao.TRANSFERENCIA);
        transacao.setValor(valor);
        transacao.setUsuario(remetente);
        transacao.setDestinatario(destinatario);
        transacao.setDataTransacao(LocalDateTime.now());

        usuarioService.atualizarSaldo(remetente, valor, OperacaoSaldo.SUBTRACAO);
        usuarioService.atualizarSaldo(destinatario, valor, OperacaoSaldo.ADICAO);

        return transacaoRepository.save(transacao);
    }
}
