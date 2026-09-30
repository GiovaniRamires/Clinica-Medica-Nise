    package br.com.cs.ClinicaMedicaNuse.entity;

    import jakarta.persistence.*;
    import lombok.Data;
    import lombok.NoArgsConstructor;

    import java.time.LocalDate;
    import java.time.LocalTime;

    @Entity
    @Table (name = "agendamento")
    @Data
    @NoArgsConstructor
    public class Agendamento {
        public enum SituacaoAgendamento {
            AGENDADO,
            CONFIRMADO,
            REALIZADO,
            AUSENTE
        }
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;


        @ManyToOne(optional = false)
        @JoinColumn(name = "paciente_id")
        private Paciente paciente;

        @ManyToOne(optional = false)
        @JoinColumn(name = "medico_id")
        private Medico medico;

        @ManyToOne(optional = false)
        private LocalDate data;

        @Column(nullable = false)
        private LocalTime horario;


        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private SituacaoAgendamento situacao;

        @Column(length = 500)
        private String observacoes;

        public Agendamento(Paciente paciente, Medico medico, LocalDate data, LocalTime horario,String observacoes){
            this.paciente = paciente;
            this.medico = medico;
            this.data = data;
            this.horario = horario;
            this.observacoes = observacoes;
            this.situacao = SituacaoAgendamento.AGENDADO;
        }

    }



