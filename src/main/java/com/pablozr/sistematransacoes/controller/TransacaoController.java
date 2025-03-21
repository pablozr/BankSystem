package com.pablozr.sistematransacoes.controller;

import com.pablozr.sistematransacoes.controller.dto.DepositoDTOIn;
import com.pablozr.sistematransacoes.controller.dto.TransacaoDTOIn;
import com.pablozr.sistematransacoes.controller.dto.TransacaoDTOOut;
import com.pablozr.sistematransacoes.enums.TipoTransacao;
import com.pablozr.sistematransacoes.exception.UsuarioNaoEncontradoException;
import com.pablozr.sistematransacoes.model.Transacao;
import com.pablozr.sistematransacoes.model.Usuario;
import com.pablozr.sistematransacoes.security.CurrentUser;
import com.pablozr.sistematransacoes.service.TransacaoService;
import com.pablozr.sistematransacoes.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transacoes")
public class TransacaoController {
    private final TransacaoService transacaoService;
    private final UsuarioService usuarioService;

    @Autowired
    public TransacaoController(TransacaoService transacaoService, UsuarioService usuarioService){
        this.transacaoService = transacaoService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/deposito")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Realiza um depósito na conta do usuário autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Depósito realizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Valor inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<TransacaoDTOOut> depositar(@Valid @RequestBody DepositoDTOIn DepositoDTO, @CurrentUser Usuario usuario){
        Transacao transacao = transacaoService.deposito(usuario, DepositoDTO.getValor());
        return ResponseEntity.ok(converterParaDTO(transacao));
    }
    @PostMapping("/transferencia")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Realiza uma transferência entre o usuário autenticado e outro usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transferência realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Valor inválido ou saldo insuficiente"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Destinatário não encontrado")
    })
    public ResponseEntity<TransacaoDTOOut> transferir(@Valid @RequestBody TransacaoDTOIn transferenciaDTO, @CurrentUser Usuario remetente){
        Usuario destinatario = usuarioService.buscarPorId(transferenciaDTO.getDestinatarioId())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Destinatário não encontrado"));
        Transacao transacao = transacaoService.tranferencia(remetente, destinatario, transferenciaDTO.getValor());
        return ResponseEntity.ok(converterParaDTO(transacao));
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Lista todas as transações do usuário autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de transações retornada"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<Page<TransacaoDTOOut>> listarTransacoes(@CurrentUser Usuario usuario, Pageable pageable, TipoTransacao tipo, LocalDateTime dataInicio, LocalDateTime dataFim) {
        return ResponseEntity.ok(transacaoService.listarTransacoes(usuario, pageable, tipo, dataInicio, dataFim).map(this::converterParaDTO));
    }

    private TransacaoDTOOut converterParaDTO(Transacao transacao) {
        return new TransacaoDTOOut(
                transacao.getId(),
                transacao.getTipo(),
                transacao.getValor(),
                transacao.getDataTransacao(),
                transacao.getUsuario().getId(),
                transacao.getDestinatario() != null ? transacao.getDestinatario().getId() : null, // Long para destinatarioId
                transacao.getUsuario().getNome(),
                transacao.getDestinatario() != null ? transacao.getDestinatario().getNome() : null // String para destinatarioNome
        );
    }
}
