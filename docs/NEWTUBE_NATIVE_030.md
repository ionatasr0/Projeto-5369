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

Validação em 16/11/2026, código `5251340b1212fff93e11090c4633a89c00448dee`, branch `fix/login-video-playback`:

| Verificação | Resultado |
| --- | --- |
| Compilação local Debug, instrumentação, unitários e lint | BUILD SUCCESSFUL; 1 min 41 s; 294 tarefas |
| Unitários | 39 aprovados; zero falhas/erros; seis testes externos opcionais ignorados |
| Android lint | Zero erros; 14 avisos |
| CI do código entregue | Build e navegação aprovados no [run 43795937462](https://github.com/ionatasr0/Projeto-5369/actions/runs/43795937462); diagnóstico live não solicitado nessa execução |
| Emissão real de código de dispositivo e cancelamento | Aprovado em Android API 35 no [run 43794401941](https://github.com/ionatasr0/Projeto-5369/actions/runs/43794401941); código não exportado |
| Preferências criptografadas no dispositivo | Aprovado, incluindo Android Keystore e ausência de valores simples no armazenamento |
| Media3 com mídia de teste | Primeiro quadro e avanço de posição aprovados; não comprova YouTube |
| YouTube real sem conta aprovada | Falhou antes de preparar a fonte; resposta categorizada como autorização/indisponibilidade; nenhum primeiro quadro |
| Aprovação de conta, restauração, logout e novo login reais | Pendentes da autorização do proprietário no dispositivo |

O teste live usa `continue-on-error`: um workflow verde não prova reprodução do YouTube. Na execução live citada, `playbackAdvancesInNativePlayer` falhou. O adaptador inicialmente agrupava bot-check e indisponibilidade; o código entregue distingue os dois, sem atribuir uma causa externa não comprovada. Também não foram validados os cenários completos de vídeos curto/longo/mais de uma hora, troca, seek e fullscreen com mídia real da plataforma.

## Diagnóstico e arquivos principais

O caminho anterior usava IFrame/WebView para os vídeos e identidade Google própria para a conta, sem integrar a sessão ao backend NewTube. Os relatórios históricos registram a corrida de inicialização/altura do IFrame e a recusa externa 150. A integração 0.3.0 substitui esses caminhos por conta do backend e player nativo; não transforma a recusa externa em sucesso. A repetição da Home já foi tratada na 0.2.2 por rotação das consultas, intercalação e deduplicação, preservadas nesta versão; a Home continua pública, sem recomendação personalizada oficial.

Principais arquivos: `app/src/main/java/dev/projeto5369/Auth.kt`, `nativeplayer/NewTubeBackend.kt`, `NativePlayer.kt`, `nativeplayer/NativeSources.kt`, telas Compose e ViewModel; módulos `native/*`, `settings.gradle.kts`, Gradle do app e workflows Android. O diff completo está nos commits da branch: `24abed4` (integração), `c7fee09` (wrapper do CI), `d2e39c4` (recuperação de autorização/layout) e `5251340` (intervalo padrão de polling/teste). A validação dos wrappers permanece ativa; o CI remove apenas jars de wrappers upstream não usados pelo build do host.

## APK entregue e próxima validação

- `artifacts/OndaTube-0.3.0-native-debug.apk`, versão 0.3.0 (5), arm64-v8a, 37.085.820 bytes.
- SHA-256 do arquivo: `636ab8b35ab26580ce28ea7467ae5398e6354e5e220777da744f8400717c37dd`.
- Assinatura Debug preservada em relação às versões 0.2.1/0.2.2; SHA-256 do certificado: `daed4c8b11429668992b853ed030d71a156f447235323b11a028888223592112`.
- APK entregue diretamente ao proprietário; nenhuma Release publicada automaticamente.

No M34: abrir Você → Ajustes → Conta YouTube → Entrar; revisar a autorização na página Google indicada, inserir o código e voltar ao app. O navegador é usado para autorizar a conta; o vídeo usa Media3 dentro do app. Depois, verificar reprodução pública e controles, reiniciar para restaurar a sessão, sair e entrar novamente. Login integral e reprodução pública permanecem critérios pendentes até esses resultados reais. Não é necessário fornecer um GoogleWebClientId para o fluxo ativo desta versão.
