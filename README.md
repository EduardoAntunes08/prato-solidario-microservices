# Prato Solidário — Arquitetura de Microsserviços

Global Solution
**Microservice and Web Engineering & IT Services** — Profª Aparecida F. Castello Rosa

## Integrantes

| Nome | RM |
|---|---|
| Eduardo Antunes Camilo | 555534 |
| Romeo Miranda Moura | 557025 |

---

## O problema

Todo dia, padarias, hortifrútis, mercados e restaurantes descartam alimentos
ainda próprios para consumo — pães do dia anterior, frutas maduras, marmitas
que sobraram do almoço. Ao mesmo tempo, ONGs e cozinhas comunitárias que
atendem famílias em insegurança alimentar não têm como saber o que está
sobrando, onde, e por quanto tempo ainda dá para aproveitar.

O descarte não acontece por falta de alimento nem por falta de quem precise:
acontece por falta de **coordenação** entre quem tem o excedente e quem
consegue distribuí-lo a tempo.

## A solução

O **Prato Solidário** conecta os dois lados. O comércio publica o excedente; a
instituição reserva o que consegue retirar. O sistema controla o ciclo de vida
de cada doação para que a mesma sobra não seja prometida a duas instituições e
para que ninguém chegue ao local e encontre a geladeira vazia.

A solução é composta por **dois microsserviços** com **comunicação síncrona**:

| Microsserviço | Responsabilidade | Banco |
|---|---|---|
| **doacao-service** | Cadastro dos excedentes publicados pelos comércios | `doacoes` |
| **reserva-service** | Reservas feitas pelas instituições | `reservas` |

O `reserva-service` **não** altera a doação no banco do outro serviço: ele pede,
por HTTP via OpenFeign, que o `doacao-service` mude o status dela. É nessa
chamada que mora a comunicação síncrona.

### Ciclo de vida e comunicação síncrona

```
                    reserva-service                        doacao-service
                    ---------------                        --------------
POST   /reservas    Reserva: ATIVA           ──Feign──>    Doacao: DISPONIVEL -> RESERVADA
PATCH  /{id}/concluir  Reserva: CONCLUIDA    ──Feign──>    Doacao: RESERVADA  -> RETIRADA
PATCH  /{id}/cancelar  Reserva: CANCELADA    ──Feign──>    Doacao: RESERVADA  -> DISPONIVEL
```

Os dois lados têm um atributo de status, e **os dois mudam por meio da
comunicação síncrona**: o `reserva-service` altera o status da própria reserva e,
na mesma operação, manda o `doacao-service` alterar o status da doação.

---

## Arquitetura

```
                        ┌──────────────────────┐
                        │   eureka-server      │  :8761
                        │  (Service Discovery) │
                        └──────────▲───────────┘
                                   │ registro / descoberta
             ┌─────────────────────┼─────────────────────┐
             │                     │                     │
   ┌─────────┴────────┐  ┌─────────┴────────┐  ┌─────────┴────────┐
   │   api-gateway    │  │  doacao-service  │  │  reserva-service │
   │      :8082       │  │  porta aleatória │  │  porta aleatória │
   └─────────┬────────┘  └─────────▲────────┘  └─────────┬────────┘
             │                     │                     │
             │ lb://               └───── OpenFeign ─────┘
             │                       Circuit Breaker
             ▼                          + Fallback
   cliente (Swagger / Postman)
                                   ┌──────────────────────┐
                                   │  MySQL 8.4 (Docker)  │  :3306
                                   │  doacoes │ reservas  │
                                   └──────────────────────┘
```

| Componente | Porta | Observação |
|---|---|---|
| eureka-server | **8761** | dashboard em http://localhost:8761 |
| api-gateway | **8082** | único ponto de entrada |
| doacao-service | **aleatória** (`server.port=0`) | pode subir em várias instâncias |
| reserva-service | **aleatória** (`server.port=0`) | pode subir em várias instâncias |
| MySQL | 3306 | container Docker |

Nenhuma URL fixa é usada entre os serviços: o Gateway aponta para
`lb://doacao-service` e o OpenFeign para `@FeignClient("doacao-service")`. Quem
traduz o nome em endereço é o Eureka, e quem escolhe a instância é o Spring
Cloud LoadBalancer.

### Tecnologias

