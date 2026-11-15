# Catálogo, InnerTube, reprodução e autenticação

## O que foi implementado

`Catalog` expõe pesquisa, detalhes, canal e próxima página. O adaptador `YouTubeCatalog` obtém HTML público de `/results`, `/watch` e `/channel/{id}/videos`. `YouTubeParser` converte `ytInitialData` e metadados de `ytInitialPlayerResponse` em modelos próprios. Suporta `videoRenderer`, `gridVideoRenderer` e a estrutura básica `lockupViewModel`. Abas não selecionadas de canais não entram na listagem.

Paginação usa `/youtubei/v1/search` ou `/youtubei/v1/browse`, com token e contexto WEB **recebidos da própria página pública naquela consulta**, sem versão de cliente hardcoded, API key de terceiros, cookies importados, login de outro cliente, perfil de TV/Android/iOS simulado ou user-agent de navegador falsificado. O user-agent do transporte identifica Projeto5369. Não há chamada InnerTube `/player`, decifração de assinatura, poToken, nsig, DRM, extração de áudio/vídeo ou remoção de anúncios. `ytInitialPlayerResponse` é usado somente para metadados públicos.

Esse contexto descreve o protocolo web público consultado, não uma credencial OAuth nem a identidade de outro aplicativo. Se o YouTube não disponibilizar contexto WEB ou exigir outro perfil, a paginação é interrompida. Não se presume autorização para dados de conta a partir de acesso público.

## Limites e falhas

- Interfaces internas sem contrato público estável. HTML, renderizadores e tokens podem mudar; apenas o adaptador/parser devem mudar.
- Não foi possível confirmar ao vivo o esquema atual: o proxy deste ambiente recusou túneis HTTPS a `www.youtube.com` com 403. Fixtures são sintéticas e estão identificadas nos testes.
- HTTP 429 não causa loop automático; a interface pede que o usuário aguarde. 401/403 e redirects de consentimento/autorização param a chamada. Não há mecanismo para transpor esses controles.
- O cliente não resolve consentimento automaticamente. O usuário pode abrir o vídeo no aplicativo/site oficial. Consentimento do navegador externo não é importado.
- Duração ausente é ocultada quando existe um mínimo ativo; transmissões ao vivo e premieres sem duração podem ficar fora da lista.
- Máximo de 8 MiB por resposta; mudanças incompatíveis geram erro legível. Alguns esquemas novos podem resultar em página vazia e requerer ajuste do parser.
- Cursosres são de sessão e não são persistidos. Não há armazenamento de HTML, cookies ou tokens de sessão em disco.
- O transporte usa TLS normal e não segue redirects. Não reenvia credenciais nem troca de cliente após recusa.

## Reprodução

### YouTube

IFrame Player API oficial em Android WebView, com user-agent real do sistema e `loadDataWithBaseURL("https://dev.projeto5369/")`, identificando o applicationId conforme orientação oficial de identificação de apps WebView. O domínio é o identificador sintético do pacote, não alegação de domínio web próprio. Sem autoplay; é necessário tocar no player.

Eventos mapeados: carregando, pronto, reproduzindo, pausado, encerrado e erro. Erros 100, 101/150 e 153 são visíveis. Timeout de 25 segundos permite tentar novamente ou abrir no YouTube. Histórico é escrito após evento `PLAYING`, não ao entrar na tela. O adaptador pausa no background e destrói o WebView ao sair.

A incorporação depende de WebView atualizado, serviços/identificação do dispositivo, disponibilidade regional, permissões do proprietário e decisões do YouTube. WebView Media Integrity pode exigir configuração externa. Não há garantia de reprodução para qualquer vídeo. **Reprodução real YouTube ainda requer teste comprovado no dispositivo**. Sucesso de teste de HTML/eventos não equivale a imagem/áudio reproduzidos.

Referências oficiais: https://developers.google.com/youtube/iframe_api_reference e https://developers.google.com/youtube/terms/required-minimum-functionality#api-client-identity-and-credentials

### Media3

`AuthorizedMediaSource(httpsUri, authorizationReference)` é o contrato para mídia que o titular autoriza reproduzir. Media3 padrão trata MP4/progressivo, MPD/DASH e M3U8/HLS por módulos oficiais; seleção de formato por URI/MIME. Não há autorização de conteúdo derivada automaticamente da URL. Responsabilidade do provedor fornecer fonte e referência válidas.

A tela de teste usa amostra Big Buck Bunny da coleção de teste ExoPlayer; obra Blender Foundation, CC BY 3.0. Não é vídeo YouTube e não valida o adaptador YouTube. Não há download de mídia, playback em background, seleção manual de qualidade ou DRM nesta versão.

### SABR

SABR não é um manifesto DASH/HLS. O material de referência descreve sessão stateful, mensagens protobuf, fragmentos UMP, escolhas de áudio/vídeo e integração customizada de MediaSource. Um endpoint SABR não pode ser passado diretamente ao ExoPlayer como um MPD. Não foi criado motor SABR. Nenhum mecanismo de troca de identidade, obtenção de tokens de proteção ou alteração de autorização dos projetos de referência foi adotado.

## Login

O estudo de SmartTube/MediaServiceCore encontrou serviço OAuth separado e fluxo de device code/polling; NewTube adapta esse fluxo de TV para uma tela touch, conforme comentário de `MobileSignInActivity`. Isso não autoriza reutilizar o cliente OAuth desses aplicativos nem aplicar device code de TV ao nosso app Android.

Na recuperação 0.2.1, o placeholder foi substituído por `AccountSession` e `GoogleAccountGateway` (Credential Manager / Google ID), separados do catálogo e do iframe. Há botão, cancelamento, timeout, restauração pelo provedor e logout; tokens não são persistidos. Ainda são necessários projeto Google Cloud próprio, cliente Android associado ao pacote `dev.projeto5369` e ao SHA-1 da assinatura, cliente Web do mesmo projeto, consentimento e validação real. Ver `LOGIN_SETUP.md`. Não se usa WebView de login. A identidade Google não autentica o iframe nem concede URLs de mídia ou escopos YouTube.
