# Diagnóstico e correção de consultas JPA

Dois mil livros e sessenta mil resenhas num PostgreSQL de verdade, e a mesma
pergunta feita de várias formas — com a contagem de statements, de entidades
materializadas e de colunas selecionadas de cada uma medida lado a lado. O
assunto não é fazer a consulta funcionar. Todas funcionam. É o que cada decisão
de mapeamento e de consulta custa em ida e volta ao banco, e quais das correções
consagradas não corrigem nada.

O instrumento são as estatísticas do próprio Hibernate
(`hibernate.generate_statistics`) mais um `StatementInspector` que registra cada
statement antes de ele chegar ao driver. Nenhum número aqui é estimado.

Seis resultados que mudam a conversa:

**O pior plano do projeto é o que tem menos statements.** Um `left join fetch`
numa consulta paginada: Hibernate não consegue empurrar o `limit` para uma
consulta cujas linhas são uma coleção, então não empurra — carrega tudo e
pagina em memória, com um aviso `HHH90003004` no log. Para devolver 20 livros
ele materializa **62 000 entidades em 202 ms**. A versão de duas consultas —
a página de ids, depois esses ids com a coleção — materializa **620 entidades em
4 ms**. Mesmos dois statements, 50x o tempo. É por isso que contagem de
statements não basta: aqui ela aponta para o lado errado.

**`Session.setFetchBatchSize` não é o `@BatchSize` em tempo de execução.** Tem o
nome, a assinatura e a documentação de quem faz batch fetching, e não faz: o
loader da coleção é construído na inicialização a partir da anotação, e um
tamanho definido na sessão chega tarde. Medido, **2001 statements — exatamente
os mesmos de não fazer nada**. Com a anotação, 41.

**O lado inverso de um `@OneToOne` é um N+1 que ninguém pediu.** Pedir só o
título de cada livro, sem nunca tocar na capa, custa **2001 statements e 171 ms**.
A palavra `LAZY` está no mapeamento. Ela não vale: do lado inverso a chave
estrangeira está na outra tabela, e o Hibernate precisa escolher entre instalar
um proxy e escrever `null` — a única forma de escolher é ir olhar. Com a capa
buscada de propósito num `left join`, **1 statement e 8 ms**.

**`@ManyToOne` é `EAGER` por padrão, e isso é o único N+1 que chega sem
ninguém escrever um laço.** Uma página de 200 resenhas cujo livro nunca é lido
custa **190 statements**: uma por livro distinto na página, resolvida linha por
linha depois do result set. A mesma tabela mapeada com `fetch = LAZY`:
**1 statement**. Nada no código mudou — o tipo de busca já tinha decidido.

**Duas das cinco projeções não são projeções.** Uma projeção por interface
*aberta* (basta um `@Value` em SpEL) precisa do objeto raiz para o SpEL avaliar,
então o Spring Data seleciona a entidade inteira; uma projeção *fechada* sobre
uma `@Query` que seleciona a entidade também, porque o Spring Data só estreita o
`select` que ele mesmo escreveu. As duas: **7 colunas e 2000 entidades
materializadas**. A expressão de construtor: **3 colunas, 0 entidades, 3 ms**
contra 21 ms.

**O aviso clássico sobre o cache de consulta está errado no Hibernate 6.** A
história é que um cache de consulta sobre uma entidade que não está no cache de
segundo nível degenera em um statement por linha. Medido: **zero statements**, o
resultado volta inteiro. O que vale é a invalidação — o cache é indexado pela
consulta e invalidado pela *tabela*, então **uma escrita em qualquer linha de
`book` joga fora toda consulta cacheada sobre `book`**, e volta a custar
statement.

## Tecnologias e bibliotecas

| Tecnologia | Para que serve aqui |
| --- | --- |
| Java 21 | aplicação |
| Spring Boot 3.5 | aplicação e API REST |
| Spring Data JPA | projeções por interface, métodos derivados, `@EntityGraph`, paginação |
| Hibernate 6.6 | `Statistics` e `StatementInspector` como instrumento; window function e tabela derivada em HQL |
| PostgreSQL 16 | banco real, com os índices que um schema de verdade teria |
| Flyway | schema versionado, com `ddl-auto: validate` |
| Hibernate JCache + Caffeine | cache de segundo nível e cache de consulta |
| Testcontainers | o PostgreSQL real nos testes de integração |
| JUnit 5 + AssertJ | 73 testes |