- Java **25**
- Spring Boot **4.0.9**
- Spring Cloud **2025.1.4** (Eureka, Gateway, OpenFeign, LoadBalancer, Resilience4j)
- MySQL **8.4.6** + Flyway (migrations)
- springdoc-openapi **3.0.3** (Swagger UI)
- Lombok
- Maven (wrapper incluído em cada projeto)

### Estrutura do repositório

```
prato-solidario-microservices/
├── docker-compose.yml          # MySQL
├── eureka-server/              # Service Discovery
├── api-gateway/                # Gateway + Swagger agregado
├── doacao-service/             # Microsserviço de doações
└── reserva-service/            # Microsserviço de reservas
```

Cada microsserviço segue arquitetura em camadas:

```
controller/   → recebe a requisição, devolve ResponseEntity (nunca acessa o repository)
service/      → regras de negócio, transações, exceções customizadas
repository/   → acesso ao banco (Spring Data JPA)
entity/       → entidades JPA e enums de domínio
dto/          → RequestDTO (entrada) e ResponseDTO (saída) — a entidade nunca é exposta
exception/    → exceções customizadas + @RestControllerAdvice
client/       → apenas no reserva-service: interface OpenFeign
```

---

## Como executar

### Pré-requisitos

| Ferramenta | Versão | Como conferir |
|---|---|---|
| JDK | **25** | `java -version` |
| Docker | qualquer recente | `docker --version` |
| Git | qualquer recente | `git --version` |

> Maven **não** precisa estar instalado: cada projeto traz o Maven Wrapper
> (`mvnw` / `mvnw.cmd`).

### 1. Clonar o repositório

```bash
git clone https://github.com/EduardoAntunes08/prato-solidario-microservices.git
cd prato-solidario-microservices
```

### 2. Subir o MySQL

Na **raiz do repositório** (onde está o `docker-compose.yml`):

```bash
docker compose up -d
```

Conferir se subiu:

```bash
docker compose ps
```

O MySQL fica em `localhost:3306`, usuário `root`, senha `root`. Os bancos
`doacoes` e `reservas` são **criados automaticamente** na primeira execução de
cada microsserviço (`createDatabaseIfNotExist=true`), e as tabelas e a carga
inicial de dados são aplicadas pelas **migrations do Flyway**. Não é preciso
rodar nenhum script SQL à mão.

### 3. Subir os serviços — nesta ordem

> A ordem importa: o Gateway e os microsserviços precisam do Eureka no ar para
> se registrar.

Abra **um terminal para cada serviço**.

**1) Eureka Server**

```bash
cd eureka-server
./mvnw spring-boot:run
```

Confirme em http://localhost:8761 — o dashboard deve abrir.

**2) API Gateway**

```bash
cd api-gateway
./mvnw spring-boot:run
```

**3) doacao-service**

```bash
cd doacao-service
./mvnw spring-boot:run
```

**4) reserva-service**

```bash
cd reserva-service
./mvnw spring-boot:run
```

No Windows use `mvnw.cmd` no lugar de `./mvnw`.

> **Antes de testar, confira o Eureka.** Abra http://localhost:8761 e espere
> `API-GATEWAY`, `DOACAO-SERVICE` e `RESERVA-SERVICE` aparecerem na lista de
> instâncias registradas — leva alguns segundos depois que cada serviço sobe.
>
> Se você chamar a API antes disso, o `reserva-service` ainda não conhece o
> endereço do `doacao-service` e a primeira chamada falha com **503**. Como o
> Circuit Breaker está configurado para abrir rápido (ver seção de
> resiliência), essa única falha já abre o circuito e as chamadas seguintes
> caem no fallback por 50 segundos. Não é defeito: é o disjuntor fazendo o
> trabalho dele. Espere o registro aparecer no Eureka e repita.

### Executando pelo IntelliJ IDEA

Abra **cada pasta de serviço como um projeto separado** (`File → Open` apontando
para `eureka-server/`, depois `api-gateway/`, etc.) e rode a classe principal:

| Projeto | Classe principal |
|---|---|
| eureka-server | `EurekaServerApplication` |
| api-gateway | `ApiGatewayApplication` |
| doacao-service | `MsDoacaoApplication` |
| reserva-service | `MsReservaApplication` |

Em `File → Project Structure → Project SDK`, selecione o **JDK 25**.

---

## Documentação da API (Swagger)

Como os microsserviços sobem em **porta aleatória**, acesse o Swagger **pelo
Gateway**, que tem porta fixa:

### 👉 http://localhost:8082/swagger-ui.html

Essa é a interface **agregada**: no seletor do canto superior direito, escolha
entre `doacao-service` e `reserva-service`. Todos os endpoints estão
documentados e são **executáveis** ali mesmo, pelo botão *Try it out*.

| O quê | URL |
|---|---|
| **Swagger UI agregado** | http://localhost:8082/swagger-ui.html |
| OpenAPI JSON — doações | http://localhost:8082/doacao-service/v3/api-docs |
| OpenAPI JSON — reservas | http://localhost:8082/reserva-service/v3/api-docs |

> **Por que não existe um `/doacao-service/swagger-ui.html`?**
> O Gateway remove o prefixo `/doacao-service` antes de repassar a chamada. O
> Swagger UI responde com um redirecionamento para `/swagger-ui/index.html`,
> que já sai sem o prefixo e não volta para o serviço certo. Por isso a
> interface usada é a agregada do Gateway, que é a que a solução publica.

Para abrir o Swagger UI direto em um microsserviço, use a porta que ele sorteou
(aparece no log, na linha `Tomcat started on port ...`, e no dashboard do
Eureka): `http://localhost:<porta>/swagger-ui.html`.

---

## Endpoints

Base pelo Gateway: `http://localhost:8082`

### doacao-service — `/doacao-service/api/v1/doacoes`

| Método | Rota | Sucesso | Erros |
|---|---|---|---|
| GET | `/api/v1/doacoes` | 200 | — |
| GET | `/api/v1/doacoes/{id}` | 200 | 404 |
| POST | `/api/v1/doacoes` | **201** + `Location` | 422 |
| PUT | `/api/v1/doacoes/{id}` | 200 | 404, 422 |
| DELETE | `/api/v1/doacoes/{id}` | **204** | 404, 409 |
| PUT | `/api/v1/doacoes/{id}/reservar` | 200 | 404, 409 |
| PUT | `/api/v1/doacoes/{id}/retirar` | 200 | 404, 409 |
| PUT | `/api/v1/doacoes/{id}/liberar` | 200 | 404, 409 |

As três últimas são as rotas chamadas pelo `reserva-service` via OpenFeign.

### reserva-service — `/reserva-service/api/v1/reservas`

| Método | Rota | Sucesso | Erros | Chama o outro serviço? |
|---|---|---|---|---|
| GET | `/api/v1/reservas` | 200 | — | não |
| GET | `/api/v1/reservas/{id}` | 200 | 404 | não |
| POST | `/api/v1/reservas` | **201** + `Location` | 404, 409, 422, 503 | **sim** → `reservar` |
| PUT | `/api/v1/reservas/{id}` | 200 | 404, 409, 422 | não |
| DELETE | `/api/v1/reservas/{id}` | **204** | 404, 409 | não |
| PATCH | `/api/v1/reservas/{id}/concluir` | 200 | 404, 409, 503 | **sim** → `retirar` |
| PATCH | `/api/v1/reservas/{id}/cancelar` | 200 | 404, 409, 503 | **sim** → `liberar` |

### Exemplos

Listar as doações (6 vêm da carga inicial do Flyway):

```bash
curl http://localhost:8082/doacao-service/api/v1/doacoes
```

Cadastrar uma doação:

```bash
curl -X POST http://localhost:8082/doacao-service/api/v1/doacoes \
  -H "Content-Type: application/json" \
  -d '{
        "doador": "Padaria Pao Quente",
        "alimento": "Paes do dia anterior",
        "categoria": "PAES",
        "quantidadeKg": 12.5,
        "dataValidade": "2027-03-15"
      }'
```

Responde **201** com o header `Location` apontando para o recurso criado, já
com o endereço público do Gateway:

```
Location: http://localhost:8082/doacao-service/api/v1/doacoes/7
```

É uma URL que você pode colar no navegador. Mesmo o microsserviço estando numa
porta aleatória, ele monta essa URI a partir dos cabeçalhos `X-Forwarded-*`
que o Gateway envia. Chamando o serviço direto, sem o Gateway, o `Location`
sai com o endereço do próprio serviço.

O status inicial `DISPONIVEL` é definido pelo sistema — não é aceito no corpo
da requisição.

Reservar essa doação (a doação 3 já começa `DISPONIVEL`):

