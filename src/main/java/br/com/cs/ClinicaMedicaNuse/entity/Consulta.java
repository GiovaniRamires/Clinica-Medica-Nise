package br.com.cs.ClinicaMedicaNuse.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "consulta")
@Data
@NoArgsConstructor
public class Consulta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "paciente_id")
    private Paciente paciente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "medico_id")
    private Medico medico;

    @OneToOne(optional = false)
    @JoinColumn(name = "agendamento_id", unique = true)
    private Agendamento agendamento;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private LocalTime horario;

    @Column(nullable = false, length = 500)
    private String queixaPrincipal;

    @Column(length = 2000)
    private String observacoesMedicas;

    @Column(length = 2000)
    private String diagnostico;

    @Column(length = 2000)
    private String conduta;

    public Consulta(Agendamento agendamento, String queixaPrincipal, String observacoesMedicas,
                    String diagnostico, String conduta) {
        this.agendamento = agendamento;
        this.paciente = agendamento.getPaciente();
        this.medico = agendamento.getMedico();
        this.data = LocalDate.now();
        this.horario = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);
        this.queixaPrincipal = queixaPrincipal;
        this.observacoesMedicas = observacoesMedicas;
        this.diagnostico = diagnostico;
        this.conduta = conduta;
    }
}