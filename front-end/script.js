'use strict';

/* =====================================================================
 * Front-end da Clínica Médica Nise
 * ---------------------------------------------------------------------
 * Consome a API REST do back-end (Spring Boot) usando fetch.
 *
 * Autenticação: POST /auth/login devolve um token JWT. Guardamos o token no
 * sessionStorage e mandamos em todo request no header
 *     Authorization: Bearer <token>
 * Usamos sessionStorage (e não localStorage) para o token morrer quando a aba
 * fecha. Qualquer 401 derruba a sessão e volta para a tela de login.
 *
 * A API só troca DTOs: enviamos PacienteRequest, AgendamentoRequest, etc. e
 * recebemos PacienteResponse, AgendamentoResponse, ... Quando dá erro, o
 * back-end devolve sempre o mesmo formato:
 *     { status, erro, mensagem, caminho, momento, campos: [{campo, mensagem}] }
 *
 * Se a página for aberta pelo Spring (http://localhost:8080/app/index.html),
 * as chamadas são relativas. Se for aberta direto do disco (file://),
 * apontamos para localhost:8080 e dependemos do CORS liberado no back-end.
 * ===================================================================== */

const API = location.protocol === 'file:' ? 'http://localhost:8080' : '';

// Situações em que o agendamento ainda pode ser atendido.
const SITUACOES_ABERTAS = ['AGENDADO', 'CONFIRMADO'];

const CHAVE_TOKEN = 'clinica.token';

// Sessão do usuário logado: token JWT + dados vindos do login.
let sessao = {token: null, usuario: null};

// Listas carregadas da API, usadas para montar as tabelas e os selects.
let pacientes = [];
let medicos = [];
let agendamentos = [];
let consultas = [];
let usuarios = [];


/* ============================== ÍCONES ================================ */
/* Pequeno subconjunto de ícones no estilo Lucide (traço), inline como SVG. */

const ICONES = {
    users: '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/>',
    userPlus: '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M19 8v6M22 11h-6"/>',
    user: '<path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>',
    calendar: '<rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/>',
    clipboard: '<rect x="8" y="2" width="8" height="4" rx="1"/><path d="M8 4H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2h-2"/>',
    book: '<path d="M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H20v20H6.5a2.5 2.5 0 0 1 0-5H20"/>',
    shield: '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>',
    logout: '<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9"/>',
    lock: '<rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>'
};

function icone(nome) {
    return `<svg viewBox="0 0 24 24" aria-hidden="true">${ICONES[nome] || ''}</svg>`;
}

/** Substitui os placeholders data-icon="nome" pelo SVG correspondente. */
function montarIcones(raiz) {
    (raiz || document).querySelectorAll('[data-icon]').forEach(el => {
        const nome = el.dataset.icon;
        if (nome && ICONES[nome]) el.innerHTML = icone(nome);
    });
}


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

/** Duas iniciais a partir do nome, para o avatar — "Maria Silva" -> "MS". */
function iniciais(nome) {
    if (!nome) return '?';
    const partes = nome.trim().split(/\s+/);
    const primeira = partes[0]?.[0] || '';
    const ultima = partes.length > 1 ? partes[partes.length - 1][0] : '';
    return (primeira + ultima).toUpperCase();
}

const PERFIS = {
    ADMINISTRADOR: 'Administrador',
    MEDICO: 'Médico',
    RECEPCIONISTA: 'Recepcionista'
};

const SELOS_SITUACAO = {
    AGENDADO: 'badge-info',
    CONFIRMADO: 'badge-info',
    REALIZADO: 'badge-success',
    AUSENTE: 'badge-warning',
    CANCELADO: 'badge-danger'
};

function perfilAtual() {
    return sessao.usuario ? sessao.usuario.perfil : null;
}

function ehAdministrador() {
    return perfilAtual() === 'ADMINISTRADOR';
}

/** Quem pode ver consulta e prontuário: médico e administrador. */
function podeVerDadoClinico() {
    return perfilAtual() === 'MEDICO' || ehAdministrador();
}

/**
 * Faz a chamada HTTP com o token da sessão e já trata o corpo de erro padrão.
 * Em caso de falha, lança um objeto { status, mensagem, campos }.
 */