```bash
curl -X POST http://localhost:8082/reserva-service/api/v1/reservas \
  -H "Content-Type: application/json" \
  -d '{
        "doacaoId": 3,
        "instituicao": "ONG Mesa Cheia",
        "responsavel": "Maria Oliveira",
        "dataRetirada": "2027-01-15"
      }'
```

Confira que a doação 3 virou `RESERVADA`:

```bash
curl http://localhost:8082/doacao-service/api/v1/doacoes/3
```

Concluir a reserva (use o `id` devolvido no POST):

```bash
curl -X PATCH http://localhost:8082/reserva-service/api/v1/reservas/4/concluir
```

A reserva vira `CONCLUIDA` e a doação 3 vira `RETIRADA`.

### Formato dos erros

```json
{
  "timestamp": "2026-11-13T23:40:12.345Z",
  "status": 404,
  "error": "Doacao nao encontrada. ID: 99",
  "path": "/api/v1/doacoes/99"
}
```

Erro de validação (**422**) detalha campo a campo:

```json
{
  "timestamp": "2026-11-13T23:41:03.120Z",
  "status": 422,
  "error": "Dados invalidos",
  "path": "/api/v1/doacoes",
  "errors": [
    { "fieldName": "quantidadeKg", "message": "O campo quantidadeKg deve ser um numero positivo maior que zero" }
  ]
}
```

---

## Testando a resiliência (Circuit Breaker e Fallback)

O `reserva-service` protege toda chamada ao `doacao-service` com
**Circuit Breaker (Resilience4j)** e **Fallback**.

Para ver funcionando:

1. Com tudo no ar, crie uma reserva para a doação 5:

   ```bash
   curl -X POST http://localhost:8082/reserva-service/api/v1/reservas \
     -H "Content-Type: application/json" \
     -d '{"doacaoId": 5, "instituicao": "ONG Mesa Cheia", "responsavel": "Maria Oliveira", "dataRetirada": "2027-02-01"}'
   ```

2. **Pare o `doacao-service`** (`Ctrl+C` no terminal dele).

3. Tente concluir a reserva recém-criada, usando o `id` que veio na resposta do
   passo 1 (com a carga inicial de 3 reservas, a nova costuma ser a de `id` 4):

   ```bash
   curl -i -X PATCH http://localhost:8082/reserva-service/api/v1/reservas/4/concluir
   ```

   Resposta: **503 Service Unavailable**, com o corpo da reserva marcada como
   **`PENDENTE_SINCRONIZACAO`**. O fallback entrou em ação: a reserva não ficou
   num estado mentiroso, ficou registrada para reconciliação.

4. Repita a chamada **3 vezes**. A partir da terceira, o log do
   `reserva-service` mostra que o circuito **abriu** — as chamadas seguintes nem
   tentam a rede, o fallback responde na hora:

   ```
   CircuitBreaker 'atualizarDoacao' is OPEN and does not permit further calls
   ```

5. Suba o `doacao-service` de novo. Após **50 segundos** o circuito vai para
   `HALF_OPEN` e libera chamadas de teste.

6. Chame o mesmo endpoint outra vez. Agora responde **200**, a reserva vai de
   `PENDENTE_SINCRONIZACAO` para `CONCLUIDA` e a doação é marcada como
   `RETIRADA` — a pendência deixada pelo fallback foi reconciliada e o circuito
   fechou:

   ```bash
   curl -X PATCH http://localhost:8082/reserva-service/api/v1/reservas/4/concluir
   curl http://localhost:8082/doacao-service/api/v1/doacoes/5
   ```

### Configuração do circuito

Em `reserva-service/src/main/resources/application.properties`:

| Propriedade | Valor | Significado |
|---|---|---|
| `slidingWindowSize` | 3 | monitora as últimas 3 chamadas |
| `slidingWindowType` | COUNT_BASED | janela por quantidade de chamadas |
| `minimumNumberOfCalls` | 2 | só calcula a taxa após 2 chamadas |
| `failureRateThreshold` | 50 | abre com 50% de falhas |
| `waitDurationInOpenState` | 50s | tempo em OPEN antes de HALF_OPEN |
| `permittedNumberOfCallsInHalfOpenState` | 2 | chamadas de teste em HALF_OPEN |

Erros de **negócio** (404 de doação inexistente, 409 de doação indisponível)
estão em `ignore-exceptions`: não contam como falha e não abrem o circuito. O
fallback, porém, continua sendo chamado pelo Resilience4j em qualquer erro —
quem garante que um 404 continua 404 e um 409 continua 409 é o repasse feito
no início de cada fallback do `ReservaController`.

