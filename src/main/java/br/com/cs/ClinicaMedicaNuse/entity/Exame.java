package br.com.cs.ClinicaMedicaNuse.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "exame")
@Data
@NoArgsConstructor
public class Exame {

    public enum SituacaoExame {
        SOLICITADO,
        AGENDADO,
        REALIZADO,
        CANCELADO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "paciente_id")
    private Paciente paciente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "consulta_id")
    private Consulta consulta;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false)
    private LocalDate dataSolicitacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoExame situacao;

    private LocalDate dataRealizacao;

    @Column(length = 2000)
    private String resultado;

    public Exame(Consulta consulta, String nome) {
        this.consulta = consulta;
        this.paciente = consulta.getPaciente();
        this.nome = nome;
        this.dataSolicitacao = LocalDate.now();
        this.situacao = SituacaoExame.SOLICITADO;
    }
}
