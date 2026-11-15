# Contrato de interface — implementação visual futura pelo Claude

A interface atual é provisória. Substituir telas e tema preservando os contratos e limites abaixo, sem mover transporte/parser para Composables.

## Modelos em `:core`

`Video(id, title, channel, channelId?, durationSeconds?, thumbnail, description, isShort)`; `Channel(id,title,description)`; `Page<T>(items,next?)`; `Cursor(token,endpoint,context)`; `ChannelPage(channel,videos)`. `Cursor` é opaco e transitório: não mostrar na interface, registrar em analytics ou salvar.

`Library(history,favorites,recentSearches,minimumMinutes,hideWatched)` é estado persistido. `visible(videos)` aplica filtro local e exclui Shorts. Não aplicar duração aos favoritos/histórico, pois devem continuar acessíveis mesmo se os ajustes mudarem.

## Estado e comandos

`AppViewModel` mantém `homeState`, `searchState`, `channelState`, `detail`, `detailError`, `library` como StateFlow. Coletar com `collectAsStateWithLifecycle`. `CatalogState` contém `heading`, `items`, `loading`, `error`, `cursor` e `channel`.

Comandos: `home()`, `search(query)`, `channel(id)`, `more("home"|"search"|"channel")`, `retry(key)`, `select(video)`, `selected(id)`, `favorite(video)`, `watched(video)`, `preferences(minutes,hideWatched)` e `clearHistory()`.

`Catalog` é a porta de dados: funções suspensas de pesquisa, vídeo, canal e próxima página. UI não deve chamar endpoints diretamente. `CatalogException.Kind`: NETWORK, BLOCKED, RATE_LIMIT, SCHEMA, UNAVAILABLE. Manter mensagem, retry explícito e abertura externa quando apropriado.

## Rotas existentes

| Rota | Conteúdo |
| --- | --- |
| `home` | Seleção por tema, filtro local, páginas |
| `search` | Entrada de termos, pesquisas recentes e URL/ID |
| `results` | Página de resultados e carregar mais |
| `video/{id}` | Metadados, favoritar, canal e reproduzir |
| `player/{id}` | `YouTubePlayer(video,onPlaying)` |
| `channel/{id}` | Cabeçalho e vídeos paginados |
| `library` | Favoritos e histórico locais |
| `settings` | Duração, ocultar assistidos, limpeza e estado visitante |
| `media-demo` | Fonte de teste autorizada do Media3 |

## Reprodução e persistência

Preservar o player oficial e seus controles/anúncios. Não cobrir o iframe com botões personalizados que interceptem seus controles. `onPlaying` é acionado pelo estado 1 oficial; apenas esse evento deve registrar o início no histórico. Manter mensagens de bloqueio, retry, fallback externo e liberação/pausa por lifecycle.

`AuthorizedMediaPlayer(AuthorizedMediaSource)` recebe somente fontes com referência de autorização; não usar como resolvedor de YouTube. Não substituir por uma tela que pareça tocar sem mídia real.

`LocalStore.update` faz atualização atômica no DataStore. Sem operações de arquivo/rede na composição. Coil já oferece cache de thumbnails. Listas devem continuar lazy e usar ID estável.

## Casos obrigatórios no design definitivo

Carregamento inicial; carregamento de próxima página preservando itens; vazio; nenhum resultado após filtros; offline; 429; consentimento/autorização; metadados indisponíveis com player ainda acessível; incorporação proibida; erro 153; login não configurado; biblioteca vazia. Deixar explícito que seleção por tema/favoritos/histórico são locais, sem alegar recomendação oficial personalizada.

Não implementar login fictício, estado de reprodução simulado ou dados de demonstração apresentados como catálogo ao vivo. Testes existentes usam fixtures sintéticas; ver relatório de validação antes de alegar compatibilidade real.
