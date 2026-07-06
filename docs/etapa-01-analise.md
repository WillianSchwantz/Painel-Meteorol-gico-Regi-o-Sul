# Painel Meteorológico — Região Sul

## Etapa 1 — Análise do `wu.sql`

Fonte analisada: `wu.sql`, banco `weather_pws`.

O dump contém quatro tabelas reais:

1. `stations`
2. `history_daily`
3. `history_hourly`
4. `fetch_log`

Nenhuma tabela ou coluna será inferida das capturas de tela.

## Estrutura encontrada

### `stations`

Cadastro das estações meteorológicas.

| Campo | Tipo | Restrições |
|---|---|---|
| `station_id` | `varchar(30)` | chave primária, não nulo |
| `station_name` | `varchar(120)` | não nulo |
| `latitude` | `decimal(9,6)` | não nulo |
| `longitude` | `decimal(9,6)` | não nulo |
| `qc_status_label` | `varchar(10)` | aceita nulo |
| `partner_id` | `varchar(20)` | aceita nulo |
| `last_update_utc` | `datetime` | aceita nulo |
| `created_at` | `datetime` | não nulo, valor padrão `CURRENT_TIMESTAMP` |

Campos principais para a aplicação: `station_id`, `station_name`, `latitude`,
`longitude`, `qc_status_label` e `last_update_utc`.

### `history_daily`

Histórico consolidado por estação e dia.

| Grupo | Campos |
|---|---|
| Identificação | `id`, `station_id`, `obs_date` |
| Horários | `obs_time_utc`, `obs_time_local`, `epoch`, `tz` |
| Localização e qualidade | `lat`, `lon`, `qc_status` |
| Radiação e UV | `solar_radiation_high`, `uv_high` |
| Direção do vento | `winddir_avg` |
| Umidade | `humidity_high`, `humidity_low`, `humidity_avg` |
| Temperatura | `temp_high`, `temp_low`, `temp_avg` |
| Ponto de orvalho | `dewpt_high`, `dewpt_low`, `dewpt_avg` |
| Índice de calor | `heatindex_high`, `heatindex_low`, `heatindex_avg` |
| Sensação pelo vento | `windchill_high`, `windchill_low`, `windchill_avg` |
| Velocidade do vento | `windspeed_high`, `windspeed_low`, `windspeed_avg` |
| Rajada | `windgust_high`, `windgust_low`, `windgust_avg` |
| Pressão | `pressure_max`, `pressure_min`, `pressure_trend` |
| Precipitação | `precip_rate`, `precip_total` |
| Auditoria | `fetched_at` |

Chave primária: `id`.

Restrições e índices:

- unicidade de `station_id` + `obs_date`;
- índice por `obs_date`;
- índice por `station_id` + `obs_date`;
- chave estrangeira de `station_id` para `stations.station_id`.

Campos principais para a primeira tela funcional: `station_id`, `obs_date`,
`temp_low`, `temp_high`, `temp_avg`, `humidity_avg`, `precip_total`,
`pressure_min`, `pressure_max` e `windspeed_avg`.

### `history_hourly`

Histórico por horário. Possui os mesmos grupos meteorológicos de
`history_daily`, mas não possui `obs_date`. O instante obrigatório é
`obs_time_utc`; `obs_time_local` é opcional.

Chave primária: `id`.

Restrições e índices:

- unicidade de `station_id` + `obs_time_utc`;
- índice por `station_id` + `obs_time_utc`;
- chave estrangeira de `station_id` para `stations.station_id`.

Campos principais previstos: `station_id`, `obs_time_utc`, `obs_time_local`,
`temp_low`, `temp_high`, `temp_avg`, `humidity_avg`, `precip_total` e
`precip_rate`.

### `fetch_log`

Registro técnico das importações.

| Campo | Tipo | Restrições |
|---|---|---|
| `id` | `bigint unsigned` | chave primária, auto incremento |
| `station_id` | `varchar(30)` | não nulo |
| `fetch_date` | `date` | não nulo |
| `endpoint` | `varchar(30)` | não nulo |
| `status` | `varchar(10)` | não nulo |
| `rows_saved` | `smallint unsigned` | padrão zero |
| `error_msg` | `text` | aceita nulo |
| `fetched_at` | `datetime` | não nulo, padrão `CURRENT_TIMESTAMP` |

Esta tabela não é necessária para os filtros e cálculos meteorológicos da
primeira versão. Ela não possui chave estrangeira para `stations`.

## Perfil dos dados do dump

