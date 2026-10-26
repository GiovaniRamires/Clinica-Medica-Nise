# Clínica Médica Nise

Sistema de gestão para uma clínica médica: cadastro de pacientes e médicos, agendamento de consultas, registro de atendimentos (prontuário), prescrições e exames. O acesso é controlado por perfis de usuário (administrador, médico, recepcionista e paciente).

O projeto é composto por uma **API REST** em Spring Boot e um **front-end** em HTML/CSS/JavaScript que consome essa API.

---

## Tecnologias

- **Java 21**
- **Spring Boot 4.1** (Web MVC, Data JPA, Security, Validation)
- **MySQL** (banco de dados em runtime)
- **H2** (banco em memória, usado apenas nos testes)
- **Hibernate / JPA** (mapeamento objeto-relacional)
- **JWT (HS256) + BCrypt** (autenticação e senhas)
- **Maven** (build e dependências, via wrapper `mvnw`)
- **springdoc-openapi / Swagger UI** (documentação da API)
- **JUnit 5 + Mockito** (testes)

---

## Pré-requisitos

Antes de rodar, você precisa ter instalado:

1. **JDK 21** — confira com:
   ```bash
   java -version
   ```
   Deve mostrar a versão 21.

2. **MySQL Server** — rodando em `localhost:3306`.

> O Maven **não** precisa ser instalado: o projeto já vem com o wrapper (`mvnw` / `mvnw.cmd`).

---

## Configuração do banco de dados

A aplicação se conecta por padrão a um MySQL local no schema `clinica_nise`. O banco é **criado automaticamente** na primeira execução (`createDatabaseIfNotExist=true`) e as tabelas são geradas pelo Hibernate (`ddl-auto=update`), então você **não precisa criar tabelas manualmente**.

As credenciais e segredos **não ficam fixos no código** — vêm de variáveis de ambiente, com valores padrão:

| Variável | Para que serve | Padrão |
|---|---|---|
| `CLINICA_DB_URL` | URL de conexão JDBC | `jdbc:mysql://localhost:3306/clinica_nise?createDatabaseIfNotExist=true&serverTimezone=UTC` |
| `CLINICA_DB_USUARIO` | Usuário do MySQL | `root` |
| `CLINICA_DB_SENHA` | Senha do MySQL | *(vazio)* |
| `CLINICA_JWT_SECRET` | Chave de assinatura do token JWT (mín. 32 caracteres) | *(sorteada a cada execução)* |
| `CLINICA_ADMIN_EMAIL` | E-mail do primeiro administrador | `admin@clinicanise.local` |
| `CLINICA_ADMIN_SENHA` | Senha do primeiro administrador | *(sorteada e exibida no log)* |

> **Atenção:** se `CLINICA_JWT_SECRET` não for definida, uma chave aleatória é usada por execução — ou seja, os logins deixam de valer toda vez que a aplicação reinicia (você precisa logar de novo).

---

## Como executar

Na raiz do projeto, no terminal (Git Bash no Windows):

```bash
CLINICA_DB_SENHA="sua_senha_do_mysql" ./mvnw spring-boot:run
```

> Troque `sua_senha_do_mysql` pela senha do seu usuário `root`. Se o root não tiver senha, pode omitir a variável.

Para definir também a senha do administrador (recomendado, para você saber qual é):

```bash
CLINICA_DB_SENHA="sua_senha_do_mysql" CLINICA_ADMIN_SENHA="admin123" ./mvnw spring-boot:run
```

> A senha do admin só é aplicada **na primeira vez**, quando ainda não existe nenhum usuário no banco.

Quando aparecer no log `Started ClinicaMedicaNuseApplication...`, a aplicação está no ar.

### Acessando

| Recurso | Endereço |
|---|---|
| Interface (front-end) | http://localhost:8080 |
| Documentação da API (Swagger) | http://localhost:8080/swagger-ui.html |

Faça login com o e-mail do administrador (`admin@clinicanise.local`) e a senha definida (ou a que apareceu no log na primeira execução).

### Usando outra porta

Se a porta 8080 estiver ocupada, inicie em outra:

```bash
CLINICA_DB_SENHA="sua_senha_do_mysql" ./mvnw spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

E acesse em http://localhost:8081.

### Parando a aplicação

Pressione **Ctrl + C** no terminal onde ela está rodando. (Evite só fechar a janela — isso pode deixar a porta ocupada.)

---

## Perfis de acesso

O sistema controla o que cada usuário pode fazer através de perfis:

- **ADMINISTRADOR** — acesso total; é quem gerencia os usuários do sistema.
- **MEDICO** — atendimentos, prontuários, prescrições e exames.
- **RECEPCIONISTA** — pacientes e agenda.
- **PACIENTE** — cadastro próprio, suas consultas e solicitação de agendamento.

---

## Principais endpoints da API

> A URL base é `http://localhost:8080`. A maioria dos endpoints exige autenticação (token JWT obtido no login).

### Autenticação — `/auth`
| Método | Rota | Descrição |
|---|---|---|
| POST | `/auth/login` | Autentica e devolve o token |
| POST | `/auth/cadastro-paciente` | Autocadastro de paciente |
| POST | `/auth/logout` | Encerra a sessão |
| GET | `/auth/eu` | Dados do usuário logado |
| PUT | `/auth/senha` | Altera a própria senha |
| GET | `/auth/minhas-consultas` | Consultas/agendamentos do paciente logado |
| POST | `/auth/solicitar-agendamento` | Paciente solicita um agendamento |