async function requisicao(caminho, metodo = 'GET', corpo = null) {
    const headers = {};
    if (corpo) headers['Content-Type'] = 'application/json';
    if (sessao.token) headers['Authorization'] = 'Bearer ' + sessao.token;

    let resposta;
    try {
        resposta = await fetch(API + caminho, {
            method: metodo,
            headers: headers,
            body: corpo ? JSON.stringify(corpo) : null
        });
    } catch (falha) {
        throw {status: 0, mensagem: 'Não foi possível falar com a API. O back-end está rodando em localhost:8080?', campos: []};
    }

    if (resposta.status === 204) return null;          // DELETE e troca de senha não devolvem corpo

    const texto = await resposta.text();
    const dados = texto ? JSON.parse(texto) : null;

    if (!resposta.ok) {
        // Token expirado ou inválido: não há o que fazer na tela, volta pro login.
        if (resposta.status === 401 && sessao.token) {
            encerrarSessao('Sua sessão expirou. Entre novamente.');
        }
        throw {
            status: resposta.status,
            mensagem: (dados && dados.mensagem) || ('Erro ' + resposta.status),
            campos: (dados && dados.campos) || []
        };
    }
    return dados;
}

/** Mostra um toast no canto da tela, no estilo do SisLab. */
function mostrarToast(texto, tipo = 'info') {
    const container = document.getElementById('toasts');
    const toast = document.createElement('div');
    toast.className = 'toast ' + (tipo === 'erro' ? 'error' : tipo === 'ok' ? 'success' : '');
    toast.innerHTML = `<span class="toast-text">${esc(texto)}</span>`;
    container.appendChild(toast);
    setTimeout(() => toast.remove(), 5000);
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
    form.querySelectorAll('.field.has-error').forEach(campo => {
        campo.classList.remove('has-error');
        const erro = campo.querySelector('.field-error');
        if (erro) erro.textContent = '';
    });
}

/** Destaca no formulário cada campo que o back-end recusou. */
function mostrarErros(form, erro) {
    if (erro.campos.length === 0) {
        mostrarToast(erro.mensagem, 'erro');
        return;
    }
    erro.campos.forEach(item => {
        const entrada = form.elements[item.campo];
        if (!entrada) return;
        const campo = entrada.closest('.field');
        if (!campo) return;
        campo.classList.add('has-error');
        const mensagem = campo.querySelector('.field-error');
        if (mensagem) mensagem.textContent = item.mensagem;
    });
    mostrarToast('Corrija os campos destacados.', 'erro');
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
    document.querySelectorAll('.nav-item[data-painel]').forEach(botao => {
        botao.classList.toggle('active', botao.dataset.painel === nome);
    });
    document.querySelectorAll('.view').forEach(painel => {
        painel.classList.toggle('oculto', painel.id !== 'painel-' + nome);
    });
    const titulos = {
        pacientes: 'Pacientes', medicos: 'Médicos', agenda: 'Agenda',
        consultas: 'Consultas', prontuario: 'Prontuário', usuarios: 'Usuários', conta: 'Minha conta'
    };
    document.getElementById('titulo-painel').textContent = titulos[nome] || '';
}


/* ======================= LOGIN E SESSÃO ============================== */

async function entrar(evento) {
    evento.preventDefault();
    const form = evento.target;
    const erro = document.getElementById('erro-login');
    erro.classList.add('oculto');
    limparErros(form);

    if (!form.reportValidity()) return;

    const dados = lerFormulario(form);

    try {
        const resposta = await requisicao('/auth/login', 'POST', {
            email: dados.email,
            senha: dados.senha
        });
        sessao = {token: resposta.token, usuario: resposta.usuario};
        sessionStorage.setItem(CHAVE_TOKEN, resposta.token);
        form.reset();
        await abrirAplicacao();
    } catch (falha) {
        erro.textContent = falha.mensagem;
        erro.classList.remove('oculto');
    }
}

async function sair() {
    try {
        await requisicao('/auth/logout', 'POST');
    } catch (falha) {
        // O logout do JWT é só no cliente: se a chamada falhar, seguimos saindo.
    }
    encerrarSessao();
}

/** Limpa a sessão e volta para a tela de login. */
function encerrarSessao(mensagem) {
    sessao = {token: null, usuario: null};
    sessionStorage.removeItem(CHAVE_TOKEN);
    pacientes = [];
    medicos = [];
    agendamentos = [];
    consultas = [];
    usuarios = [];

    document.getElementById('aplicacao').classList.add('oculto');
    document.getElementById('tela-login').classList.remove('oculto');
    if (mensagem) mostrarToast(mensagem, 'erro');
}