---

## Testando múltiplas instâncias e Load Balancer

Os microsserviços sobem em **porta aleatória** (`server.port=0`) e recebem um
`instance-id` com sufixo aleatório, então dá para subir várias instâncias do
mesmo serviço sem conflito.

Abra um **segundo terminal** no `doacao-service` e rode de novo:

```bash
cd doacao-service
./mvnw spring-boot:run
```

No dashboard do Eureka (http://localhost:8761), `DOACAO-SERVICE` aparece com
**duas instâncias**, cada uma numa porta diferente. As chamadas feitas pelo
Gateway (`lb://doacao-service`) e pelo OpenFeign passam a ser distribuídas entre
as duas pelo Spring Cloud LoadBalancer.

---

## Banco de dados e migrations

Cada microsserviço tem **banco próprio e independente** — nenhum schema ou
tabela é compartilhado:

| Microsserviço | Banco | Tabela |
|---|---|---|
| doacao-service | `doacoes` | `tb_doacao` |
| reserva-service | `reservas` | `tb_reserva` |

O `doacaoId` guardado na reserva é uma **referência lógica**, não uma chave
estrangeira — justamente porque os bancos são separados.

As migrations ficam em `src/main/resources/db/migration` de cada serviço:

| Arquivo | O que faz |
|---|---|
| `V1__create_table_tb_doacao.sql` | cria a tabela de doações |
| `V2__insert_tb_doacao.sql` | carga inicial: 6 doações |
| `V1__create_table_tb_reserva.sql` | cria a tabela de reservas |
| `V2__insert_tb_reserva.sql` | carga inicial: 3 reservas |

As cargas iniciais são coerentes entre si: a doação 1 está `RESERVADA` (reserva 1
`ATIVA`), a doação 2 está `RETIRADA` (reserva 2 `CONCLUIDA`) e a doação 3 está
`DISPONIVEL` (reserva 3 foi `CANCELADA` e devolveu a doação).

Para inspecionar o banco:

```bash
docker exec -it mysql_prato_solidario mysql -uroot -proot
```

```sql
SHOW DATABASES;
USE doacoes;
SELECT * FROM tb_doacao;
SELECT * FROM flyway_schema_history;
```

### Recomeçar do zero

```bash
docker compose down
```

Apague a pasta `.data/` e suba de novo — os bancos, as tabelas e a carga inicial
são recriados automaticamente.

---

## Problemas comuns

**Reiniciei um microsserviço e as chamadas pelo Gateway começaram a dar 500**

Como a porta é aleatória, ao reiniciar o serviço sobe em uma porta nova. O
Eureka ainda mantém o registro da instância antiga por até ~90 segundos (tempo
de expiração do *lease*), e o Gateway continua alternando entre as duas — as
chamadas que caem na instância morta falham.

Não é erro de configuração: é o comportamento normal de registro do Eureka.
Espere cerca de um minuto e meio, ou confira em http://localhost:8761 que
sobrou só uma instância do serviço.


**Todas as rotas do Gateway respondem 500 com `UnknownHostException`**

Acontece quando os serviços se registram no Eureka com o nome de rede da
máquina (algo como `DESKTOP-XXXX.mshome.net`) em vez de `localhost`. O
resolvedor DNS do Netty, usado pelo API Gateway, não resolve esse tipo de nome
e derruba toda chamada roteada.

A solução já traz `eureka.instance.hostname=localhost` nos três serviços que se
registram, justamente para que isso não aconteça. Se o erro aparecer, confira
se essa linha continua nos `application.properties` do `api-gateway`, do
`doacao-service` e do `reserva-service`.

**A migration falhou e o serviço não sobe mais**

Pare a aplicação, entre no banco e limpe o registro da migration que falhou:

```sql
USE doacoes;   -- ou reservas
DELETE FROM flyway_schema_history WHERE success = 0;
```

Corrija o arquivo `.sql` e suba o serviço de novo. Sempre pare a aplicação
antes de criar ou alterar um arquivo de migration.

**`JAVA_HOME` apontando para outra versão**

Os microsserviços exigem **Java 25**. Confirme com `java -version`. No IntelliJ,
ajuste em `File → Project Structure → Project SDK`.

---

## Parando tudo

```bash
# Ctrl+C em cada terminal dos quatro serviços
docker compose down
```