- 301 registros em `stations`.
- 182.128 registros em `history_daily`.
- Período diário encontrado: 01/06/2023 a 31/05/2026.
- 8.901 registros em `history_hourly`.
- O histórico horário contém dados somente da estação `ISANTA1877`.
- Período horário UTC encontrado: 20/02/2025 19:30:36 a
  01/05/2026 02:55:02.
- Coordenadas das estações: latitude entre `-34.200690` e `-27.163310`;
  longitude entre `-58.087200` e `-50.008000`.

## Inconsistências e decisões

1. Não existem campos de cidade ou estado em `stations`.
   Portanto, um filtro real por cidade não pode ser implementado apenas com o
   dump original. A versão final usa a tabela complementar `station_location`,
   relacionada por `station_id`, para atender ao requisito visual sem alterar
   os campos originais de `stations`.

2. `history_daily` contém dados do identificador `ISANTA1074`, que não existe
   em `stations`. O dump desativa `FOREIGN_KEY_CHECKS` durante a importação, por
   isso essa linha órfã pôde ser incluída. As consultas da aplicação partirão
   de `stations` e usarão junções coerentes, evitando apresentar uma estação
   sem cadastro ou coordenadas.

3. Cinco estações cadastradas não possuem histórico diário:
   `IESTEI2`, `IIBIRA6`, `IPEDRA26`, `ISERTO11` e `ISOLED27`.
   Elas podem aparecer no mapa, mas uma busca de medições deve retornar uma
   lista vazia e uma mensagem informativa.

4. `history_hourly` tem cobertura muito menor que `history_daily`.
   Os acumulados gerais de 24 h, 48 h e 72 h serão calculados a partir de
   `history_daily.precip_total`, somando respectivamente o dia final e os
   últimos dois ou três dias consecutivos disponíveis. Quando a sequência não
   estiver completa, o resumo informará que não há dados suficientes.

5. O schema não registra unidades. A aplicação não fará conversões silenciosas.
   Os valores serão apresentados com as unidades meteorológicas solicitadas
   pelo trabalho (°C, %, mm, hPa e km/h), com essa premissa documentada no
   README da etapa de implementação.

6. Latitude e longitude aparecem tanto em `stations` quanto nos históricos.
   O mapa usará `stations.latitude` e `stations.longitude`, que representam o
   cadastro oficial da estação.

7. A tabela diária é criada antes de `stations` no dump, embora declare uma
   chave estrangeira para ela. Isso funciona durante a restauração porque o
   arquivo desativa temporariamente as verificações de chave estrangeira.

8. O ambiente possui Java 17, mas não possui o comando global `mvn`. A etapa de
   estruturação deve incluir Maven Wrapper para tornar a compilação
   reproduzível sem exigir uma instalação global do Maven.

## Estrutura de pacotes proposta

```text
src/
└── main/
    ├── java/
    │   ├── app/
    │   │   └── Main.java
    │   ├── model/
    │   │   ├── Estacao.java
    │   │   ├── MedicaoDiaria.java
    │   │   ├── MedicaoHoraria.java
    │   │   ├── ResumoMeteorologico.java
    │   │   └── NivelAlerta.java
    │   ├── dao/
    │   │   ├── ConexaoMySQL.java
    │   │   ├── EstacaoDAO.java
    │   │   └── MedicaoDAO.java
    │   ├── service/
    │   │   ├── EstacaoService.java
    │   │   ├── AlagamentoService.java
    │   │   └── ChuvaTendenciaService.java
    │   ├── view/
    │   │   ├── TelaPrincipal.java
    │   │   ├── PainelFiltros.java
    │   │   └── PainelTabela.java
    │   ├── map/
    │   │   ├── MapaPanel.java
    │   │   ├── EstacaoWaypoint.java
    │   │   └── EstacaoPainter.java
    │   └── util/
    │       ├── DateUtils.java
    │       └── NumberUtils.java
    └── resources/
        └── application.properties.example
```

As validações manuais serão documentadas no README da aplicação final, mantendo
o código de produção separado de executáveis temporários.

## Plano da próxima etapa

A Etapa 2 deverá:

1. criar o projeto Maven e o Maven Wrapper;
2. criar os pacotes obrigatórios;
3. adicionar uma classe `Main` mínima e compilável;
4. declarar as dependências JDBC e JXMapViewer2;
5. criar configuração de conexão por variáveis de ambiente, sem senha fixa no
   código;
6. executar a compilação e validar a inicialização da aplicação.

Nenhuma classe Java foi criada nesta Etapa 1, pois o roteiro solicitado exige
que a análise do schema seja apresentada antes da geração do código.
