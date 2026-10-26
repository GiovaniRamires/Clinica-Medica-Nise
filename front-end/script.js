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
let exames = [];
let usuarios = [];


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

/** Primeiro nome, para a saudação "Olá, Maria!" no topo. */
function primeiroNome(nome) {
    return nome ? nome.trim().split(/\s+/)[0] : '';
}

const PERFIS = {
    ADMINISTRADOR: 'Administrador',
    MEDICO: 'Médico',
    RECEPCIONISTA: 'Recepcionista',
    PACIENTE: 'Paciente'
};

function perfilAtual() {
    return sessao.usuario ? sessao.usuario.perfil : null;
}

function ehAdministrador() {
    return perfilAtual() === 'ADMINISTRADOR';
}

function ehPaciente() {
    return perfilAtual() === 'PACIENTE';
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

/** Mostra um toast no canto da tela, no estilo do protótipo. */
function mostrarToast(texto, tipo = 'info') {
    const container = document.getElementById('toasts');
    const toast = document.createElement('div');
    toast.className = 'toast ' + (tipo === 'erro' ? 'erro' : tipo === 'ok' ? 'sucesso' : '');
    toast.textContent = texto;
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
    form.querySelectorAll('[aria-invalid="true"]').forEach(campo => campo.removeAttribute('aria-invalid'));
    form.querySelectorAll('.erro-campo').forEach(erro => {
        erro.textContent = '';
        erro.classList.add('oculto');
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
        entrada.setAttribute('aria-invalid', 'true');
        const campo = entrada.closest('.campo');
        const mensagem = campo && campo.querySelector('.erro-campo');
        if (mensagem) {
            mensagem.textContent = item.mensagem;
            mensagem.classList.remove('oculto');
        }
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
    document.querySelectorAll('.pagina').forEach(painel => {
        painel.classList.toggle('oculto', painel.id !== 'painel-' + nome);
    });
    document.getElementById('aplicacao').classList.remove('menu-aberto');
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
    exames = [];
    usuarios = [];

    document.getElementById('aplicacao').classList.add('oculto');
    document.getElementById('tela-cadastro-paciente').classList.add('oculto');
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
    document.getElementById('saudacao-nome').textContent = primeiroNome(usuario.nome) + '!';
    document.getElementById('usuario-avatar').textContent = iniciais(usuario.nome);

    document.getElementById('ficha-conta').innerHTML = `
        <div><p class="ficha-rotulo">Nome</p><p class="ficha-valor">${esc(usuario.nome)}</p></div>
        <div><p class="ficha-rotulo">E-mail</p><p class="ficha-valor">${esc(usuario.email)}</p></div>
        <div><p class="ficha-rotulo">Perfil</p><p class="ficha-valor">${esc(PERFIS[usuario.perfil] || usuario.perfil)}</p></div>
    `;

    aplicarPermissoes();
    trocarAba(ehPaciente() ? 'minhas-consultas' : 'dashboard');
    await carregarTudo();
}

/** Carrega só o que o perfil tem permissão de ler. */
async function carregarTudo() {
    try {
        if (ehPaciente()) {
            await carregarListaDeMedicosParaPaciente();
            await carregarMinhasConsultas();
            return;
        }
        await carregarPacientes();
        await carregarMedicos();
        await carregarAgendamentos();
        if (podeVerDadoClinico()) await carregarConsultas();
        if (podeVerDadoClinico()) await carregarExames();
        if (ehAdministrador()) await carregarUsuarios();
        renderizarDashboard();
    } catch (erro) {
        mostrarToast(erro.mensagem, 'erro');
    }
}


/* ======================= AUTO-CADASTRO DE PACIENTE ===================== */

function mostrarCadastroPaciente() {
    document.getElementById('tela-login').classList.add('oculto');
    document.getElementById('tela-cadastro-paciente').classList.remove('oculto');
}

function mostrarLogin() {
    document.getElementById('tela-cadastro-paciente').classList.add('oculto');
    document.getElementById('tela-login').classList.remove('oculto');
}

async function cadastrarPaciente(evento) {
    evento.preventDefault();
    const form = evento.target;
    const erro = document.getElementById('erro-cadastro-paciente');
    erro.classList.add('oculto');
    limparErros(form);

    if (!form.reportValidity()) return;

    const dados = lerFormulario(form);

    try {
        const resposta = await requisicao('/auth/cadastro-paciente', 'POST', {
            nome: dados.nome,
            cpf: dados.cpf,
            email: dados.email,
            senha: dados.senha,
            convenio: dados.convenio
        });
        sessao = {token: resposta.token, usuario: resposta.usuario};
        sessionStorage.setItem(CHAVE_TOKEN, resposta.token);
        form.reset();
        document.getElementById('tela-cadastro-paciente').classList.add('oculto');
        mostrarToast('Conta criada! Bem-vindo(a), ' + primeiroNome(resposta.usuario.nome) + '.', 'ok');
        await abrirAplicacao();
    } catch (falha) {
        if (falha.campos.length) {
            mostrarErros(form, falha);
        } else {
            erro.textContent = falha.mensagem;
            erro.classList.remove('oculto');
        }
    }
}


/* ========================= VISÃO DO PACIENTE =========================== */
/* O paciente logado só vê a própria agenda — nunca usa /pacientes,
   /medicos ou /agendamentos, que são das rotas administrativas. */

async function carregarListaDeMedicosParaPaciente() {
    const lista = await requisicao('/medicos');
    const itens = lista.map(m => ({valor: m.id, texto: m.nome + ' — ' + m.especialidade}));
    preencherSelect(document.getElementById('form-solicitar-consulta').elements.medicoId,
        itens, 'Nenhum médico disponível no momento');
}

async function carregarMinhasConsultas() {
    const minhas = await requisicao('/auth/minhas-consultas');
    document.getElementById('contador-minhas-consultas').textContent = minhas.length;
    const tabela = document.getElementById('tabela-minhas-consultas');

    if (minhas.length === 0) {
        tabela.innerHTML = '<tr><td class="vazio" colspan="4">Você ainda não tem consultas marcadas.</td></tr>';
        return;
    }

    tabela.innerHTML = minhas.map(a => `
        <tr>
            <td>${esc(formatarData(a.data))}</td>
            <td>${esc(a.horario)}</td>
            <td>${esc(a.medico.nome)} (${esc(a.medico.especialidade)})</td>
            <td><span class="selo selo--${esc(a.situacao)}">${esc(a.situacao)}</span></td>
        </tr>
    `).join('');
}

async function solicitarConsulta(evento) {
    evento.preventDefault();
    const form = evento.target;
    limparErros(form);
    if (!form.reportValidity()) return;

    const dados = lerFormulario(form);

    try {
        await requisicao('/auth/solicitar-agendamento', 'POST', {
            medicoId: Number(dados.medicoId),
            data: dados.data,
            horario: dados.horario,
            observacoes: dados.observacoes
        });
        mostrarToast('Solicitação enviada. A clínica vai confirmar o horário.', 'ok');
        form.reset();
        await carregarMinhasConsultas();
        trocarAba('minhas-consultas');
    } catch (erro) {
        mostrarErros(form, erro);
    }
}


/* ============================== DASHBOARD ============================= */
/* Espelha o Dashboard.tsx do protótipo: 4 cards de resumo, o card de próximo
   atendimento, ações rápidas, alertas e a lista de hoje/amanhã — só que com
   dados vindos da API em vez de valores fixos no componente. */

/** Atualiza o dashboard só se a aplicação já estiver na tela (evita
 *  renderizar antes da primeira carga, quando os elementos ainda não
 *  têm todos os dados prontos). */
function atualizarDashboardSeAberto() {
    if (!document.getElementById('aplicacao').classList.contains('oculto')) {
        renderizarDashboard();
    }
}

function renderizarDashboard() {
    const hoje = new Date().toISOString().slice(0, 10);
    const amanha = new Date(Date.now() + 86400000).toISOString().slice(0, 10);

    const agendaAbertaOrdenada = agendamentos
        .filter(a => SITUACOES_ABERTAS.includes(a.situacao))
        .sort((a, b) => (a.data + a.horario).localeCompare(b.data + b.horario));

    const deHojeOuAmanha = agendaAbertaOrdenada.filter(a => a.data === hoje || a.data === amanha);
    const proximo = agendaAbertaOrdenada[0];

    // ---- cards de resumo ----
    const cards = [
        {
            rotulo: 'Próximo atendimento',
            valor: proximo ? formatarData(proximo.data) : '—',
            sub: proximo ? proximo.horario : 'Nenhum agendado',
            cor: '#1d4ed8'
        },
        {rotulo: 'Agendamentos abertos', valor: agendaAbertaOrdenada.length, sub: 'Agendados ou confirmados', cor: '#16a34a'},
        {rotulo: 'Pacientes cadastrados', valor: pacientes.length, sub: 'Total na base', cor: '#16a34a'},
        {rotulo: 'Médicos ativos', valor: medicos.length, sub: 'No corpo clínico', cor: '#475569'}
    ];
    document.getElementById('cards-resumo').innerHTML = cards.map(c => `
        <div class="card-resumo">
            <p class="card-resumo-rotulo">${esc(c.rotulo)}</p>
            <p class="card-resumo-valor" style="color:${c.cor}">${esc(c.valor)}</p>
            <p class="card-resumo-sub">${esc(c.sub)}</p>
        </div>
    `).join('');

    // ---- card "próximo atendimento" ----
    const cartaoProximo = document.getElementById('cartao-proximo-atendimento');
    if (!proximo) {
        cartaoProximo.innerHTML = `
            <div class="cartao-corpo">
                <h2 class="cartao-titulo" style="margin-bottom:8px">Nenhum atendimento agendado</h2>
                <p class="dica">Agende uma consulta para o próximo paciente aparecer aqui.</p>
            </div>`;
    } else {
        cartaoProximo.innerHTML = `
            <div class="cartao-corpo">
                <div class="proximo-atendimento-cabecalho">
                    <div>
                        <h2 class="cartao-titulo" style="margin-bottom:8px">Próximo atendimento</h2>
                        <span class="selo selo--${esc(proximo.situacao)}">${esc(proximo.situacao)}</span>
                    </div>
                    <span class="dica">${esc(formatarData(proximo.data))}</span>
                </div>
                <div class="proximo-atendimento-linhas">
                    <div><p class="ficha-rotulo">Paciente</p><p class="ficha-valor">${esc(proximo.paciente.nome)}</p></div>
                    <div><p class="ficha-rotulo">Médico</p><p class="ficha-valor">${esc(proximo.medico.nome)} (${esc(proximo.medico.especialidade)})</p></div>
                    <div><p class="ficha-rotulo">Horário</p><p class="ficha-valor">${esc(proximo.horario)}</p></div>
                    <div><p class="ficha-rotulo">Observações</p><p class="ficha-valor">${esc(proximo.observacoes)}</p></div>
                </div>
            </div>`;
    }

    // ---- alertas ----
    const alertas = [];
    const semMedico = pacientes.length > 0 && medicos.length === 0;
    if (semMedico) {
        alertas.push({
            tipo: 'aviso', icone: '⚠️', titulo: 'Nenhum médico cadastrado',
            texto: 'Cadastre um médico para poder marcar agendamentos.'
        });
    }
    const consultasAFazer = agendamentos.filter(a => SITUACOES_ABERTAS.includes(a.situacao) && (a.data === hoje));
    if (consultasAFazer.length > 0 && podeVerDadoClinico()) {
        alertas.push({
            tipo: 'info', icone: 'ℹ️', titulo: `${consultasAFazer.length} atendimento(s) hoje`,
            texto: 'Confira a agenda do dia antes de começar os atendimentos.'
        });
    }
    document.getElementById('alertas-dashboard').innerHTML = alertas.map(a => `
        <div class="alerta alerta--${a.tipo}">
            <span class="alerta-icone">${a.icone}</span>
            <div>
                <p class="alerta-titulo">${esc(a.titulo)}</p>
                <p class="alerta-texto">${esc(a.texto)}</p>
            </div>
        </div>
    `).join('');

    // ---- lista de hoje/amanhã ----
    const lista = document.getElementById('lista-dashboard');
    if (deHojeOuAmanha.length === 0) {
        lista.innerHTML = '<p class="vazio" style="padding:20px 24px">Nenhum agendamento para hoje ou amanhã.</p>';
    } else {
        lista.innerHTML = deHojeOuAmanha.map(a => `
            <div class="item-lista-dashboard">
                <div>
                    <p class="item-lista-titulo">${esc(a.paciente.nome)}</p>
                    <p class="item-lista-sub">${esc(formatarData(a.data))} · ${esc(a.horario)} · ${esc(a.medico.nome)}</p>
                </div>
                <span class="selo selo--${esc(a.situacao)}">${esc(a.situacao)}</span>
            </div>
        `).join('');
    }

    // O cartão de demonstração só faz sentido com o banco vazio.
    document.getElementById('cartao-demo').classList.toggle('oculto', pacientes.length > 0 || medicos.length > 0);
}

/**
 * Cria pacientes, médicos e agendamentos fictícios pela própria API, no
 * mesmo espírito dos dados de exemplo do protótipo Figma (lá: professor
 * Carlos Almeida, turma ADS, alunos João Silva/Maria Souza/...). Aqui:
 * nomes, CPFs e CRMs fictícios de uma clínica.
 */
async function criarDadosDemonstracao() {
    const botao = document.getElementById('botao-seed');
    botao.disabled = true;
    botao.textContent = 'Criando…';

    try {
        const medicosSeed = [
            {nome: 'Dra. Erika Miranda', crm: '111111/SP', especialidade: 'Cardiologia'},
            {nome: 'Dr. Rafael Nunes', crm: '222222/SP', especialidade: 'Clínica Geral'}
        ];
        const pacientesSeed = [
            {nome: 'João Silva', cpf: '11111111111', convenio: 'Unimed', alergias: 'Dipirona', prontuario: 'Hipertenso, uso continuo de losartana.'},
            {nome: 'Maria Souza', cpf: '22222222222', convenio: 'Particular', alergias: 'Nenhuma conhecida', prontuario: 'Sem comorbidades.'},
            {nome: 'Ana Oliveira', cpf: '33333333333', convenio: 'Bradesco Saúde', alergias: 'Penicilina', prontuario: 'Alérgica a penicilina, atenção na prescrição.'},
            {nome: 'Lucas Santos', cpf: '44444444444', convenio: 'Particular', alergias: 'Nenhuma conhecida', prontuario: ''}
        ];

        const idsMedicos = [];
        for (const m of medicosSeed) {
            const criado = await requisicao('/medicos', 'POST', m);
            idsMedicos.push(criado.id);
        }

        const idsPacientes = [];
        for (const p of pacientesSeed) {
            const criado = await requisicao('/pacientes', 'POST', p);
            idsPacientes.push(criado.id);
        }

        const hoje = new Date();
        const horarios = ['09:00', '10:30', '14:00', '16:00'];
        for (let i = 0; i < idsPacientes.length; i++) {
            const data = new Date(hoje.getTime() + i * 86400000).toISOString().slice(0, 10);
            await requisicao('/agendamentos', 'POST', {
                pacienteId: idsPacientes[i],
                medicoId: idsMedicos[i % idsMedicos.length],
                data: data,
                horario: horarios[i % horarios.length],
                observacoes: i === 0 ? 'Retorno de exames' : ''
            });
        }

        mostrarToast('Dados de demonstração criados: ' + pacientesSeed.length + ' pacientes, '
            + medicosSeed.length + ' médicos e ' + idsPacientes.length + ' agendamentos.', 'ok');
        await carregarTudo();
    } catch (erro) {
        mostrarToast(erro.mensagem, 'erro');
    } finally {
        botao.disabled = false;
        botao.textContent = '✨ Criar dados de demonstração';
    }
}


/* ============================= PACIENTES ============================= */

async function carregarPacientes() {
    pacientes = await requisicao('/pacientes');
    mostrarPacientes();
    atualizarSelectsDePaciente();
    atualizarDashboardSeAberto();
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
        let acoes = `<button class="botao botao--secundario botao--pequeno" onclick="editarPaciente(${paciente.id})">Editar</button>`;
        if (clinico) {
            acoes += `<button class="botao botao--secundario botao--pequeno" onclick="verProntuario(${paciente.id})">Prontuário</button>`;
        }
        if (ehAdministrador()) {
            acoes += `<button class="botao botao--perigo botao--pequeno" onclick="excluirPaciente(${paciente.id})">Excluir</button>`;
        }
        // Alergias é dado clínico: a coluna só existe para quem pode ver.
        const alergias = clinico ? `<td>${esc(paciente.alergias)}</td>` : '';
        return `
            <tr>
                <td>${esc(paciente.nome)}</td>
                <td>${esc(formatarCpf(paciente.cpf))}</td>
                <td>${esc(paciente.convenio)}</td>
                ${alergias}
                <td><div class="acoes-linha">${acoes}</div></td>
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
    atualizarDashboardSeAberto();
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
            ? `<button class="botao botao--secundario botao--pequeno" onclick="editarMedico(${medico.id})">Editar</button>
               <button class="botao botao--perigo botao--pequeno" onclick="excluirMedico(${medico.id})">Excluir</button>`
            : '—';
        return `
            <tr>
                <td>${esc(medico.nome)}</td>
                <td>${esc(medico.crm)}</td>
                <td>${esc(medico.especialidade)}</td>
                <td><div class="acoes-linha">${acoes}</div></td>
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
    atualizarDashboardSeAberto();
    atualizarSino();
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

        if (agendamento.situacao === 'SOLICITADO') {
            // Solicitação do próprio paciente: a clínica confirma o horário
            // (vira AGENDADO) ou recusa (vira CANCELADO).
            acoes += `<button class="botao botao--primario botao--pequeno" onclick="alterarSituacao(${agendamento.id}, 'AGENDADO')">Confirmar</button>
                      <button class="botao botao--perigo botao--pequeno" onclick="alterarSituacao(${agendamento.id}, 'CANCELADO')">Recusar</button>`;
        }
        if (agendamento.situacao === 'AGENDADO') {
            acoes += `<button class="botao botao--secundario botao--pequeno" onclick="alterarSituacao(${agendamento.id}, 'CONFIRMADO')">Confirmar</button>`;
        }
        if (aberto && podeVerDadoClinico()) {
            acoes += `<button class="botao botao--primario botao--pequeno" onclick="atenderAgendamento(${agendamento.id})">Atender</button>`;
        }
        if (aberto) {
            acoes += `<button class="botao botao--secundario botao--pequeno" onclick="alterarSituacao(${agendamento.id}, 'AUSENTE')">Faltou</button>
                      <button class="botao botao--perigo botao--pequeno" onclick="alterarSituacao(${agendamento.id}, 'CANCELADO')">Cancelar</button>`;
        }

        return `
            <tr>
                <td>${esc(formatarData(agendamento.data))}</td>
                <td>${esc(agendamento.horario)}</td>
                <td>${esc(agendamento.paciente.nome)}</td>
                <td>${esc(agendamento.medico.nome)} (${esc(agendamento.medico.especialidade)})</td>
                <td><span class="selo selo--${esc(agendamento.situacao)}">${esc(agendamento.situacao)}</span></td>
                <td><div class="acoes-linha">${acoes || '—'}</div></td>
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
        atualizarHorariosAgendamento();
        await carregarAgendamentos();
    } catch (erro) {
        mostrarErros(form, erro);
    }
}

async function atualizarHorariosAgendamento() {
    const form = document.getElementById('form-agendamento');
    const medicoId = form.elements.medicoId.value;
    const data = form.elements.data.value;
    const caixa = document.getElementById('age-horarios');
    const info = document.getElementById('age-horarios-info');
    const hidden = document.getElementById('age-horario');

    hidden.value = '';
    info.textContent = '';

    if (!medicoId || !data) {
        caixa.innerHTML = '<p class="dica">Escolha o médico e a data para ver os horários.</p>';
        return;
    }

    try {
        const agenda = await requisicao('/agendamentos/horarios-disponiveis?medicoId=' + medicoId + '&data=' + data);
        if (!agenda.horarios.length) {
            caixa.innerHTML = '<p class="dica">Nenhum horário nesse dia.</p>';
            return;
        }
        caixa.innerHTML = agenda.horarios.map(h =>
            '<button type="button" class="slot-horario" data-hora="' + h.horario + '"' +
            (h.livre ? '' : ' disabled') + '>' + h.horario + '</button>'
        ).join('');
        info.textContent = agenda.livres + ' de ' + agenda.totalSlots +
            ' horários livres · consultas de ' + agenda.duracaoMinutos +
            ' min, com intervalo mínimo de ' + agenda.duracaoMinutos + ' min entre agendamentos.';

        caixa.querySelectorAll('.slot-horario:not(:disabled)').forEach(botao => {
            botao.addEventListener('click', () => {
                caixa.querySelectorAll('.slot-horario').forEach(b => b.classList.remove('ativo'));
                botao.classList.add('ativo');
                hidden.value = botao.dataset.hora;
            });
        });
    } catch (erro) {
        caixa.innerHTML = '<p class="dica">Não foi possível carregar os horários.</p>';
        mostrarToast(erro.mensagem, 'erro');
    }
}

function coletarNotificacoes() {
    const hoje = new Date().toISOString().slice(0, 10);
    const pendentes = agendamentos.filter(a => a.situacao === 'SOLICITADO');
    const deHoje = agendamentos.filter(a => a.data === hoje && SITUACOES_ABERTAS.includes(a.situacao));
    return {pendentes, deHoje, total: pendentes.length + deHoje.length};
}

function atualizarSino() {
    const contador = document.getElementById('sino-contador');
    if (ehPaciente()) {
        contador.classList.add('oculto');
        return;
    }
    const total = coletarNotificacoes().pendentes.length;
    if (total > 0) {
        contador.textContent = total > 9 ? '9+' : String(total);
        contador.classList.remove('oculto');
    } else {
        contador.classList.add('oculto');
    }
}

function montarPainelSino() {
    const corpo = document.getElementById('sino-painel-corpo');
    const {pendentes, deHoje, total} = coletarNotificacoes();

    if (total === 0) {
        corpo.innerHTML = '<p class="sino-vazio">Nada por aqui. Tudo em dia!</p>';
        return;
    }

    let html = '';
    if (pendentes.length > 0) {
        html += '<p class="sino-grupo-titulo">Solicitações pendentes</p>';
        html += pendentes.map(a =>
            '<div class="sino-item"><strong>' + esc(a.paciente.nome) + '</strong> pediu horário com ' +
            esc(a.medico.nome) + '<div class="sino-item-sub">' + esc(formatarData(a.data)) + ' · ' + esc(a.horario) + '</div></div>'
        ).join('');
    }
    if (deHoje.length > 0) {
        html += '<p class="sino-grupo-titulo">Agendamentos de hoje</p>';
        html += deHoje.map(a =>
            '<div class="sino-item"><strong>' + esc(a.horario) + '</strong> · ' + esc(a.paciente.nome) +
            '<div class="sino-item-sub">' + esc(a.medico.nome) + ' · ' + esc(a.situacao) + '</div></div>'
        ).join('');
    }
    corpo.innerHTML = html;
}

function alternarSino() {
    const painel = document.getElementById('sino-painel');
    const botao = document.getElementById('botao-sino');
    const aberto = !painel.classList.contains('oculto');
    if (aberto) {
        painel.classList.add('oculto');
        botao.setAttribute('aria-expanded', 'false');
    } else {
        montarPainelSino();
        painel.classList.remove('oculto');
        botao.setAttribute('aria-expanded', 'true');
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
    atualizarSelectDeConsulta();
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
            <td><div class="acoes-linha"><button class="botao botao--secundario botao--pequeno" onclick="editarConsulta(${consulta.id})">Editar</button></div></td>
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
            await requisicao('/consultas/' + id, 'PUT', {
                queixaPrincipal: dados.queixaPrincipal,
                observacoesMedicas: dados.observacoesMedicas,
                diagnostico: dados.diagnostico,
                conduta: dados.conduta
            });
            mostrarToast('Consulta atualizada.', 'ok');
            cancelarEdicaoConsulta();
            await carregarConsultas();
            await carregarAgendamentos();
        } else {
            const criada = await requisicao('/consultas', 'POST', {
                agendamentoId: Number(dados.agendamentoId),
                queixaPrincipal: dados.queixaPrincipal,
                observacoesMedicas: dados.observacoesMedicas,
                diagnostico: dados.diagnostico,
                conduta: dados.conduta
            });
            mostrarToast('Atendimento registrado. Adicione as prescrições abaixo.', 'ok');
            await carregarConsultas();
            await carregarAgendamentos();
            editarConsulta(criada.id);
        }
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
    abrirPrescricoes(consulta.id);
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
    fecharPrescricoes();
}

let prescricoes = [];

async function abrirPrescricoes(consultaId) {
    document.getElementById('form-prescricao').elements.consultaId.value = consultaId;
    document.getElementById('cartao-prescricoes').classList.remove('oculto');
    cancelarEdicaoPrescricao();
    await carregarPrescricoes(consultaId);
}

function fecharPrescricoes() {
    document.getElementById('cartao-prescricoes').classList.add('oculto');
    document.getElementById('form-prescricao').elements.consultaId.value = '';
    prescricoes = [];
}

async function carregarPrescricoes(consultaId) {
    prescricoes = await requisicao('/prescricoes?consultaId=' + consultaId);
    mostrarPrescricoes();
}

function mostrarPrescricoes() {
    document.getElementById('contador-prescricoes').textContent = prescricoes.length;
    const tabela = document.getElementById('tabela-prescricoes');

    if (prescricoes.length === 0) {
        tabela.innerHTML = '<tr><td class="vazio" colspan="5">Nenhuma prescrição nesta consulta.</td></tr>';
        return;
    }

    tabela.innerHTML = prescricoes.map(p => `
        <tr>
            <td>${esc(p.medicamento)}</td>
            <td>${esc(p.dosagem)}</td>
            <td>${esc(p.frequencia)}</td>
            <td>${esc(p.duracao || '—')}</td>
            <td><div class="acoes-linha">
                <button class="botao botao--secundario botao--pequeno" onclick="editarPrescricao(${p.id})">Editar</button>
                <button class="botao botao--perigo botao--pequeno" onclick="excluirPrescricao(${p.id})">Excluir</button>
            </div></td>
        </tr>
    `).join('');
}

async function salvarPrescricao(evento) {
    evento.preventDefault();
    const form = evento.target;
    limparErros(form);
    if (!form.reportValidity()) return;

    const dados = lerFormulario(form);
    const id = dados.id;
    const corpo = {
        consultaId: Number(dados.consultaId),
        medicamento: dados.medicamento,
        dosagem: dados.dosagem,
        frequencia: dados.frequencia,
        duracao: dados.duracao,
        observacoes: dados.observacoes
    };

    try {
        if (id) {
            await requisicao('/prescricoes/' + id, 'PUT', corpo);
            mostrarToast('Prescrição atualizada.', 'ok');
        } else {
            await requisicao('/prescricoes', 'POST', corpo);
            mostrarToast('Prescrição adicionada.', 'ok');
        }
        cancelarEdicaoPrescricao();
        await carregarPrescricoes(dados.consultaId);
    } catch (erro) {
        mostrarErros(form, erro);
    }
}

function editarPrescricao(id) {
    const p = prescricoes.find(item => item.id === id);
    const form = document.getElementById('form-prescricao');

    form.elements.id.value = p.id;
    form.elements.medicamento.value = p.medicamento || '';
    form.elements.dosagem.value = p.dosagem || '';
    form.elements.frequencia.value = p.frequencia || '';
    form.elements.duracao.value = p.duracao || '';
    form.elements.observacoes.value = p.observacoes || '';

    document.getElementById('cancelar-prescricao').classList.remove('oculto');
    form.scrollIntoView({behavior: 'smooth', block: 'start'});
}

function cancelarEdicaoPrescricao() {
    const form = document.getElementById('form-prescricao');
    const consultaId = form.elements.consultaId.value;
    form.reset();
    form.elements.consultaId.value = consultaId;
    limparErros(form);
    document.getElementById('cancelar-prescricao').classList.add('oculto');
}

async function excluirPrescricao(id) {
    if (!confirm('Excluir esta prescrição?')) return;
    const consultaId = document.getElementById('form-prescricao').elements.consultaId.value;
    try {
        await requisicao('/prescricoes/' + id, 'DELETE');
        mostrarToast('Prescrição excluída.', 'ok');
        await carregarPrescricoes(consultaId);
    } catch (erro) {
        mostrarToast(erro.mensagem, 'erro');
    }
}


/* ============================== EXAMES ==============================
 * RF07: solicitar exame a partir de uma consulta.
 * RF08: cada exame guarda paciente, consulta de origem, nome, data da
 *       solicitação, situação, data de realização e resultado.
 * RF09: listar os exames de um paciente (filtro na tabela).
 * RF10: filtro "Só pendentes" mostra os ainda não realizados.
 */

async function carregarExames() {
    const somentePendentes = document.getElementById('filtro-exames-pendentes').checked;
    exames = await requisicao(somentePendentes ? '/exames?pendentes=true' : '/exames');
    mostrarExames();
}

/** Textos dos botões de situação conforme o estado atual do exame. */
function acoesExame(exame) {
    let acoes = '';
    if (exame.situacao === 'SOLICITADO') {
        acoes += `<button class="botao botao--secundario botao--pequeno" onclick="alterarSituacaoExame(${exame.id}, 'AGENDADO')">Agendar</button>`;
    }
    if (exame.situacao === 'SOLICITADO' || exame.situacao === 'AGENDADO') {
        acoes += `<button class="botao botao--primario botao--pequeno" onclick="registrarResultadoExame(${exame.id})">Registrar resultado</button>
                  <button class="botao botao--perigo botao--pequeno" onclick="alterarSituacaoExame(${exame.id}, 'CANCELADO')">Cancelar</button>`;
    }
    if (exame.situacao === 'REALIZADO' && exame.resultado) {
        acoes += `<button class="botao botao--secundario botao--pequeno" onclick="verResultadoExame(${exame.id})">Ver resultado</button>`;
    }
    return acoes || '—';
}

function mostrarExames() {
    document.getElementById('contador-exames').textContent = exames.length;
    const tabela = document.getElementById('tabela-exames');

    if (exames.length === 0) {
        tabela.innerHTML = '<tr><td class="vazio" colspan="7">Nenhum exame solicitado.</td></tr>';
        return;
    }

    tabela.innerHTML = exames.map(exame => `
        <tr>
            <td>${esc(formatarData(exame.dataSolicitacao))}</td>
            <td>${esc(exame.paciente.nome)}</td>
            <td>${esc(exame.nome)}</td>
            <td><span class="selo selo--${esc(exame.situacao)}">${esc(exame.situacao)}</span></td>
            <td>${esc(formatarData(exame.dataRealizacao))}</td>
            <td>${exame.resultado ? esc(exame.resultado.length > 40 ? exame.resultado.slice(0, 40) + '…' : exame.resultado) : '—'}</td>
            <td><div class="acoes-linha">${acoesExame(exame)}</div></td>
        </tr>
    `).join('');
}

/** Monta o select de consultas do formulário de solicitação de exame. */
function atualizarSelectDeConsulta() {
    const itens = consultas.map(c => ({
        valor: c.id,
        texto: formatarData(c.data) + ' — ' + c.paciente.nome + ' / ' + c.medico.nome
    }));
    preencherSelect(document.getElementById('form-exame').elements.consultaId,
        itens, 'Registre uma consulta primeiro');
}

async function solicitarExame(evento) {
    evento.preventDefault();
    const form = evento.target;
    limparErros(form);
    if (!form.reportValidity()) return;

    const dados = lerFormulario(form);

    try {
        await requisicao('/exames', 'POST', {
            consultaId: Number(dados.consultaId),
            nome: dados.nome
        });
        mostrarToast('Exame solicitado.', 'ok');
        form.reset();
        await carregarExames();
    } catch (erro) {
        mostrarErros(form, erro);
    }
}

async function alterarSituacaoExame(id, situacao) {
    try {
        await requisicao('/exames/' + id + '/situacao', 'PUT', {situacao: situacao});
        mostrarToast('Exame marcado como ' + situacao + '.', 'ok');
        await carregarExames();
    } catch (erro) {
        mostrarToast(erro.mensagem, 'erro');
    }
}

async function registrarResultadoExame(id) {
    const resultado = prompt('Resultado do exame:');
    if (resultado === null) return;             // cancelou o prompt
    if (resultado.trim() === '') {
        mostrarToast('O resultado não pode ficar em branco.', 'erro');
        return;
    }
    try {
        await requisicao('/exames/' + id + '/resultado', 'PUT', {resultado: resultado.trim()});
        mostrarToast('Resultado registrado.', 'ok');
        await carregarExames();
    } catch (erro) {
        mostrarToast(erro.mensagem, 'erro');
    }
}

function verResultadoExame(id) {
    const exame = exames.find(e => e.id === id);
    if (exame) alert('Resultado de "' + exame.nome + '":\n\n' + exame.resultado);
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
        : prontuario.consultas.map(consulta => `
            <article class="item-prontuario">
                <h3>${esc(formatarData(consulta.data))} às ${esc(consulta.horario)} —
                    ${esc(consulta.medico.nome)} (${esc(consulta.medico.especialidade)})</h3>
                <p><strong>Queixa:</strong> ${esc(consulta.queixaPrincipal)}</p>
                <p><strong>Diagnóstico:</strong> ${esc(consulta.diagnostico)}</p>
                <p><strong>Conduta:</strong> ${esc(consulta.conduta)}</p>
                <p><strong>Observações:</strong> ${esc(consulta.observacoesMedicas)}</p>
            </article>
        `).join('<div style="height:12px"></div>');

    destino.innerHTML = `
        <div class="cartao">
            <div class="cartao-cabecalho"><h2 class="cartao-titulo">Prontuário de ${esc(paciente.nome)}</h2></div>
            <div class="cartao-corpo">
                <div class="grade-ficha">
                    <div><p class="ficha-rotulo">Nome</p><p class="ficha-valor">${esc(paciente.nome)}</p></div>
                    <div><p class="ficha-rotulo">CPF</p><p class="ficha-valor">${esc(formatarCpf(paciente.cpf))}</p></div>
                    <div><p class="ficha-rotulo">Convênio</p><p class="ficha-valor">${esc(paciente.convenio)}</p></div>
                    <div><p class="ficha-rotulo">Alergias</p><p class="ficha-valor">${esc(paciente.alergias)}</p></div>
                </div>
                <p class="ficha-rotulo" style="margin-top:18px">Anotações gerais</p>
                <p class="ficha-valor">${esc(paciente.prontuario)}</p>
            </div>
        </div>
        <div class="cartao">
            <div class="cartao-cabecalho">
                <h2 class="cartao-titulo">Histórico de consultas</h2>
                <span class="selo selo--neutro">${prontuario.totalConsultas}</span>
            </div>
            <div class="cartao-corpo">${historico}</div>
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
            : `<button class="botao botao--perigo botao--pequeno" onclick="excluirUsuario(${usuario.id})">Excluir</button>`;
        return `
            <tr>
                <td>${esc(usuario.nome)}${ehVoce ? ' <span class="selo selo--neutro">você</span>' : ''}</td>
                <td>${esc(usuario.email)}</td>
                <td>${esc(PERFIS[usuario.perfil] || usuario.perfil)}</td>
                <td><div class="acoes-linha"><button class="botao botao--secundario botao--pequeno" onclick="editarUsuario(${usuario.id})">Editar</button>${excluir}</div></td>
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
    // Navegação da sidebar
    document.querySelectorAll('.nav-item[data-painel]').forEach(botao => {
        botao.addEventListener('click', () => trocarAba(botao.dataset.painel));
    });

    // Menu mobile (a sidebar fica fora da tela em telas estreitas)
    document.getElementById('botao-menu-mobile').addEventListener('click', () => {
        document.getElementById('aplicacao').classList.add('menu-aberto');
    });
    document.getElementById('overlay-mobile').addEventListener('click', () => {
        document.getElementById('aplicacao').classList.remove('menu-aberto');
    });

    document.getElementById('botao-sino').addEventListener('click', evento => {
        evento.stopPropagation();
        alternarSino();
    });
    document.addEventListener('click', evento => {
        const container = document.querySelector('.sino-container');
        if (container && !container.contains(evento.target)) {
            document.getElementById('sino-painel').classList.add('oculto');
            document.getElementById('botao-sino').setAttribute('aria-expanded', 'false');
        }
    });

    // Formulários
    document.getElementById('form-login').addEventListener('submit', entrar);
    document.getElementById('botao-sair').addEventListener('click', sair);
    document.getElementById('abrir-cadastro-paciente').addEventListener('click', mostrarCadastroPaciente);
    document.getElementById('voltar-para-login').addEventListener('click', mostrarLogin);
    document.getElementById('form-cadastro-paciente').addEventListener('submit', cadastrarPaciente);
    document.getElementById('form-solicitar-consulta').addEventListener('submit', solicitarConsulta);
    document.getElementById('form-paciente').addEventListener('submit', salvarPaciente);
    document.getElementById('form-medico').addEventListener('submit', salvarMedico);
    document.getElementById('form-agendamento').addEventListener('submit', salvarAgendamento);
    document.getElementById('age-medico').addEventListener('change', atualizarHorariosAgendamento);
    document.getElementById('age-data').addEventListener('change', atualizarHorariosAgendamento);
    document.getElementById('form-consulta').addEventListener('submit', salvarConsulta);
    document.getElementById('form-prescricao').addEventListener('submit', salvarPrescricao);
    document.getElementById('cancelar-prescricao').addEventListener('click', cancelarEdicaoPrescricao);
    document.getElementById('form-exame').addEventListener('submit', solicitarExame);
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
    document.getElementById('filtro-exames-pendentes').addEventListener('change', () => {
        carregarExames().catch(erro => mostrarToast(erro.mensagem, 'erro'));
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