## Pré-requisitos

- Java 21
- Docker e Docker Compose

## Como rodar

```bash
docker compose up -d
```

```bash
./gradlew bootRun
```

Para derrubar tudo:

```bash
docker compose down -v
```

## Endpoints

Semear o catálogo (2000 livros, 60 000 resenhas, 6000 vínculos de autoria):

```bash
curl -s -X POST http://localhost:8080/api/dataset
```

Semear outro tamanho, para medir os mesmos cenários em outra escala:

```bash
curl -s -X POST http://localhost:8080/api/dataset/custom \
  -H 'Content-Type: application/json' \
  -d '{"publishers":4,"authors":100,"books":500,"reviewsPerBook":10,
       "authorsPerBook":2,"coverRatio":0.5}'
```

Listar os cenários e a pergunta de cada um:

```bash
curl -s http://localhost:8080/api/scenarios
```

Rodar um cenário. `warmups` descarta execuções (a primeira paga JIT, plano do
Hibernate e plano do PostgreSQL) e `repeats` é o número de execuções
cronometradas, das quais se reporta a mediana:

```bash
curl -s -X POST 'http://localhost:8080/api/scenarios/n-plus-one?warmups=3&repeats=9'
```

Rodar todos, e ler os relatórios depois:

```bash
curl -s -X POST 'http://localhost:8080/api/scenarios?warmups=3&repeats=9'
```

```bash
curl -s http://localhost:8080/api/measurements
```

O catálogo servido do jeito que os cenários concluíram que deve ser servido —
a listagem em duas consultas, as colunas por expressão de construtor, e o livro
com as duas coleções em uma consulta cada:

```bash
curl -s 'http://localhost:8080/api/books?page=0&size=3'
curl -s http://localhost:8080/api/books/summaries
curl -s http://localhost:8080/api/books/7
```

## O que cada variante produz

Cada relatório traz, por variante: statements preparados, entidades
materializadas, coleções inicializadas, acertos e erros dos dois caches, quantos
objetos foram devolvidos — nenhuma variante pode parecer barata respondendo uma
pergunta menor — a mediana do tempo, e o SQL que ela provocou, colapsado por
texto com a contagem de repetições. Um N+1 de dois mil statements vira duas
linhas, que é a única forma em que se lê.

As variantes são classificadas em três tipos: `PROBLEM` é o plano que sai do
código óbvio, `FIX` é uma correção que responde a mesma pergunta por menos, e
`TRAP` é o que parece correção e não é — ou é pior que não fazer nada.

## As medições

Catálogo de 2000 livros, 8 editoras, 400 autores, 60 000 resenhas (30 por
livro), 6000 vínculos de autoria, 1000 capas. Jar empacotado rodando contra o
PostgreSQL 16 do `docker-compose.yml`, daemon do Gradle parado, 3 execuções de
aquecimento descartadas e a mediana de 9 cronometradas. Tudo numa única sessão.

### `n-plus-one` — quantas resenhas cada livro tem

| variante | | statements | ms |
| --- | --- | --- | --- |
| `lazy-loop` | problema | 2001 | 323 |
| `entity-graph` | correção | 1 | 185 |
| `join-fetch` | correção | 1 | 177 |
| `batch-size` | correção | 41 | 184 |
| `subselect` | correção | 2 | 143 |
| `session-fetch-batch-size` | armadilha | 2001 | 377 |

Quatro correções, três formas. `@EntityGraph` e `join fetch` geram **o mesmo
SQL** — o teste compara as duas strings e afirma a igualdade. `@BatchSize(50)`
deixa a consulta dos pais intacta e gasta um statement por lote
(1 + ⌈2000/50⌉ = 41). O subselect custa exatamente dois, reexecutando a consulta
original como subconsulta:

```sql
select ... from review r1_0 where r1_0.book_id in (select b1_0.id from book b1_0)
```

### `paginated-fetch-join` — a primeira página de 20 livros com suas resenhas

| variante | | statements | entidades | ms |
| --- | --- | --- | --- | --- |
| `fetch-join-page` | armadilha | 2 | 62 000 | 202 |
| `ids-then-fetch` | correção | 2 | 620 | 4 |
| `batch-size-page` | correção | 3 | 620 | 4 |
| `subselect-page` | armadilha | 1983 | 62 000 | 329 |

