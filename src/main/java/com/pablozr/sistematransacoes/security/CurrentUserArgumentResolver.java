package com.pablozr.sistematransacoes.security;

import com.pablozr.sistematransacoes.exception.UsuarioNaoEncontradoException;
import com.pablozr.sistematransacoes.model.Usuario;
import com.pablozr.sistematransacoes.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {
    private final UsuarioService usuarioService;

    @Autowired
    public CurrentUserArgumentResolver(UsuarioService usuarioService){
        this.usuarioService = usuarioService;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter){
        return parameter.hasParameterAnnotation(CurrentUser.class) && parameter.getParameterType().equals(Usuario.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new UsuarioNaoEncontradoException("Nenhum usuário autenticado encontrado");
        }
        String email = auth.getName();
        return usuarioService.buscarPorEmail(email)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário autenticado não encontrado"));
    }
}
