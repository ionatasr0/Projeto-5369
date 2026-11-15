# OndaTube — interface e correções (branch `onda-tube`, 15/11/2026)

## Interface

Design OndaTube aplicado sobre a arquitetura do Projeto 5369, sem mover transporte/parser para Composables:

- Temas **Preto AMOLED** e **Onda Glass** (escolha em Ajustes, salva no aparelho). O Glass usa luzes em degradê, sem desfoque em tempo real.
- Barra inferior flutuante só com ícones: Seu Tube (estatísticas locais), Início, Shorts, Pesquisa e Você.
- Início com onda animada, chips de tema (busca pública por tema, sem alegar recomendação personalizada), destaque, "Continue assistindo" e cartões Navegar.
- Pesquisa com campo em pílula, voz (se o aparelho tiver reconhecimento), assuntos, recentes removíveis e abrir por link.
- Tela do vídeo: player oficial no topo (nada sobre o iframe), fundo com blur da miniatura (Android 12+), canal, seguir no aparelho, favoritar, compartilhar, descrição e "A seguir".
- Tela cheia do próprio player oficial agora funciona (WebChromeClient + rotação para horizontal; a Activity não é recriada).
- Shorts: feed vertical por tema. Só a página atual cria um player, e só após o toque.
- Você: perfil visitante, contadores, histórico e pastas no formato Onda (Favoritos, Histórico, Canais, Pesquisas).
- Seu Tube: estatísticas calculadas do histórico local.
- Nome do app **OndaTube**, ícone adaptativo (Opção 1 · Play Onda) e fonte Inter (OFL 1.1).

## Correções comprovadas ao vivo (GitHub Actions, workflow `OndaTube (teste ao vivo)`)

1. **Pesquisa quebrada em qualquer aparelho.** O user-agent `Projeto5369/0.1 (Android; …)` fazia o YouTube responder `302 → m.youtube.com`, e o cliente tratava isso como "consentimento". Com `OndaTube/0.2 (Projeto5369; anonymous public catalog)` (sem "Android"/"Mobile", ainda sem se passar por navegador), a página pública volta com `ytInitialData`. Teste ao vivo: 18 vídeos na 1ª página, 20 na 2ª, durações lidas. Um user-agent de navegador falsificado foi testado só como diagnóstico e caiu em `google.com/sorry` (bloqueio) — não foi adotado.
2. **Erro do player apagado.** Depois de `onError`, o YouTube envia o estado `-1`; o app voltava para "carregando" e, 25 s depois, mostrava "o player não respondeu". Agora o erro real continua visível até "Tentar novamente".
3. **Detalhes indisponíveis.** Quando a página `/watch` não traz `videoDetails` (no CI: "Faça login para confirmar que você não é um bot"), o app usa o oEmbed público do YouTube para título e canal e mantém duração/canal já conhecidos da lista.
4. **Shorts**: parser de `shortsLockupViewModel`/`reelItemRenderer` validado ao vivo (33 Shorts lidos), com paginação própria.

## O que não pôde ser comprovado no CI

Reprodução real do vídeo. Os runners do GitHub ficam em datacenter nos EUA e o YouTube os trata como robô: a página `/watch` responde `LOGIN_REQUIRED` e o player incorporado retorna erro 150 até para o vídeo de exemplo oficial (`M7lc1UVf-VE`), com e sem `youtube-nocookie`. O player carrega e informa "pronto" (estado 5) normalmente. A reprodução precisa ser confirmada num aparelho real (rede residencial/móvel), como já indicava o VALIDATION.md.

Os testes `LiveYouTubeTest` (JVM) e `LiveYouTubePlaybackTest`/`ScreenTourTest` (emulador) só rodam com `-Pyt.live=true` / `ytLive=true` e não fazem parte do workflow principal.
