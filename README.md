# Painel Meteorológico — Região Sul

Sistema desktop acadêmico em Java Swing para consultar e visualizar dados
históricos de estações meteorológicas da Região Sul. A aplicação reúne filtros,
tabela, resumo meteorológico, alerta de alagamento, ondas de calor/frio,
mapa OpenStreetMap, cluster de estações, popup com sparkline, heatmap IDW e
isolinhas.

## Tecnologias

- Java 17 e Swing
- Maven
- MySQL e JDBC
- MySQL Connector/J 8.4.0
- JXMapViewer2 2.8
- OpenStreetMap

## Pré-requisitos

- JDK 17 ou superior;
- MySQL 8 instalado e em execução;
- dump `wu.sql`;
- acesso à internet na primeira execução do Maven e para os tiles do mapa.

O Maven Wrapper está incluído. Não é necessário instalar o Maven globalmente.

## 1. Importar o banco

O dump já cria e seleciona o banco `weather_pws`.

No MySQL Workbench:

1. Abra **File > Open SQL Script** e selecione `wu.sql`.
2. Conecte-se com um usuário autorizado a criar bancos.
3. Execute todo o script.
4. Atualize a lista de schemas e confirme a existência de `weather_pws`.

Pelo cliente de terminal do MySQL:

```text
mysql -u root -p
source C:/caminho/para/wu.sql
```

As tabelas principais usadas pela aplicação são `stations`, `history_daily` e
`history_hourly`. Nenhuma tabela principal é criada pelo código Java.

Para atender ao filtro visual de cidade sem alterar o dump original, o projeto
inclui a tabela complementar opcional `station_location`, associada por
`station_id`. Depois de importar o `wu.sql`, execute:

```text
source C:/caminho/para/docs/station_location.sql
```

O filtro de cidade usa `station_location.cidade`, `uf` e `regiao` quando essa
tabela existe. Se ela ainda não existir, a aplicação continua funcionando e
filtra apenas por nome/identificador da estação.

## 2. Configurar a conexão

Os valores padrão em `dao.ConexaoMySQL` são:

| Configuração | Valor padrão |
|---|---|
| Host | `localhost` |
| Porta | `3306` |
| Banco | `weather_pws` |
| Usuário | `root` |
| Senha | vazia |

Recomenda-se configurar as credenciais por variáveis de ambiente, sem gravar a
senha no código:

```powershell
$env:WEATHER_DB_HOST = "localhost"
$env:WEATHER_DB_PORT = "3306"
$env:WEATHER_DB_NAME = "weather_pws"
$env:WEATHER_DB_USER = "root"
$env:WEATHER_DB_PASSWORD = "sua_senha"
```

Também são aceitas as propriedades JVM `weather.db.host`, `weather.db.port`,
`weather.db.name`, `weather.db.user` e `weather.db.password`. Elas têm
precedência sobre as variáveis de ambiente.

## 3. Compilar e executar

No Windows, abra o PowerShell na pasta do projeto:

```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd exec:java
```

No Linux ou macOS:

```bash
./mvnw clean compile
./mvnw exec:java
```

Também é possível importar o `pom.xml` em uma IDE e executar `app.Main`.

## Uso

1. Aguarde as estações carregarem no `JComboBox`.
2. Opcionalmente, selecione ou digite **Cidade/UF** e clique em **Filtrar** para
   reduzir a lista de estações e os marcadores do mapa.
3. Selecione a estação, o período no formato `yyyy-MM-dd` e a variável.
4. Clique em **Buscar**.
5. Consulte a tabela, o resumo e o nível de alerta.
6. Clique em uma linha da tabela para manter estação, resumo e mapa
   sincronizados.
7. Marque **Exibir heatmap** e clique em **Buscar** para interpolar a variável
   selecionada no período.
8. Use o slider de tempo para atualizar o mapa conforme o dia selecionado.
9. Clique em um marcador para abrir o popup com sparkline dos últimos 7 dias.

A tabela e o resumo representam somente a estação selecionada. O heatmap usa
todas as estações que possuem coordenadas e valor válido no período; um mapa de
calor feito com uma única estação não teria significado espacial.

## Filtro de cidade

O dump `wu.sql` não possui campos de cidade ou UF na tabela `stations`. Para
cumprir o requisito visual sem inventar colunas no dump original, o projeto usa
a tabela relacional complementar `station_location`, criada pelo script
`docs/station_location.sql`.

Quando essa tabela está presente, o combo **Cidade/UF** lista as cidades
cadastradas e filtra por `cidade`, `uf`, `regiao`, nome da estação ou
`station_id`. Quando ela não está presente, o sistema mantém um campo editável
com fallback por nome da estação ou identificador, permitindo que o restante da
aplicação continue funcionando.

## Funcionalidades

- listagem de estações reais;
- filtros por cidade/UF, estação, período e variável meteorológica;
- tabela de medições diárias;
- resumo de temperatura, umidade, vento, pressão e chuva;
- chuva acumulada em 24, 48 e 72 horas;
- classificação `NORMAL`, `ATENCAO`, `ALERTA` e `EMERGENCIA`;
- detecção acadêmica de onda de calor e onda de frio por 3 dias consecutivos;
- mapa OpenStreetMap com marcadores por nível de alerta;
- centralização e destaque da estação selecionada;
- cluster de estações em zoom menor que 8;
- heatmap de Temperatura, Chuva, Umidade, Vento ou Pressão;
- ativação e desativação do heatmap;
- isolinhas calculadas por Marching Squares;
- slider de tempo para atualizar mapa e alertas por dia;
- popup no mapa com sparkline de 7 dias;
- zonas alagadiças e alerta de tendência de chuva.