O subselect numa consulta paginada é a armadilha dentro da armadilha: a
subconsulta que ele reexecuta é a consulta original **sem o `limit`**. Ele busca
as coleções da tabela inteira e depois precisa carregar os 1980 pais que a
página não selecionou, um por um.

### `inverse-one-to-one` — só o título de cada livro

| variante | | statements | ms |
| --- | --- | --- | --- |
| `inverse-lazy` | problema | 2001 | 171 |
| `entity-graph` | correção | 1 | 8 |
| `no-inverse-side` | correção | 1 | 11 |

A terceira variante é o mapeamento que não tem o problema: só o filho aponta
para o pai. Quando o pai não precisa navegar para o filho, é a correção mais
barata que existe — nenhuma.

### `eager-to-one` — uma página de 200 resenhas, sem ler o livro

| variante | | statements | entidades | ms |
| --- | --- | --- | --- | --- |
| `jpa-default-eager` | problema | 190 | 389 | 43 |
| `declared-lazy` | correção | 1 | 200 | 16 |
| `join-fetch` | correção | 1 | 389 | 27 |

A página é ordenada por resenhista, não pela chave estrangeira. Ordenada por
`book_id` a primeira linha de cada livro aquece a sessão para as outras 29, uma
página de 200 resenhas toca sete livros e o fetch eager parece quase gratuito —
é assim que isso passa pelos testes.

### `cartesian-product` — um livro com seus autores e suas resenhas

| variante | | statements | linhas | ms |
| --- | --- | --- | --- | --- |
| `two-bags` | problema | — | não roda | — |
| `one-join-rows` | armadilha | 1 | 90 | 1 |
| `two-queries` | correção | 2 | 33 | 4 |

`MultipleBagFetchException` não é o Hibernate sendo chato com a diferença entre
`List` e `Set`. É o Hibernate recusando desduplicar duas coleções de um result
set em que cada linha é um par. Trocar as duas por `Set` remove a recusa e deixa
o produto de pé: 3 autores × 30 resenhas = **90 linhas** para um livro, contra
3 + 30 = 33 em uma consulta por coleção.

### `projections` — título, ano e nome da editora de cada livro

| variante | | colunas | entidades | ms |
| --- | --- | --- | --- | --- |
| `entity-then-lazy` | problema | 7 | 2008 | 14 |
| `closed-over-entity-query` | armadilha | 7 | 2000 | 11 |
| `open-projection` | armadilha | 7 | 2000 | 21 |
| `closed-derived` | correção | 5 | 8 | 9 |
| `dto-constructor` | correção | 3 | 0 | 3 |

Aqui o número que importa é a largura do `select`, não a contagem de statements:
quatro das cinco variantes mandam um statement só, e não pedem a mesma coisa.

A projeção fechada num método derivado estreita de verdade — mas a projeção
aninhada da editora alarga para a associação inteira, porque o que ela busca é a
entidade associada:

```sql
select b1_0.title,b1_0.published_year,p1_0.id,p1_0.country,p1_0.name from book b1_0 ...
```

Só a expressão de construtor pede exatamente o que foi pedido:

```sql
select b1_0.title,b1_0.published_year,p1_0.name from book b1_0 ...
```

O statement do `entity-then-lazy` é baixo (9, não 2001) por um motivo que vale
registrar: **um N+1 sobre um to-one é limitado pelo número de valores
distintos**, não pelo de linhas. São 8 editoras para 2000 livros.

### `second-level-cache` — a segunda leitura, numa sessão nova

| variante | | statements | acertos L2 | acertos/erros do cache de consulta |
| --- | --- | --- | --- | --- |
| `no-cache` | problema | 1 | 0 | — |
| `entity-cache-by-id` | correção | 0 | 8 | — |
| `entity-cache-by-query` | armadilha | 1 | 0 | — |
| `query-cache` | correção | 0 | 0 | 1 / 0 |
| `query-cache-over-cached-entity` | correção | 0 | 0 | 1 / 0 |
| `query-cache-after-write` | armadilha | 2 | 0 | 0 / 1 |

O cache de entidade responde `find` por id e mais nada: a mesma entidade
cacheada, lida por consulta, custa o statement inteiro. O cache de consulta é o
que responde uma consulta — e funciona mesmo quando a entidade não está
cacheada. O que não funciona é conviver com escrita: uma única escrita em `book`
transforma o acerto em erro.

### `top-n-per-group` — as 3 resenhas mais bem avaliadas de cada livro

