package br.com.aurora.exception;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Converte exceções em respostas JSON com mensagens simples. Nunca devolve stack trace
 * nem dados do paciente; o log registra só o tipo do erro inesperado.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(NegocioException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public RespostaErro negocio(NegocioException e) {
        return new RespostaErro(e.getErros());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public RespostaErro validacao(MethodArgumentNotValidException e) {
        List<ErroCampo> erros = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new ErroCampo(f.getField(), f.getDefaultMessage()))
                .toList();
        return new RespostaErro(erros);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public RespostaErro corpoInvalido(HttpMessageNotReadableException e) {
        return RespostaErro.de(null,
                "Algum campo foi preenchido em formato inválido. Confira datas e números e tente de novo.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public RespostaErro parametroInvalido(MethodArgumentTypeMismatchException e) {
        return RespostaErro.de(e.getName(), "Valor inválido. Confira e tente de novo.");
    }

    @ExceptionHandler(NaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public RespostaErro naoEncontrado(NaoEncontradoException e) {
        return RespostaErro.de(null, e.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public RespostaErro recursoInexistente(NoResourceFoundException e) {
        return RespostaErro.de(null, "Página ou recurso não encontrado.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public RespostaErro acessoNegado(AccessDeniedException e) {
        return RespostaErro.de(null, "Seu perfil não tem permissão para esta ação.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public RespostaErro integridade(DataIntegrityViolationException e) {
        return RespostaErro.de(null,
                "Não foi possível salvar: já existe um registro com estes dados ou algum dado está fora do permitido.");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RespostaErro inesperado(Exception e) {
        log.error("Erro inesperado: {}", e.getClass().getName());
        return RespostaErro.de(null,
                "Ocorreu um erro inesperado. Tente de novo; se continuar, avise o administrador do sistema.");
    }
}
