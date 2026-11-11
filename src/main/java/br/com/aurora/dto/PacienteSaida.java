package br.com.aurora.dto;

import java.time.LocalDate;

import br.com.aurora.domain.Paciente;
import br.com.aurora.domain.Sexo;

public record PacienteSaida(Long id, String cns, String nome, LocalDate dataNascimento, Sexo sexo,
        String racaCor, String etnia, String nacionalidade, String codigoMunicipio, String cep,
        String logradouro, String numero, String bairro, String telefone) {

    public static PacienteSaida de(Paciente p) {
        return new PacienteSaida(p.getId(), p.getCns(), p.getNome(), p.getDataNascimento(), p.getSexo(),
                p.getRacaCor(), p.getEtnia(), p.getNacionalidade(), p.getCodigoMunicipio(), p.getCep(),
                p.getLogradouro(), p.getNumero(), p.getBairro(), p.getTelefone());
    }
}
