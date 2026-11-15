# Estudo de referências — 15/11/2026

Inspeção de fontes e documentação, sem incorporar código, recursos ou dependências desses repositórios ao aplicativo. SHAs abaixo fixam o material observado; não representam validação de todo o projeto ou de toda sua cadeia de dependências.

| Projeto | Revisão observada | Licença raiz declarada |
| --- | --- | --- |
| [yuliskov/MediaServiceCore](https://github.com/yuliskov/MediaServiceCore/tree/1087a014f027113ab0cb9ff77b274563530bb2f1) | `1087a014f027113ab0cb9ff77b274563530bb2f1` | MIT |
| [yuliskov/SmartTube](https://github.com/yuliskov/SmartTube/tree/e58b9934bb321286659ec86ce7e6b6fb46cbbd90) | `e58b9934bb321286659ec86ce7e6b6fb46cbbd90` | MIT |
| [aleixrodriala/newtube](https://github.com/aleixrodriala/newtube/tree/6f96a1b42bb9272935360d7e5aec34d35f2753f0) | `6f96a1b42bb9272935360d7e5aec34d35f2753f0` | MIT |
| [Flow-Tube/Flow](https://github.com/Flow-Tube/Flow/tree/e9e8d6af919e5626f7f20794dcfe8175e6990fbe) | `e9e8d6af919e5626f7f20794dcfe8175e6990fbe` | GPL-3.0 |
| [TeamNewPipe/NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor/tree/b80dd8a1f932758fdecd58dce3f788b684b4f5a4) | `b80dd8a1f932758fdecd58dce3f788b684b4f5a4` | GPL-3.0 |

## MediaServiceCore (prioridade)

Arquivos examinados: `mediaserviceinterfaces/…/MediaItemService.java`, `SignInService.java`, `data/MediaItemFormatInfo.java`; `youtubeapi/…/search/v2/SearchService2.kt`, `browse/v2/BrowseService2.kt`, `innertube/InnertubeService.kt`, `innertube/initialresponse/InitialResponseService.kt`, `innertube/impl/MediaItemFormatInfoImpl.kt` e `googleapi/oauth2/OAuth2Service.java`.

Achados de engenharia:

- Contratos separados para grupos/listas, metadados, formatos e autenticação. Nosso `Catalog` mantém essas fronteiras sem importar suas interfaces/RxJava.
- Pesquisa inicial e continuação são operações distintas; o token deve continuar ligado ao contexto original e cada chamada de transporte é descartável. Não se deve reexecutar uma instância de chamada consumida.
- Browse possui variações web/TV e tratamento de grupos/canais. O Projeto 5369 não adota fallback para identidade de TV. A aba de vídeos do canal é consultada explicitamente.
- Informações de mídia separam adaptive/progressive, manifests HLS/DASH, motivos de impossibilidade e presença de SABR. Metadados não demonstram que a fonte seja reproduzível ou autorizada.
- O parser de resposta inicial é um limite isolável. Nosso scanner balanceado lê somente objetos JSON já publicados na página; não executa JavaScript remoto para decifrar mídia.
- OAuth possui serviço próprio e polling/device code nas referências. Nenhum identificador, segredo, cookie ou configuração de cliente foi copiado.

Dependências observadas no código de referência incluem Retrofit, Gson, RxJava e utilitários Liskovsoft. Não são incorporadas em bloco. A licença MIT raiz não dispensa auditoria de arquivos/submódulos/dependências; o primeiro APK usa código original e bibliotecas diretas listadas em THIRD_PARTY_NOTICES.

## SmartTube e NewTube

SmartTube: examinados licença e `GoogleSignInPresenter.java`; delegação de login para serviço separado com apresentação de código e estado de conta. NewTube declara derivação de SmartTube; examinados README, licença, `MobileSignInActivity.java`, `docs/mobile-port/SABR-MEDIA3-2026-09-08.md` e `sabr-media3/PROVENANCE.md`.

NewTube documenta uma adaptação touch do fluxo device code TV e um módulo SABR Media3 experimental. Sua proveniência descreve responsabilidades próprias de transporte, fragmentação UMP, protobuf, buffers e uma ponte dependente da versão do Media3. Esses mecanismos não foram transferidos. Os números de reprodução/performance do upstream não são resultados do Projeto 5369.

## Flow e NewPipeExtractor

Verificados árvore, licença e README. Ambos declaram GPLv3. Foram usados para entender a abrangência de uma integração de catálogo/extrator e o custo de manutenção, sem copiar implementação, fixtures, recursos, assinatura de código ou incorporar bibliotecas desses projetos. Não se adicionou NewPipeExtractor ao Gradle. Não se presume compatibilidade GPL com a distribuição proprietária pretendida.

## Decisões

Implementação original de transporte/parser/modelos; catálogo anônimo; nenhuma credencial de terceiro; fallback de reprodução para IFrame oficial; módulos DASH/HLS padrão do Media3 restritos a fontes autorizadas; SABR adiado; OAuth próprio pendente. O APK não afirma oferecer supressão de anúncios, assinaturas de conta, playback protegido, download ou contorno de verificações.
