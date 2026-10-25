'use strict';

/* =====================================================================
 * Front-end da Clínica Médica Nise
 * ---------------------------------------------------------------------
 * Consome a API REST do back-end (Spring Boot) usando fetch.
 * A API só troca DTOs: enviamos PacienteRequest, AgendamentoRequest, etc.
 * e recebemos PacienteResponse, AgendamentoResponse, ...
 * Quando dá erro, o back-end devolve sempre o mesmo formato:
 *     { status, erro, mensagem, caminho, momento, campos: [{campo, mensagem}] }
 *
 * Se a página for aberta pelo Spring (http://localhost:8080/app/index.html),
 * as chamadas são relativas. Se for aberta direto do disco (file://),
 * apontamos para localhost:8080 e dependemos do CORS liberado no back-end.
 * ===================================================================== */

const API = location.protocol === 'file:' ? 'http://localhost:8080' : '';

// Situações em que o agendamento ainda pode ser atendido.
const SITUACOES_ABERTAS = ['AGENDADO', 'CONFIRMADO'];

// Listas carregadas da API, usadas para montar as tabelas e os selects.
let pacientes = [];
let medicos = [];
let agendamentos = [];
let consultas = [];


/* ============================= AJUDANTES ============================= */

/** Escapa o texto do usuário antes de jogar no HTML (evita quebrar a página). */
function esc(valor) {
    if (valor === null || valor === undefined || valor === '') return '—';
    return String(valor)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

/** 2026-10-25 -> 25/10/2026 */
function formatarData(iso) {
    if (!iso) return '—';
    const partes = iso.split('-');
    return `${partes[2]}/${partes[1]}/${partes[0]}`;
}

/** 11122233344 -> 111.222.333-44 */
function formatarCpf(cpf) {
    if (!cpf || cpf.length !== 11) return cpf || '—';
    return cpf.slice(0, 3) + '.' + cpf.slice(3, 6) + '.' + cpf.slice(6, 9) + '-' + cpf.slice(9);
}

/**
 * Faz a chamada HTTP e já trata o corpo de erro padrão da API.
 * Em caso de falha, lança um objeto { status, mensagem, campos }.
 */
async function requisicao(caminho, metodo = 'GET', corpo = null) {
    let resposta;
    try {
        resposta = await fetch(API + caminho, {
            method: metodo,
            headers: corpo ? {'Content-Type': 'application/json'} : {},
            body: corpo ? JSON.stringify(corpo) : null
        });
    } catch (falha) {
        throw {status: 0, mensagem: 'Não foi possível falar com a API. O back-end está rodando em localhost:8080?', campos: []};
    }

    if (resposta.status === 204) return null;          // DELETE não devolve corpo

    const texto = await resposta.text();
    const dados = texto ? JSON.parse(texto) : null;

    if (!resposta.ok) {
        throw {
            status: resposta.status,
            mensagem: (dados && dados.mensagem) || ('Erro ' + resposta.status),
            campos: (dados && dados.campos) || []
        };
    }
    return dados;
}

let timerAviso;

/** Mostra a mensagem flutuante no canto da tela. */
function mostrarAviso(texto, tipo, itens) {
    const aviso = document.getElementById('aviso');
    aviso.className = 'aviso aviso--' + tipo;
    let html = esc(texto);
    if (itens && itens.length) {
        html += '<ul>' + itens.map(item => '<li>' + esc(item) + '</li>').join('') + '</ul>';
    }
    aviso.innerHTML = html;
    clearTimeout(timerAviso);
    timerAviso = setTimeout(() => aviso.classList.add('oculto'), 5000);
}

/** Lê os campos de um formulário como objeto; campo vazio virá como null. */
function lerFormulario(form) {
    const dados = {};
    new FormData(form).forEach((valor, nome) => {
        const texto = String(valor).trim();
        dados[nome] = texto === '' ? null : texto;
    });
    return dados;
}

function limparErros(form) {
    form.querySelectorAll('.erro-campo').forEach(no => no.remove());
    form.querySelectorAll('[aria-invalid]').forEach(no => no.removeAttribute('aria-invalid'));
}

/** Destaca no formulário cada campo que o back-end recusou. */
function mostrarErros(form, erro) {
    if (erro.campos.length === 0) {
        mostrarAviso(erro.mensagem, 'erro');
        return;
    }
    erro.campos.forEach(item => {
        const campo = form.elements[item.campo];
        if (!campo) return;
        campo.setAttribute('aria-invalid', 'true');
        const rotulo = campo.closest('label') || campo.parentElement;
        const aviso = document.createElement('span');
        aviso.className = 'erro-campo';
        aviso.textContent = item.mensagem;
        rotulo.appendChild(aviso);
    });
    mostrarAviso('Corrija os campos destacados.', 'erro', erro.campos.map(c => c.mensagem));
}

/** Monta as opções de um <select>. */
function preencherSelect(select, itens, textoQuandoVazio) {
    const escolhidoAntes = select.value;
    if (itens.length === 0) {
        select.innerHTML = '<option value="">' + esc(textoQuandoVazio) + '</option>';
        select.disabled = true;
        return;
    }
    select.disabled = false;
    select.innerHTML = '<option value="">Selecione…</option>' +
        itens.map(item => '<option value="' + item.valor + '">' + esc(item.texto) + '</option>').join('');
    select.value = escolhidoAntes;
}

function trocarAba(nome) {
    document.querySelectorAll('.aba').forEach(aba => {
        aba.setAttribute('aria-selected', String(aba.dataset.painel === nome));
    });
    document.querySelectorAll('.painel').forEach(painel => {
        painel.classList.toggle('oculto', painel.id !== 'painel-' + nome);
    });
}


/* ============================= PACIENTES ============================= */

async function carregarPacientes() {
    pacientes = await requisicao('/pacientes');
    mostrarPacientes();
    atualizarSelectsDePaciente();
}

function mostrarPacientes() {
    document.getElementById('contador-pacientes').textContent = '(' + pacientes.length + ')';
    const tabela = document.getElementById('tabela-pacientes');

    if (pacientes.length === 0) {
        tabela.innerHTML = '<tr><td class="vazio" colspan="5">Nenhum paciente cadastrado ainda.</td></tr>';
        return;
    }

    tabela.innerHTML = pacientes.map(paciente => `
        <tr>
            <td>${esc(paciente.nome)}</td>
            <td>${esc(formatarCpf(paciente.cpf))}</td>
            <td>${esc(paciente.convenio)}</td>
            <td>${esc(paciente.alergias)}</td>
            <td>
                <div class="col-acoes-celula">
                    <button class="botao botao--neutro botao--pequeno"
                            onclick="editarPaciente(${paciente.id})">Editar</button>
                    <button class="botao botao--neutro botao--pequeno"
                            onclick="verProntuario(${paciente.id})">Prontuário</button>
                    <button class="botao botao--perigo botao--pequeno"
                            onclick="excluirPaciente(${paciente.id})">Excluir</button>
                </div>
            </td>
        </tr>
    `).join('');
}

async function salvarPaciente(evento) {
    evento.preventDefault();
    const form = evento.target;
    limparErros(form);
    if (!form.reportValidity()) return;

    const dados = lerFormulario(form);
    const id = dados.id;
    delete dados.id;

    try {
        if (id) {
            await requisicao('/pacientes/' + id, 'PUT', dados);
            mostrarAviso('Paciente atualizado.', 'ok');
        } else {
            await requisicao('/pacientes', 'POST', dados);
            mostrarAviso('Paciente cadastrado.', 'ok');
        }
        cancelarEdicaoPaciente();
        await carregarPacientes();
    } catch (erro) {
        mostrarErros(form, erro);
    }
}

function editarPaciente(id) {
    const paciente = pacientes.find(p => p.id === id);
    const form = document.getElementById('form-paciente');

    form.elements.id.value = paciente.id;
    form.elements.nome.value = paciente.nome || '';
    form.elements.cpf.value = paciente.cpf || '';
    form.elements.convenio.value = paciente.convenio || '';
    form.elements.alergias.value = paciente.alergias || '';
    form.elements.prontuario.value = paciente.prontuario || '';

    document.getElementById('titulo-form-paciente').textContent = 'Editando paciente';
    document.getElementById('cancelar-paciente').classList.remove('oculto');
    form.scrollIntoView({behavior: 'smooth', block: 'start'});
}

function cancelarEdicaoPaciente() {
    const form = document.getElementById('form-paciente');
    form.reset();
    limparErros(form);
    document.getElementById('titulo-form-paciente').textContent = 'Novo paciente';
    document.getElementById('cancelar-paciente').classList.add('oculto');
}

async function excluirPaciente(id) {
    const paciente = pacientes.find(p => p.id === id);
    if (!confirm('Excluir o paciente ' + paciente.nome + '? Essa ação não pode ser desfeita.')) return;

    try {
        await requisicao('/pacientes/' + id, 'DELETE');
        mostrarAviso('Paciente excluído.', 'ok');
        await carregarPacientes();
    } catch (erro) {
        mostrarAviso(erro.mensagem, 'erro');
    }
}


/* ============================== MÉDICOS ============================== */

async function carregarMedicos() {
    medicos = await requisicao('/medicos');
    mostrarMedicos();

    const itens = medicos.map(m => ({valor: m.id, texto: m.nome + ' — ' + m.especialidade}));
    preencherSelect(document.getElementById('form-agendamento').elements.medicoId,
        itens, 'Cadastre um médico primeiro');
}

function mostrarMedicos() {
    document.getElementById('contador-medicos').textContent = '(' + medicos.length + ')';
    const tabela = document.getElementById('tabela-medicos');

    if (medicos.length === 0) {
        tabela.innerHTML = '<tr><td class="vazio" colspan="4">Nenhum médico cadastrado ainda.</td></tr>';
        return;
    }

    tabela.innerHTML = medicos.map(medico => `
        <tr>
            <td>${esc(medico.nome)}</td>
            <td>${esc(medico.crm)}</td>
            <td>${esc(medico.especialidade)}</td>
            <td>
                <div class="col-acoes-celula">
                    <button class="botao botao--neutro botao--pequeno"
                            onclick="editarMedico(${medico.id})">Editar</button>
                    <button class="botao botao--perigo botao--pequeno"
                            onclick="excluirMedico(${medico.id})">Excluir</button>
                </div>
            </td>
        </tr>
    `).join('');
}

async function salvarMedico(evento) {
    evento.preventDefault();
    const form = evento.target;
    limparErros(form);
    if (!form.reportValidity()) return;

    const dados = lerFormulario(form);
    const id = dados.id;
    delete dados.id;

    try {
        if (id) {
            await requisicao('/medicos/' + id, 'PUT', dados);
            mostrarAviso('Médico atualizado.', 'ok');
        } else {
            await requisicao('/medicos', 'POST', dados);
            mostrarAviso('Médico cadastrado.', 'ok');
        }
        cancelarEdicaoMedico();
        await carregarMedicos();
    } catch (erro) {
        mostrarErros(form, erro);
    }
}

function editarMedico(id) {
    const medico = medicos.find(m => m.id === id);
    const form = document.getElementById('form-medico');

    form.elements.id.value = medico.id;
    form.elements.nome.value = medico.nome || '';
    form.elements.crm.value = medico.crm || '';
    form.elements.especialidade.value = medico.especialidade || '';

    document.getElementById('titulo-form-medico').textContent = 'Editando médico';
    document.getElementById('cancelar-medico').classList.remove('oculto');
    form.scrollIntoView({behavior: 'smooth', block: 'start'});
}

function cancelarEdicaoMedico() {
    const form = document.getElementById('form-medico');
    form.reset();
    limparErros(form);
    document.getElementById('titulo-form-medico').textContent = 'Novo médico';
    document.getElementById('cancelar-medico').classList.add('oculto');
}

async function excluirMedico(id) {
    const medico = medicos.find(m => m.id === id);
    if (!confirm('Excluir o médico ' + medico.nome + '?')) return;

    try {
        await requisicao('/medicos/' + id, 'DELETE');
        mostrarAviso('Médico excluído.', 'ok');
        await carregarMedicos();
    } catch (erro) {
        mostrarAviso(erro.mensagem, 'erro');
    }
}


/* ============================== AGENDA =============================== */

async function carregarAgendamentos() {
    agendamentos = await requisicao('/agendamentos');
    mostrarAgendamentos();

    // No formulário de consulta só podem aparecer agendamentos ainda abertos.
    const abertos = agendamentos.filter(a => SITUACOES_ABERTAS.includes(a.situacao));
    const itens = abertos.map(a => ({
        valor: a.id,
        texto: formatarData(a.data) + ' ' + a.horario + ' — ' + a.paciente.nome + ' / ' + a.medico.nome
    }));
    preencherSelect(document.getElementById('form-consulta').elements.agendamentoId,
        itens, 'Nenhum agendamento aberto');
}

function mostrarAgendamentos() {
    document.getElementById('contador-agendamentos').textContent = '(' + agendamentos.length + ')';
    const tabela = document.getElementById('tabela-agendamentos');

    if (agendamentos.length === 0) {
        tabela.innerHTML = '<tr><td class="vazio" colspan="6">Nenhum agendamento na agenda.</td></tr>';
        return;
    }

    tabela.innerHTML = agendamentos.map(agendamento => {
        const aberto = SITUACOES_ABERTAS.includes(agendamento.situacao);
        let acoes = '';

        if (agendamento.situacao === 'AGENDADO') {
            acoes += `<button class="botao botao--neutro botao--pequeno"
                              onclick="alterarSituacao(${agendamento.id}, 'CONFIRMADO')">Confirmar</button>`;
        }
        if (aberto) {
            acoes += `<button class="botao botao--primario botao--pequeno"
                              onclick="atenderAgendamento(${agendamento.id})">Atender</button>
                      <button class="botao botao--neutro botao--pequeno"
                              onclick="alterarSituacao(${agendamento.id}, 'AUSENTE')">Faltou</button>
                      <button class="botao botao--perigo botao--pequeno"
                              onclick="alterarSituacao(${agendamento.id}, 'CANCELADO')">Cancelar</button>`;
        }

        return `
            <tr>
                <td>${esc(formatarData(agendamento.data))}</td>
                <td>${esc(agendamento.horario)}</td>
                <td>${esc(agendamento.paciente.nome)}</td>
                <td>${esc(agendamento.medico.nome)} (${esc(agendamento.medico.especialidade)})</td>
                <td><span class="selo selo--${esc(agendamento.situacao)}">${esc(agendamento.situacao)}</span></td>
                <td><div class="col-acoes-celula">${acoes || '—'}</div></td>
            </tr>
        `;
    }).join('');
}

async function salvarAgendamento(evento) {
    evento.preventDefault();
    const form = evento.target;
    limparErros(form);
    if (!form.reportValidity()) return;

    const dados = lerFormulario(form);

    try {
        await requisicao('/agendamentos', 'POST', {
            pacienteId: Number(dados.pacienteId),
            medicoId: Number(dados.medicoId),
            data: dados.data,
            horario: dados.horario,
            observacoes: dados.observacoes
        });
        mostrarAviso('Agendamento criado.', 'ok');
        form.reset();
        await carregarAgendamentos();
    } catch (erro) {
        mostrarErros(form, erro);
    }
}

async function alterarSituacao(id, situacao) {
    try {
        await requisicao('/agendamentos/' + id + '/situacao', 'PUT', {situacao: situacao});
        mostrarAviso('Agendamento marcado como ' + situacao + '.', 'ok');
        await carregarAgendamentos();
    } catch (erro) {
        mostrarAviso(erro.mensagem, 'erro');
    }
}

/** Leva o atendente para a aba de consultas já com o agendamento escolhido. */
function atenderAgendamento(id) {
    cancelarEdicaoConsulta();
    trocarAba('consultas');
    const form = document.getElementById('form-consulta');
    form.elements.agendamentoId.value = String(id);
    form.elements.queixaPrincipal.focus();
}


/* ============================= CONSULTAS ============================= */

async function carregarConsultas() {
    consultas = await requisicao('/consultas');
    mostrarConsultas();
}

function mostrarConsultas() {
    document.getElementById('contador-consultas').textContent = '(' + consultas.length + ')';
    const tabela = document.getElementById('tabela-consultas');

    if (consultas.length === 0) {
        tabela.innerHTML = '<tr><td class="vazio" colspan="6">Nenhuma consulta registrada.</td></tr>';
        return;
    }

    tabela.innerHTML = consultas.map(consulta => `
        <tr>
            <td>${esc(formatarData(consulta.data))}</td>
            <td>${esc(consulta.horario)}</td>
            <td>${esc(consulta.paciente.nome)}</td>
            <td>${esc(consulta.medico.nome)}</td>
            <td>${esc(consulta.queixaPrincipal)}</td>
            <td>
                <div class="col-acoes-celula">
                    <button class="botao botao--neutro botao--pequeno"
                            onclick="editarConsulta(${consulta.id})">Editar</button>
                </div>
            </td>
        </tr>
    `).join('');
}

async function salvarConsulta(evento) {
    evento.preventDefault();
    const form = evento.target;
    limparErros(form);
    if (!form.reportValidity()) return;

    const dados = lerFormulario(form);
    const id = dados.id;

    try {
        if (id) {
            // Na edição o agendamento de origem não muda (ConsultaAtualizacaoRequest).
            await requisicao('/consultas/' + id, 'PUT', {
                queixaPrincipal: dados.queixaPrincipal,
                observacoesMedicas: dados.observacoesMedicas,
                diagnostico: dados.diagnostico,
                conduta: dados.conduta
            });
            mostrarAviso('Consulta atualizada.', 'ok');
        } else {
            await requisicao('/consultas', 'POST', {
                agendamentoId: Number(dados.agendamentoId),
                queixaPrincipal: dados.queixaPrincipal,
                observacoesMedicas: dados.observacoesMedicas,
                diagnostico: dados.diagnostico,
                conduta: dados.conduta
            });
            mostrarAviso('Atendimento registrado.', 'ok');
        }
        cancelarEdicaoConsulta();
        await carregarConsultas();
        await carregarAgendamentos();   // o agendamento virou REALIZADO
    } catch (erro) {
        mostrarErros(form, erro);
    }
}

function editarConsulta(id) {
    const consulta = consultas.find(c => c.id === id);
    const form = document.getElementById('form-consulta');

    form.elements.id.value = consulta.id;
    form.elements.queixaPrincipal.value = consulta.queixaPrincipal || '';
    form.elements.observacoesMedicas.value = consulta.observacoesMedicas || '';
    form.elements.diagnostico.value = consulta.diagnostico || '';
    form.elements.conduta.value = consulta.conduta || '';

    // O agendamento não pode ser trocado depois do registro: esconde o campo.
    document.getElementById('campo-agendamento').classList.add('oculto');
    form.elements.agendamentoId.removeAttribute('required');

    document.getElementById('titulo-form-consulta').textContent = 'Editando consulta';
    document.getElementById('cancelar-consulta').classList.remove('oculto');
    form.scrollIntoView({behavior: 'smooth', block: 'start'});
}

function cancelarEdicaoConsulta() {
    const form = document.getElementById('form-consulta');
    form.reset();
    limparErros(form);

    document.getElementById('campo-agendamento').classList.remove('oculto');
    form.elements.agendamentoId.setAttribute('required', '');

    document.getElementById('titulo-form-consulta').textContent = 'Registrar atendimento';
    document.getElementById('cancelar-consulta').classList.add('oculto');
}


/* ============================ PRONTUÁRIO ============================= */

function atualizarSelectsDePaciente() {
    const itens = pacientes.map(p => ({valor: p.id, texto: p.nome + ' — ' + formatarCpf(p.cpf)}));
    preencherSelect(document.getElementById('form-agendamento').elements.pacienteId,
        itens, 'Cadastre um paciente primeiro');
    preencherSelect(document.getElementById('select-prontuario'),
        itens, 'Cadastre um paciente primeiro');
}

/** Abre a aba de prontuário já no paciente escolhido. */
function verProntuario(id) {
    trocarAba('prontuario');
    document.getElementById('select-prontuario').value = String(id);
    mostrarProntuario(id);
}

async function mostrarProntuario(pacienteId) {
    const destino = document.getElementById('conteudo-prontuario');

    let prontuario;
    try {
        prontuario = await requisicao('/pacientes/' + pacienteId + '/prontuario');
    } catch (erro) {
        destino.innerHTML = '';
        mostrarAviso(erro.mensagem, 'erro');
        return;
    }

    const paciente = prontuario.paciente;

    const historico = prontuario.consultas.length === 0
        ? '<p class="vazio">Este paciente ainda não possui consultas.</p>'
        : prontuario.consultas.map(consulta => `
            <article class="consulta-historico">
                <h3>${esc(formatarData(consulta.data))} às ${esc(consulta.horario)} —
                    ${esc(consulta.medico.nome)} (${esc(consulta.medico.especialidade)})</h3>
                <p><strong>Queixa:</strong> ${esc(consulta.queixaPrincipal)}</p>
                <p><strong>Diagnóstico:</strong> ${esc(consulta.diagnostico)}</p>
                <p><strong>Conduta:</strong> ${esc(consulta.conduta)}</p>
                <p><strong>Observações:</strong> ${esc(consulta.observacoesMedicas)}</p>
            </article>
        `).join('');

    destino.innerHTML = `
        <section class="cartao">
            <h2>Prontuário de ${esc(paciente.nome)}</h2>
            <dl class="ficha">
                <div><dt>Nome</dt><dd>${esc(paciente.nome)}</dd></div>
                <div><dt>CPF</dt><dd>${esc(formatarCpf(paciente.cpf))}</dd></div>
                <div><dt>Convênio</dt><dd>${esc(paciente.convenio)}</dd></div>
                <div><dt>Alergias</dt><dd>${esc(paciente.alergias)}</dd></div>
            </dl>
            <p class="rotulo">Anotações gerais</p>
            <p>${esc(paciente.prontuario)}</p>
        </section>
        <section class="cartao">
            <h2>Histórico de consultas (${prontuario.totalConsultas})</h2>
            ${historico}
        </section>
    `;
}


/* ============================== INÍCIO =============================== */

async function iniciar() {
    // Abas
    document.querySelectorAll('.aba').forEach(aba => {
        aba.addEventListener('click', () => trocarAba(aba.dataset.painel));
    });
    trocarAba('pacientes');

    // Formulários
    document.getElementById('form-paciente').addEventListener('submit', salvarPaciente);
    document.getElementById('form-medico').addEventListener('submit', salvarMedico);
    document.getElementById('form-agendamento').addEventListener('submit', salvarAgendamento);
    document.getElementById('form-consulta').addEventListener('submit', salvarConsulta);
    document.getElementById('select-prontuario').addEventListener('change', evento => {
        if (evento.target.value) mostrarProntuario(Number(evento.target.value));
        else document.getElementById('conteudo-prontuario').innerHTML = '';
    });

    // Carga inicial
    const status = document.getElementById('status-api');
    try {
        await carregarPacientes();
        await carregarMedicos();
        await carregarAgendamentos();
        await carregarConsultas();
        status.textContent = 'API conectada';
        status.className = 'status status--ok';
    } catch (erro) {
        status.textContent = 'API offline';
        status.className = 'status status--erro';
        mostrarAviso(erro.mensagem, 'erro');
    }
}

iniciar();