/**
 * Esconde da tela tudo que o perfil do usuário não pode usar.
 * Isso é conveniência de interface: quem garante a regra é o back-end, que
 * devolve 403 mesmo se alguém chamar o endpoint na mão.
 */
function aplicarPermissoes() {
    const perfil = perfilAtual();
    document.querySelectorAll('[data-perfil]').forEach(elemento => {
        const permitidos = elemento.dataset.perfil.split(',');
        elemento.classList.toggle('oculto', !permitidos.includes(perfil));
    });
}

/** Entra na aplicação: mostra os dados do usuário, aplica permissões e carrega tudo. */
async function abrirAplicacao() {
    document.getElementById('tela-login').classList.add('oculto');
    document.getElementById('aplicacao').classList.remove('oculto');

    const usuario = sessao.usuario;
    document.getElementById('usuario-nome').textContent = usuario.nome;
    document.getElementById('usuario-perfil').textContent = PERFIS[usuario.perfil] || usuario.perfil;
    document.getElementById('usuario-avatar').textContent = iniciais(usuario.nome);

    document.getElementById('ficha-conta').innerHTML = `
        <div><p class="kv-label">Nome</p><p class="kv-value">${esc(usuario.nome)}</p></div>
        <div><p class="kv-label">E-mail</p><p class="kv-value">${esc(usuario.email)}</p></div>
        <div><p class="kv-label">Perfil</p><p class="kv-value">${esc(PERFIS[usuario.perfil] || usuario.perfil)}</p></div>
    `;

    aplicarPermissoes();
    trocarAba('pacientes');
    await carregarTudo();
}

/** Carrega só o que o perfil tem permissão de ler. */
async function carregarTudo() {
    try {
        await carregarPacientes();
        await carregarMedicos();
        await carregarAgendamentos();
        if (podeVerDadoClinico()) await carregarConsultas();
        if (ehAdministrador()) await carregarUsuarios();
    } catch (erro) {
        mostrarToast(erro.mensagem, 'erro');
    }
}


/* ============================= PACIENTES ============================= */

async function carregarPacientes() {
    pacientes = await requisicao('/pacientes');
    mostrarPacientes();
    atualizarSelectsDePaciente();
}

function mostrarPacientes() {
    document.getElementById('contador-pacientes').textContent = pacientes.length;
    const tabela = document.getElementById('tabela-pacientes');
    const clinico = podeVerDadoClinico();

    if (pacientes.length === 0) {
        tabela.innerHTML = `<tr><td class="vazio" colspan="${clinico ? 5 : 4}">Nenhum paciente cadastrado ainda.</td></tr>`;
        return;
    }

    tabela.innerHTML = pacientes.map(paciente => {
        let acoes = `<button class="btn btn-secondary btn-sm" onclick="editarPaciente(${paciente.id})">Editar</button>`;
        if (clinico) {
            acoes += `<button class="btn btn-secondary btn-sm" onclick="verProntuario(${paciente.id})">Prontuário</button>`;
        }
        if (ehAdministrador()) {
            acoes += `<button class="btn btn-danger-soft btn-sm" onclick="excluirPaciente(${paciente.id})">Excluir</button>`;
        }
        // Alergias é dado clínico: a coluna só existe para quem pode ver.
        const alergias = clinico ? `<td>${esc(paciente.alergias)}</td>` : '';
        return `
            <tr>
                <td>${esc(paciente.nome)}</td>
                <td>${esc(formatarCpf(paciente.cpf))}</td>
                <td>${esc(paciente.convenio)}</td>
                ${alergias}
                <td><div class="actions">${acoes}</div></td>
            </tr>
        `;
    }).join('');
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
            mostrarToast('Paciente atualizado.', 'ok');
        } else {
            await requisicao('/pacientes', 'POST', dados);
            mostrarToast('Paciente cadastrado.', 'ok');
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
    if (form.elements.alergias) form.elements.alergias.value = paciente.alergias || '';
    if (form.elements.prontuario) form.elements.prontuario.value = paciente.prontuario || '';

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
        mostrarToast('Paciente excluído.', 'ok');
        await carregarPacientes();
    } catch (erro) {
        mostrarToast(erro.mensagem, 'erro');
    }
}


/* ============================== MÉDICOS ============================== */

