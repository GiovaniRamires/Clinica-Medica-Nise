package br.com.cs.ClinicaMedicaNuse.service;

import br.com.cs.ClinicaMedicaNuse.dto.AtendimentoResponse;
import br.com.cs.ClinicaMedicaNuse.entity.*;
import br.com.cs.ClinicaMedicaNuse.repository.ConsultaRepository;
import br.com.cs.ClinicaMedicaNuse.repository.ExameRepository;
import br.com.cs.ClinicaMedicaNuse.repository.PrescricaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AtendimentoServiceTest {

    @Mock
    private ConsultaRepository consultaRepository;
    @Mock
    private PrescricaoRepository prescricaoRepository;
    @Mock
    private ExameRepository exameRepository;

    @InjectMocks
    private AtendimentoService atendimentoService;

    private Consulta consulta1;
    private Consulta consulta2;

    @BeforeEach
    void setUp() {
        var paciente = new Paciente("Maria", "12345678901", "Unimed", null, null);
        paciente.setId(1L);
        var medico = new Medico("Dr. House", "123456", "Cardiologia",
                new Usuario("Dr. House", "house@clinicanise.local", "hash", Usuario.Perfil.MEDICO));
        medico.setId(2L);

        var ag1 = new Agendamento(paciente, medico, LocalDate.now(), LocalTime.of(9, 0), null);
        var ag2 = new Agendamento(paciente, medico, LocalDate.now(), LocalTime.of(10, 0), null);
        consulta1 = new Consulta(ag1, "Dor de cabeça", "ANOTAÇÃO INTERNA", "Enxaqueca", "Repouso");
        consulta1.setId(100L);
        consulta2 = new Consulta(ag2, "Tosse", null, null, null);
        consulta2.setId(200L);
    }

    @Test
    public void agrupaPrescricoesEExamesPorConsulta() {
        var prescricao = new Prescricao(consulta1, "Dipirona", "500mg", "8/8h", "5 dias", null);
        var exame = new Exame(consulta2, "Raio-X de tórax");
        when(consultaRepository.findByPacienteIdOrderByDataDescHorarioDesc(1L))
                .thenReturn(List.of(consulta1, consulta2));
        when(prescricaoRepository.findByConsultaPacienteIdOrderByCriadoEmDesc(1L))
                .thenReturn(List.of(prescricao));
        when(exameRepository.findByPacienteIdOrderByDataSolicitacaoDesc(1L))
                .thenReturn(List.of(exame));

        List<AtendimentoResponse> resultado = atendimentoService.listarDoPaciente(1L);

        assertEquals(2, resultado.size());
        assertEquals("Enxaqueca", resultado.get(0).diagnostico());
        assertEquals(1, resultado.get(0).prescricoes().size());
        assertEquals("Dipirona", resultado.get(0).prescricoes().get(0).medicamento());
        assertTrue(resultado.get(0).exames().isEmpty());
        assertTrue(resultado.get(1).prescricoes().isEmpty());
        assertEquals("Raio-X de tórax", resultado.get(1).exames().get(0).nome());
        assertEquals("Dr. House", resultado.get(0).medico().nome());
    }

    @Test
    public void pacienteSemConsultasRecebeListaVazia() {
        when(consultaRepository.findByPacienteIdOrderByDataDescHorarioDesc(1L)).thenReturn(List.of());
        when(prescricaoRepository.findByConsultaPacienteIdOrderByCriadoEmDesc(1L)).thenReturn(List.of());
        when(exameRepository.findByPacienteIdOrderByDataSolicitacaoDesc(1L)).thenReturn(List.of());

        assertTrue(atendimentoService.listarDoPaciente(1L).isEmpty());
    }
}