### Pacientes — `/pacientes`
| Método | Rota | Descrição |
|---|---|---|
| POST | `/pacientes` | Cadastra paciente |
| GET | `/pacientes` | Lista pacientes |
| GET | `/pacientes/{id}` | Busca por id |
| PUT | `/pacientes/{id}` | Atualiza |
| DELETE | `/pacientes/{id}` | Remove |
| GET | `/pacientes/{id}/consultas` | Histórico de consultas |
| GET | `/pacientes/{id}/prontuario` | Prontuário completo |

### Médicos — `/medicos`
| Método | Rota | Descrição |
|---|---|---|
| POST | `/medicos` | Cadastra médico |
| GET | `/medicos` | Lista médicos |
| GET | `/medicos/{id}` | Busca por id |
| PUT | `/medicos/{id}` | Atualiza |
| DELETE | `/medicos/{id}` | Remove |

### Agendamentos — `/agendamentos`
| Método | Rota | Descrição |
|---|---|---|
| POST | `/agendamentos` | Cria agendamento |
| GET | `/agendamentos` | Lista agendamentos |
| GET | `/agendamentos/{id}` | Busca por id |
| PUT | `/agendamentos/{id}/realizado` | Marca como realizado |
| PUT | `/agendamentos/{id}/situacao` | Altera a situação |
| GET | `/agendamentos/horarios-disponiveis` | Horários livres de um médico numa data |

### Consultas — `/consultas`
| Método | Rota | Descrição |
|---|---|---|
| POST | `/consultas` | Registra atendimento |
| GET | `/consultas` | Lista consultas |
| GET | `/consultas/{id}` | Busca por id |
| PUT | `/consultas/{id}` | Atualiza |
| DELETE | `/consultas/{id}` | Remove |

### Exames — `/exames`
| Método | Rota | Descrição |
|---|---|---|
| POST | `/exames` | Solicita exame |
| GET | `/exames` | Lista exames (filtros: `pacienteId`, `pendentes`) |
| PUT | `/exames/{id}/situacao` | Altera a situação |
| PUT | `/exames/{id}/resultado` | Registra o resultado |

### Prescrições — `/prescricoes`
| Método | Rota | Descrição |
|---|---|---|
| POST | `/prescricoes` | Cria prescrição |
| GET | `/prescricoes/{id}` | Busca por id |
| GET | `/prescricoes?consultaId={id}` | Lista por consulta |
| GET | `/prescricoes?pacienteId={id}` | Lista por paciente |
| PUT | `/prescricoes/{id}` | Atualiza |
| DELETE | `/prescricoes/{id}` | Remove |

### Usuários — `/usuarios` *(restrito ao ADMINISTRADOR)*
| Método | Rota | Descrição |
|---|---|---|
| POST | `/usuarios` | Cria usuário |
| GET | `/usuarios` | Lista usuários |
| GET | `/usuarios/{id}` | Busca por id |
| PUT | `/usuarios/{id}` | Atualiza |
| DELETE | `/usuarios/{id}` | Remove |

---

## Testes

Os testes usam **H2 em memória**, então não precisam de MySQL rodando.

```bash
# Roda todos os testes
./mvnw test

# Roda uma classe específica
./mvnw test -Dtest=PacienteServiceTest
```

A suíte cobre:
- **Testes unitários de serviço** (com Mockito): regras de negócio de cada `Service`.
- **Testes de integração da API** (`ApiIntegracaoTest`): contrato dos endpoints, validações e fluxos ponta a ponta.

---

## Estrutura do projeto

```
Clinica-Medica-Nise/
├── front-end/                 # Interface web (HTML, CSS, JS)
├── src/
│   ├── main/
│   │   ├── java/.../ClinicaMedicaNuse/
│   │   │   ├── controller/    # Endpoints REST
│   │   │   ├── service/       # Regras de negócio
│   │   │   ├── repository/    # Acesso a dados (Spring Data JPA)
│   │   │   ├── entity/        # Entidades JPA
│   │   │   ├── dto/           # Objetos de requisição/resposta
│   │   │   ├── security/      # Configuração de segurança e JWT
│   │   │   └── exception/     # Tratamento centralizado de erros
│   │   └── resources/
│   │       └── application.properties
│   └── test/                  # Testes unitários e de integração
├── pom.xml                    # Dependências e build (Maven)
└── mvnw / mvnw.cmd            # Maven Wrapper
```

---

## Solução de problemas

| Problema | Causa provável | Solução |
|---|---|---|
| `release version 21 not supported` | Java instalado é anterior ao 21 | Instale o **JDK 21** e reabra o terminal |
| `Access denied for user 'root'@'localhost'` | Senha do MySQL incorreta/ausente | Informe `CLINICA_DB_SENHA` com a senha correta |
| `Port 8080 was already in use` | Outra execução segurando a porta | Pare o processo anterior (Ctrl+C) ou use `--server.port=8081` |
| Login sempre "inválido" após reiniciar | `CLINICA_JWT_SECRET` não definida (chave muda a cada execução) | Defina `CLINICA_JWT_SECRET` fixo ou apenas faça login novamente |

---

## Observações

- O `ddl-auto=update` é adequado para um projeto acadêmico/local, mas **não deve ser usado em produção** com dados reais.
- Segredos (senha do banco, chave JWT, senha do admin) ficam fora do repositório de propósito — configure-os por variáveis de ambiente.