| variante | | statements | entidades | ms |
| --- | --- | --- | --- | --- |
| `in-java` | problema | 2001 | 62 000 | 368 |
| `jpql-per-book` | problema | 2001 | 0 | 200 |
| `hql-window` | correção | 1 | 0 | 24 |
| `native-window` | correção | 1 | 0 | 25 |

Fazer em Java perde duas vezes: é o N+1 e ainda materializa toda resenha do
catálogo para jogar quase todas fora. Em JPQL livro por livro é o mesmo N+1 sem
nenhuma entidade desperdiçada — melhor, e ainda linear no número de livros.

Um statement resolve tudo, e **não precisa ser SQL nativo**. O HQL 6 renderiza
`row_number() over (partition by ...)` e aceita tabela derivada na cláusula
`from`:

```sql
select t1_0.bookId,t1_0.reviewId,t1_0.rating,t1_0.rn
from (select r1_0.book_id,r1_0.id,r1_0.rating,
             row_number() over(partition by r1_0.book_id
                               order by r1_0.rating desc,r1_0.id)
      from review r1_0) t1_0(bookId,reviewId,rating,rn)
where t1_0.rn<=? order by t1_0.bookId,t1_0.rn
```

### `native-sql` — a resenha mais nova de cada livro

| variante | | statements | ms |
| --- | --- | --- | --- |
| `hql-window` | correção | 1 | 20 |
| `native-distinct-on` | correção | 1 | 20 |

A pergunta deste cenário é se descer para SQL nativo compensa, feita no caso
mais favorável que existe: `DISTINCT ON` é um operador que o PostgreSQL tem para
exatamente esta forma, não está em norma nenhuma e não tem grafia em HQL. Mesmo
assim, **20 ms contra 20 ms**. O PostgreSQL ordena uma vez de qualquer jeito, e
a window function sobre as linhas já ordenadas não cobra nada que valha nome.

Não é que SQL nativo nunca compense. É que "isso é complicado, tem que ser
nativo" não é motivo — e todos os cenários que produziram diferença de ordem de
grandeza neste projeto produziram mudando o **número de idas e voltas**, não a
linguagem em que a consulta foi escrita.

## `open-in-view`

O padrão do Spring Boot é `true`, e a aplicação roda com `false`. A diferença
está em dois testes que fazem a mesma requisição a um endpoint que entrega um
proxy ao Jackson:

- com `open-in-view: false`, a sessão já fechou quando a resposta é escrita e a
  requisição falha alto — `HttpMessageNotWritableException: Could not initialize
  proxy ... no session`;
- com `true`, a mesma requisição responde 200 e a consulta acontece na camada de
  view, durante a serialização.

Ligado, a alternativa não é "nenhuma consulta". É uma consulta que nenhum teste
de serviço e nenhum teste de repositório consegue ver.

## Como as medições se sustentam

Três coisas precisam ser verdade para um número aqui significar algo, e as três
são responsabilidade do medidor, não do cenário.

**Contexto de persistência novo.** Cada execução abre a sua própria transação, e
portanto o seu próprio cache de primeiro nível. Duas variantes na mesma
transação e a segunda é de graça: as entidades já estão gerenciadas.

**Cache de segundo nível frio**, exceto no cenário que é sobre cache. Deixá-lo
quente faria a variante que por acaso rodasse depois parecer a correção.

**Contadores lidos depois do commit**, não dentro da transação. Um flush no
commit manda statements, e uma unidade de trabalho que deixa trabalho para o
commit não é mais barata que a que o faz na hora.

## Testes

73 testes de integração contra o PostgreSQL real via Testcontainers, com
container **singleton** em inicializador estático — o Spring reaproveita
contextos entre classes de teste, e um container por classe seria derrubado
enquanto um contexto em cache ainda apontasse para ele.

Cada cenário tem um teste que afirma **contagem exata** de statements, não
limites. É o que impede uma mudança de mapeamento de transformar silenciosamente
uma afirmação deste README em mentira. O catálogo de teste é pequeno e fixo (60
livros, 4 resenhas, 2 autores) porque as contagens são função do número de
linhas — e 60 e não 20 porque o cenário de paginação pede uma página de 20: com
um catálogo de 20 a página seria o catálogo inteiro, e a variante que carrega
tudo ficaria indistinguível da que não carrega.

```bash
DOCKER_HOST=unix:///var/run/docker.sock ./gradlew clean build
```