## Heatmap IDW

O `HeatmapService` obtém um resumo agregado de `history_daily` por estação. A
variável selecionada é associada às coordenadas reais de `stations`. O
`HeatmapPainter` cria uma grade leve de 30 × 30 pixels e aplica:

```text
valor = Σ(valor_i / distancia_i²) / Σ(1 / distancia_i²)
```

Pontos muito próximos usam diretamente o valor da estação, evitando divisão
por zero. Valores nulos são ignorados e, com menos de dois pontos, o heatmap não
é desenhado. A escala visual vai de azul a amarelo e vermelho, com
transparência. A imagem interpolada é armazenada em cache enquanto viewport,
zoom e tamanho não mudarem.

As variáveis usam somente colunas existentes:

| Opção | Origem |
|---|---|
| Temperatura | média de `history_daily.temp_avg` |
| Chuva | soma de `history_daily.precip_total` |
| Umidade | média de `history_daily.humidity_avg` |
| Vento | média de `history_daily.windspeed_avg` |
| Pressão | média derivada de `pressure_min` e `pressure_max` |

## Alertas de alagamento

O nível é calculado em `AlagamentoService`, sempre na ordem:
`EMERGENCIA → ALERTA → ATENCAO → NORMAL`.

O sistema tenta usar `history_hourly.precip_total`. No dump analisado, os 8.901
registros horários têm essa coluna nula. Por isso, a aplicação usa
`history_daily.precip_total` como aproximação por dias-calendário: um dia para
24 horas, dois para 48 horas e três para 72 horas. `precip_rate` não é somada,
pois é taxa e não volume acumulado.

## Ondas de calor e frio

O cálculo fica em `OndaTermicaService` e usa somente as medições diárias já
buscadas do banco:

- onda de calor: `history_daily.temp_high >= média histórica de verão + 5 °C`
  por 3 dias consecutivos ou mais;
- onda de frio: `history_daily.temp_low <= média histórica de inverno - 5 °C`
  por 3 dias consecutivos ou mais.

As médias históricas de apoio usadas no trabalho são:

| Região de apoio | Verão | Inverno |
|---|---:|---:|
| Litoral/Leste e Depressão Central | 31 °C | 10 °C |
| Planalto/Serra | 31 °C | 7 °C |
| São José dos Ausentes | 31 °C | 6 °C |
| Fronteira Oeste/Campanha | 34 °C | 8 °C |

Como `stations` não possui cidade, UF nem altitude, a região é classificada de
forma aproximada por coordenadas e por nomes reais da estação quando eles estão
disponíveis. Essa classificação é suficiente para apresentação acadêmica, mas
não substitui uma base climatológica oficial.

## Estrutura

```text
src/main/java/
├── app/      inicialização da aplicação
├── model/    objetos de dados e enum de alerta
├── dao/      conexão e consultas JDBC com PreparedStatement
├── service/  resumo, alerta, ondas térmicas e preparação do heatmap
├── view/     janela, filtros e tabela Swing
├── map/      mapa, waypoints, marcadores e IDW
└── util/     espaço para utilitários sem responsabilidade de outra camada
```

## Limitações conhecidas

- os acumulados 24/48/72 h são aproximações diárias com o dump atual;
- não existe `pressure_avg`; a pressão média é derivada quando mínimo e máximo
  estão disponíveis;
- `stations` não possui cidade ou UF; o filtro de cidade depende da tabela
  complementar `station_location` estar criada e preenchida;
- a tabela consulta uma estação por vez; seu objeto `MedicaoDiaria` conserva o
  `station_id`, embora essa coluna não precise ficar visível;
- por custo de consulta, a busca atualiza o alerta exato do marcador
  selecionado; os demais ficam cinza como **Não calculado** até serem consultados;
- a onda de calor/frio usa médias históricas simplificadas e classificação
  regional aproximada por latitude, longitude e nome da estação; não é uma
  climatologia oficial;
- os tiles do OpenStreetMap dependem de internet e da disponibilidade do
  servidor público;
- o heatmap é uma interpolação visual histórica por IDW, não uma previsão
  meteorológica nem uma medição entre estações;
- **Filtro de cidade**: usa a tabela relacional de apoio `station_location`;
  sem essa tabela, o campo funciona como busca por nome/ID da estação;
- **Zonas alagadiças**: são uma aproximação visual baseada no nível de alerta calculado, não um mapa hidrológico oficial;
- **Tendência de chuva**: as setas indicam tendência estatística de chuva baseada em regressão linear dos últimos 7, 15 ou 30 dias. Não é previsão meteorológica;
- não há CRUD administrativo para preencher `station_location` pela interface;
- as isolinhas e o heatmap são visualizações interpoladas, não medições entre
  estações.

O dump analisado contém 302 estações com latitude e longitude válidas; nenhuma
foi descartada por falta de coordenadas. A quantidade efetiva de pontos do
heatmap varia com o período e a variável escolhidos.


