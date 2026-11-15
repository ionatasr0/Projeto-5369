# Recuperação de login e reprodução — Projeto 5369

## Base inspecionada

Repositório confirmado: `ionatasr0/Projeto-5369` (público na inspeção). A branch padrão é `dev/primeiro-apk`, mas a base que contém OndaTube 0.2.0 e a interface atual é `onda-tube`, commit `9d490987a8cc1fa1f81a53a4ce46279d037b6aa5`. A correção parte dessa base, em `fix/login-video-playback`. Não foi feita reescrita.

APK recebido: `OndaTube-0.2.0-debug.apk`, pacote `dev.projeto5369`, versão 0.2.0 (2), minSdk 26 / targetSdk 35. Certificado SHA-1: `a7b94c083a33fb0f46d0a294902410eb03e46f4e`. Não houve execução desse APK neste ambiente. Os logs anteriores usados como evidência estão na branch `ci-out/onda-tube`, commit `c862783`.

## Causas identificadas

### Reprodução

- Catálogo: páginas públicas de resultados, paginação WEB e metadados watch/oEmbed. A seleção envia ID validado ao player oficial IFrame em WebView. Não existe resolvedor de streams YouTube/InnerTube `/player` nesta base. Media3 é exclusivo da amostra autorizada.
- **Altura zero do iframe:** o CSS selecionava `#player`, mas o IFrame API substitui o div por iframe e pode trocar seu ID. O iframe ficava fora das regras de dimensionamento. Logs anteriores: `ready iframe=412x0 viewport=412x232 body=412x0`, enquanto a WebView tinha `1080x608`. A correção seleciona explicitamente `iframe`, espera a WebView ter tamanho nativo positivo antes de carregar HTML e ajusta o player com dimensões numéricas reais de `innerWidth/innerHeight` em ready/resize. O teste posterior mostrou que CSS `100vh` sozinho continuava resolvendo a zero na criação inicial; não foi considerado suficiente.
- **Corrida na inicialização JavaScript:** a API externa era carregada antes da definição de `onYouTubeIframeAPIReady`. O teste instrumentado com API pronta imediatamente produziu `ReferenceError: onYouTubeIframeAPIReady is not defined`. O callback agora existe antes de carregar a API; CSS é aplicado no cabeçalho antes de iniciar o player.
- **WebView antiga retida:** `remember(video.id, attempt)` criava novo objeto, mas `AndroidView(factory={webView})` sem chave mantinha a instância anterior. Corrigido com `key(video.id, attempt)`; a mídia autorizada recebe chave de fonte também.
- **Timeout único:** a espera inicial de 25 s só observava um instante depois da criação; um buffering posterior poderia continuar indefinidamente. O prazo agora acompanha cada entrada em loading, suspenso fora da tela ativa. Estado `-1` depois de ready mantém espera por gesto; erros sobrevivem a callbacks atrasados.
- Erros 100, 101/150, 153, formato 5 e parâmetros 2 permanecem distintos. HTTP e rede têm mensagens próprias. Falhas da plataforma não provocam rotação de clientes, tokens ou repetição automática infinita. Retry é explícito.
- Os logs anteriores também registram recusas de incorporação. Não é possível concluir que CSS era a única falha. Login próprio não resolve verificações ou restrições do YouTube.

### Login

`Auth.kt` era um placeholder `OfficialOAuthPending`, não conectado a um fluxo real. Não existiam credenciais Google próprias configuradas, autorização, retorno, restauração nem logout efetivo. A interface apenas informava modo visitante. Não foi encontrado um OAuth funcional com erro de SHA-1 a corrigir.

Implementado Credential Manager/Google ID com botão nos Ajustes, estado compartilhado com perfil, cancelamento, timeout, recuperação de falhas, restauração consultando o provedor e limpeza local/provedor no logout. **Configuração externa e validação real continuam pendentes.** Ver `LOGIN_SETUP.md`.

## Referência NewTube

Inspecionados os três arquivos Java no ZIP descompilado recebido (incluindo limitações do JADX), e o fonte público `aleixrodriala/newtube` na tag `v1.17.1`, commit `6f96a1b42bb9272935360d7e5aec34d35f2753f0`, com MediaServiceCore em `52fecea0b697987739813cc747148671bed3f49e`.

- `YTSignInPresenter`: `signInObserve()`, atualização do código, descarte da assinatura ao destruir a tela.
- `YouTubeSignInService`: delegação ao gerenciador de contas, cache de autorização, renovação e invalidação. Esse fluxo usa credenciais e contratos próprios da referência; não foram reutilizados.
- `Media3SourceFactory`: fontes progressivas/DASH/HLS/SABR, deadlines de inicialização, categorias HTTP e retry limitado. Esses contratos não se aplicam diretamente a um iframe oficial. Foram adotados os princípios de prazo e classificação; nenhum extrator ou implementação SABR foi transplantado.
- Licença principal pública conferida: MIT. O módulo SABR inclui avisos/licenças adicionais (Apache 2.0 no material upstream inspecionado). Nenhum código da referência foi copiado; novas dependências Google/AndroidX usam suas licenças próprias.

## Observabilidade e limites

Logs locais `P5369Player` e `P5369Auth`: ID público, etapa, tipo de fonte, estado, categoria, código HTTP quando exposto, tempo até evento PLAYING. Console remoto, URLs completas, cookies, códigos OAuth e tokens não são registrados. IFrame não expõe HTTP de todas as requisições internas nem callback de primeiro quadro; o evento PLAYING não é anunciado como medição de primeiro quadro. Media3 registra seu callback real de primeiro quadro.

## Checklist real pendente

Em Galaxy M34/rede residencial ou móvel: vídeo curto, longo e >1 h; troca; pause/resume; seek; fullscreen; Home; desconexão; vídeo indisponível. Confirmar imagem visível e tempo avançando, não apenas evento ready. Configurar Google Cloud e executar o checklist de `LOGIN_SETUP.md`. Emulador/testes locais não substituem essa validação.
