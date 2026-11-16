# Integração nativa NewTube — OndaTube 0.3.0

## Decisão e escopo

O proprietário confirmou usar login e player do jeito do NewTube, substituindo o caminho anterior de identidade Google própria e IFrame. A integração preserva UI/Compose, feed, biblioteca local e distribuição Debug privada. O login por código usa o serviço de contas da referência; não depende de criar um projeto Google Cloud para este APK. A aprovação real ainda precisa ser concedida pelo usuário na página do Google.

## Fonte e licenças

- NewTube v1.17.1: `6f96a1b42bb9272935360d7e5aec34d35f2753f0`.
- MediaServiceCore: `52fecea0b697987739813cc747148671bed3f49e`.
- SharedModules do NewTube: `252138054d130b19de9826e45592cfb893f44f86`.
- Todos obtidos de `aleixrodriala` no GitHub; licenças MIT mantidas nos assets, assim como avisos de SABR/protocolos e J2V8/V8.
- O submódulo permanece sem alterações. `native/upstream.gradle` produz cópias para compilação e registra os patches do host.

## Caminhos efetivamente integrados

Conta: `DeviceAccountSession` → `NewTubeAccountGateway` → `YouTubeSignInService.signInObserve()` → código → página de verificação retornada pelo servidor → polling → conta persistida pelo backend. Restauração lê o mesmo armazenamento. Logout remove as contas do backend e invalida seus caches.

Reprodução: `WatchScreen`/`ShortsScreen` → `YouTubePlayer` em NativePlayer.kt → NativeSources → MediaItemService/MediaItemFormatInfo → fonte SABR, DASH, HLS ou progressiva → ExoPlayer/PlayerView. Play, pause, seek e fullscreen são controles nativos. A tela usa audio focus, pausa no background e libera o player ao sair. Não há abertura automática de navegador para reproduzir.

O renderer de SABR e os adaptadores de formatos/manifesto derivam da implementação NewTube. A fonte de mídia não recebe cabeçalhos de conta arbitrariamente copiados. Erros de proteção/indisponibilidade são apresentados ao usuário; não há código do host para contornar 403 ou desafios.

## Patches de privacidade e correção

- Preferências do backend passam por EncryptedSharedPreferences com chave no Android Keystore; `getData/setData` deixam de gravar arquivos simples. O backup Android permanece desativado.
- Logger upstream substituído por fachada silenciosa; prints de exceções e logs HTTP básicos são removidos/desativados nas cópias. Media3 também fica sem logs brutos; os eventos próprios não incluem códigos de autorização, tokens, cookies, resposta HTTP ou URLs assinadas.
- O armazenamento de conta sinaliza quando terminou de carregar antes da restauração do estado do app.
- O código de usuário mantém o valor original, sem remover hífens. A URL de verificação vem do servidor, exige HTTPS/origem Google ou YouTube e não é construída com credenciais.
- Polling respeita intervalo e validade anunciados, aumenta o intervalo em slow_down e termina em rejeição/expiração. O controlador tem prazo máximo de dez minutos, cancelamento e proteção contra callbacks tardios. O backend não persiste uma nova conta depois de detectar uma assinatura cancelada.
- A alternativa upstream que ignorava hostname em uma rota de DNS-over-HTTPS é trocada pela verificação TLS padrão.

## Anúncios e restrições

Nenhum SponsorBlock, download, skip de segmentos ou Premium simulado foi integrado. `isInlinePlaybackNoAd` é false no host. Este adaptador ainda não renderiza anúncios YouTube nativamente: se o backend anunciar espera de pre-roll, o vídeo é interrompido com mensagem e opção externa em vez de descartar anúncios ou simular que foram exibidos. Vídeos protegidos/privados/DRM sem autorização continuam indisponíveis.

Isso limita os vídeos que podem ser aceitos nesta primeira integração; não há alegação de equivalência integral a todos os formatos, anúncios ou recursos do NewTube.

## Builds e plataformas

JDK 21, SDK 36, AGP 8.9.2, Kotlin 2.1.20, Media3 1.10.1. O target Android continua 35; minSdk 26. O APK Debug padrão é arm64-v8a, com bibliotecas nativas comprimidas. Instrumentação em emulador exige `-PnativeTestAbi=x86_64`.

Clone/CI precisam baixar submódulos recursivamente. GoogleWebClientId/Credential Manager não são mais requisitos do login ativo. Não há publicação automática de Release.

## Validação

Resultados de compilação, unitários, criptografia no dispositivo, emissão real de código e reprodução real serão registrados aqui após a execução. Emissão de código, estado de player e testes sintéticos não comprovam autorização completa de uma conta. O login integral depende de aprovação do proprietário; não será declarado validado sem isso.