async function carregarMedicos() {
    medicos = await requisicao('/medicos');
    mostrarMedicos();

    const itens = medicos.map(m => ({valor: m.id, texto: m.nome + ' — ' + m.especialidade}));
    preencherSelect(document.getElementById('form-agendamento').elements.medicoId,
        itens, 'Nenhum médico cadastrado');
}

function mostrarMedicos() {
    document.getElementById('contador-medicos').textContent = medicos.length;
    const tabela = document.getElementById('tabela-medicos');

    if (medicos.length === 0) {
        tabela.innerHTML = '<tr><td class="vazio" colspan="4">Nenhum médico cadastrado ainda.</td></tr>';
        return;
    }

    tabela.innerHTML = medicos.map(medico => {
        // Manutenção do corpo clínico é só do administrador.
        const acoes = ehAdministrador()
            ? `<button class="btn btn-secondary btn-sm" onclick="editarMedico(${medico.id})">Editar</button>
               <button class="btn btn-danger-soft btn-sm" onclick="excluirMedico(${medico.id})">Excluir</button>`
            : '—';
        return `
            <tr>
                <td>${esc(medico.nome)}</td>
                <td>${esc(medico.crm)}</td>
                <td>${esc(medico.especialidade)}</td>
                <td><div class="actions">${acoes}</div></td>
            </tr>
        `;
    }).join('');
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
            mostrarToast('Médico atualizado.', 'ok');
        } else {
            await requisicao('/medicos', 'POST', dados);
            mostrarToast('Médico cadastrado.', 'ok');
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
        mostrarToast('Médico excluído.', 'ok');
        await carregarMedicos();
    } catch (erro) {
        mostrarToast(erro.mensagem, 'erro');
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
    document.getElementById('contador-agendamentos').textContent = agendamentos.length;
    const tabela = document.getElementById('tabela-agendamentos');

    if (agendamentos.length === 0) {
        tabela.innerHTML = '<tr><td class="vazio" colspan="6">Nenhum agendamento na agenda.</td></tr>';
        return;
    }

    tabela.innerHTML = agendamentos.map(agendamento => {
        const aberto = SITUACOES_ABERTAS.includes(agendamento.situacao);
        let acoes = '';

        if (agendamento.situacao === 'AGENDADO') {
            acoes += `<button class="btn btn-secondary btn-sm" onclick="alterarSituacao(${agendamento.id}, 'CONFIRMADO')">Confirmar</button>`;
        }
        if (aberto && podeVerDadoClinico()) {
            acoes += `<button class="btn btn-primary btn-sm" onclick="atenderAgendamento(${agendamento.id})">Atender</button>`;
        }
        if (aberto) {
            acoes += `<button class="btn btn-secondary btn-sm" onclick="alterarSituacao(${agendamento.id}, 'AUSENTE')">Faltou</button>
                      <button class="btn btn-danger-soft btn-sm" onclick="alterarSituacao(${agendamento.id}, 'CANCELADO')">Cancelar</button>`;
        }

        const selo = SELOS_SITUACAO[agendamento.situacao] || 'badge-neutral';

        return `
            <tr>
                <td>${esc(formatarData(agendamento.data))}</td>
                <td>${esc(agendamento.horario)}</td>
                <td>${esc(agendamento.paciente.nome)}</td>
                <td>${esc(agendamento.medico.nome)} (${esc(agendamento.medico.especialidade)})</td>
                <td><span class="badge ${selo}">${esc(agendamento.situacao)}</span></td>
                <td><div class="actions">${acoes || '—'}</div></td>
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
        mostrarToast('Agendamento criado.', 'ok');
        form.reset();
        await carregarAgendamentos();
    } catch (erro) {
        mostrarErros(form, erro);
    }
}

async function alterarSituacao(id, situacao) {
    try {
        await requisicao('/agendamentos/' + id + '/situacao', 'PUT', {situacao: situacao});
        mostrarToast('Agendamento marcado como ' + situacao + '.', 'ok');
        await carregarAgendamentos();
    } catch (erro) {
        mostrarToast(erro.mensagem, 'erro');
    }
}

/** Leva para a aba de consultas já com o agendamento escolhido. */
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
    document.getElementById('contador-consultas').textContent = consultas.length;
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
            <td><div class="actions"><button class="btn btn-secondary btn-sm" onclick="editarConsulta(${consulta.id})">Editar</button></div></td>
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
            mostrarToast('Consulta atualizada.', 'ok');
        } else {
            await requisicao('/consultas', 'POST', {
                agendamentoId: Number(dados.agendamentoId),
                queixaPrincipal: dados.queixaPrincipal,
                observacoesMedicas: dados.observacoesMedicas,
                diagnostico: dados.diagnostico,
                conduta: dados.conduta
            });
            mostrarToast('Atendimento registrado.', 'ok');
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
        mostrarToast(erro.mensagem, 'erro');
        return;
    }

    const paciente = prontuario.paciente;

    const historico = prontuario.consultas.length === 0
        ? '<p class="vazio">Este paciente ainda não possui consultas.</p>'
        : `<div class="form">${prontuario.consultas.map(consulta => `
            <article class="prontuario-item">
                <h3>${esc(formatarData(consulta.data))} às ${esc(consulta.horario)} —
                    ${esc(consulta.medico.nome)} (${esc(consulta.medico.especialidade)})</h3>
                <p><strong>Queixa:</strong> ${esc(consulta.queixaPrincipal)}</p>
                <p><strong>Diagnóstico:</strong> ${esc(consulta.diagnostico)}</p>
                <p><strong>Conduta:</strong> ${esc(consulta.conduta)}</p>
                <p><strong>Observações:</strong> ${esc(consulta.observacoesMedicas)}</p>
            </article>
        `).join('')}</div>`;

    destino.innerHTML = `
        <div class="card">
            <div class="card-header"><span class="card-title">Prontuário de ${esc(paciente.nome)}</span></div>
            <div class="card-body">
                <div class="kv-grid">
                    <div><p class="kv-label">Nome</p><p class="kv-value">${esc(paciente.nome)}</p></div>
                    <div><p class="kv-label">CPF</p><p class="kv-value">${esc(formatarCpf(paciente.cpf))}</p></div>
                    <div><p class="kv-label">Convênio</p><p class="kv-value">${esc(paciente.convenio)}</p></div>
                    <div><p class="kv-label">Alergias</p><p class="kv-value">${esc(paciente.alergias)}</p></div>
                </div>
                <p class="kv-label" style="margin-top:18px">Anotações gerais</p>
                <p class="kv-value">${esc(paciente.prontuario)}</p>
            </div>
        </div>
        <div class="card">
            <div class="card-header">
                <span class="card-title">Histórico de consultas</span>
                <span class="badge badge-neutral">${prontuario.totalConsultas}</span>
            </div>
            <div class="card-body">${historico}</div>
        </div>
    `;
}


/* ============================= USUÁRIOS ============================== */

async function carregarUsuarios() {
    usuarios = await requisicao('/usuarios');
    mostrarUsuarios();
}

function mostrarUsuarios() {
    document.getElementById('contador-usuarios').textContent = usuarios.length;
    const tabela = document.getElementById('tabela-usuarios');

    if (usuarios.length === 0) {
        tabela.innerHTML = '<tr><td class="vazio" colspan="4">Nenhum usuário cadastrado.</td></tr>';
        return;
    }

    tabela.innerHTML = usuarios.map(usuario => {
        const ehVoce = sessao.usuario && usuario.id === sessao.usuario.id;
        const excluir = ehVoce
            ? ''
            : `<button class="btn btn-danger-soft btn-sm" onclick="excluirUsuario(${usuario.id})">Excluir</button>`;
        return `
            <tr>
                <td>${esc(usuario.nome)}${ehVoce ? ' <span class="badge badge-neutral">você</span>' : ''}</td>
                <td>${esc(usuario.email)}</td>
                <td>${esc(PERFIS[usuario.perfil] || usuario.perfil)}</td>
                <td><div class="actions"><button class="btn btn-secondary btn-sm" onclick="editarUsuario(${usuario.id})">Editar</button>${excluir}</div></td>
            </tr>
        `;
    }).join('');
}

async function salvarUsuario(evento) {
    evento.preventDefault();
    const form = evento.target;
    limparErros(form);
    if (!form.reportValidity()) return;

    const dados = lerFormulario(form);
    const id = dados.id;

    try {
        if (id) {
            // UsuarioAtualizacaoRequest não tem senha: trocar senha é outra operação.
            await requisicao('/usuarios/' + id, 'PUT', {
                nome: dados.nome,
                email: dados.email,
                perfil: dados.perfil
            });
            mostrarToast('Usuário atualizado.', 'ok');
        } else {
            await requisicao('/usuarios', 'POST', {
                nome: dados.nome,
                email: dados.email,
                senha: dados.senha,
                perfil: dados.perfil
            });
            mostrarToast('Usuário cadastrado.', 'ok');
        }
        cancelarEdicaoUsuario();
        await carregarUsuarios();
    } catch (erro) {
        mostrarErros(form, erro);
    }
}

function editarUsuario(id) {
    const usuario = usuarios.find(u => u.id === id);
    const form = document.getElementById('form-usuario');

    form.elements.id.value = usuario.id;
    form.elements.nome.value = usuario.nome || '';
    form.elements.email.value = usuario.email || '';
    form.elements.perfil.value = usuario.perfil || '';

    // A senha não é editada aqui: esconde o campo e tira o required.
    document.getElementById('campo-senha-usuario').classList.add('oculto');
    form.elements.senha.removeAttribute('required');
    form.elements.senha.value = '';

    document.getElementById('titulo-form-usuario').textContent = 'Editando usuário';
    document.getElementById('cancelar-usuario').classList.remove('oculto');
    form.scrollIntoView({behavior: 'smooth', block: 'start'});
}

function cancelarEdicaoUsuario() {
    const form = document.getElementById('form-usuario');
    form.reset();
    limparErros(form);

    document.getElementById('campo-senha-usuario').classList.remove('oculto');
    form.elements.senha.setAttribute('required', '');

    document.getElementById('titulo-form-usuario').textContent = 'Novo usuário';
    document.getElementById('cancelar-usuario').classList.add('oculto');
}

async function excluirUsuario(id) {
    const usuario = usuarios.find(u => u.id === id);
    if (!confirm('Excluir o usuário ' + usuario.nome + '?')) return;

    try {
        await requisicao('/usuarios/' + id, 'DELETE');
        mostrarToast('Usuário excluído.', 'ok');
        await carregarUsuarios();
    } catch (erro) {
        mostrarToast(erro.mensagem, 'erro');
    }
}


/* =========================== TROCA DE SENHA ========================== */

async function trocarSenha(evento) {
    evento.preventDefault();
    const form = evento.target;
    limparErros(form);
    if (!form.reportValidity()) return;

    const dados = lerFormulario(form);

    try {
        await requisicao('/auth/senha', 'PUT', {
            senhaAtual: dados.senhaAtual,
            novaSenha: dados.novaSenha
        });
        form.reset();
        mostrarToast('Senha trocada. Use a nova no próximo login.', 'ok');
    } catch (erro) {
        mostrarErros(form, erro);
    }
}


/* ============================== INÍCIO =============================== */

async function iniciar() {
    montarIcones();

    // Navegação da sidebar
    document.querySelectorAll('.nav-item[data-painel]').forEach(botao => {
        botao.addEventListener('click', () => trocarAba(botao.dataset.painel));
    });

    // Formulários
    document.getElementById('form-login').addEventListener('submit', entrar);
    document.getElementById('botao-sair').addEventListener('click', sair);
    document.getElementById('form-paciente').addEventListener('submit', salvarPaciente);
    document.getElementById('form-medico').addEventListener('submit', salvarMedico);
    document.getElementById('form-agendamento').addEventListener('submit', salvarAgendamento);
    document.getElementById('form-consulta').addEventListener('submit', salvarConsulta);
    document.getElementById('form-usuario').addEventListener('submit', salvarUsuario);
    document.getElementById('form-senha').addEventListener('submit', trocarSenha);
    document.getElementById('cancelar-paciente').addEventListener('click', cancelarEdicaoPaciente);
    document.getElementById('cancelar-medico').addEventListener('click', cancelarEdicaoMedico);
    document.getElementById('cancelar-consulta').addEventListener('click', cancelarEdicaoConsulta);
    document.getElementById('cancelar-usuario').addEventListener('click', cancelarEdicaoUsuario);
    document.getElementById('select-prontuario').addEventListener('change', evento => {
        if (evento.target.value) mostrarProntuario(Number(evento.target.value));
        else document.getElementById('conteudo-prontuario').innerHTML = '';
    });

    // Se já existe um token nesta aba, confirma com o back-end antes de entrar.
    const tokenSalvo = sessionStorage.getItem(CHAVE_TOKEN);
    if (!tokenSalvo) return;

    sessao.token = tokenSalvo;
    try {
        sessao.usuario = await requisicao('/auth/eu');
        await abrirAplicacao();
    } catch (erro) {
        // Token velho ou API fora: fica na tela de login.
        encerrarSessao();
    }
}

iniciar();
