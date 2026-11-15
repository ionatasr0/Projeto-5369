# Resultados — recuperação OndaTube 0.2.1

Data: 15/11/2026. Branch: `fix/login-video-playback`, baseada em `onda-tube` (`9d490987a8cc1fa1f81a53a4ce46279d037b6aa5`). Código compilado: `ece06715ae9514d98eb5480075308b32b482584e`.

## Diagnóstico e implementação

As causas demonstradas no código/logs são: iframe com altura zero; callback IFrame definido depois do carregamento da API (corrida reproduzida no emulador); AndroidView que podia manter a WebView anterior em troca/retry; timeout só inicial; e cancelamento engolido no fallback de metadados. O login era apenas um placeholder não integrado, sem configuração própria.

Foram corrigidos dimensionamento e ordem de inicialização, identidade da WebView por instância, timeout por carregamento ativo, lifecycle/liberação e falha de renderer, tratamento de erros, cancelamento de metadados e logs sanitizados. Credential Manager/Google ID foi integrado ao estado do app e à interface existente, com cancelamento, restauração consultando o provedor e logout. Não foi adotado extrator YouTube, SABR, cliente/tokens de terceiros ou supressão de anúncios.

O diagnóstico completo e as referências NewTube estão em [RECOVERY.md](RECOVERY.md). A configuração do proprietário está em [LOGIN_SETUP.md](LOGIN_SETUP.md).

## Testes e compilação locais

JDK Temurin 21, Gradle 8.11.1, AGP 8.9.2, Kotlin 2.1.20, SDK 35 / Build Tools 35.0.0. Mantida validação TLS com o trust store do ambiente.

Comando de verificação: `:core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest`.

Resultado final local: **BUILD SUCCESSFUL**, 87 tarefas, 21 executadas / 66 atualizadas, 1 min 21 s. A compilação completa anterior passou em 4 min 25 s; a execução final recompilou o ajuste numérico de viewport e a espera pelo tamanho nativo.

| Verificação | Resultado |
| --- | --- |
| CatalogTest | 17 passaram, incluindo cancelamento do fallback |
| EmbeddedStateTest | 3 passaram |
| LocalStoreTest | 2 passaram |
| AccountSessionTest | 7 passaram com provedor falso; sem login Google real |
| Total unitário | 29 passaram; 0 falhas/erros |
| Testes live na execução normal | 5 ignorados por opção explícita |
| Lint | 0 erros, 9 avisos |
| APK app e APK de testes | Compilados |
| Assinatura do APK | Verificada com apksigner |

Execução live adicional, somente `LiveYouTubeTest.searchReturnsLongVideosAndPaginates` e `detailsAndChannelLoad`: **2 testes passaram**, BUILD SUCCESSFUL em 22 s. Busca real: 20 vídeos na primeira página e 20 na segunda; metadados reais com duração de 4.904 s e canal com 30 vídeos. Não foi executado o diagnóstico antigo que experimenta outros user-agents/cookies.

Esses dois testes comprovam catálogo/metadados, não reprodução de vídeos de longa duração.

## GitHub Actions

[Execução atual](https://github.com/ionatasr0/Projeto-5369/actions/runs/43774864932), commit `ece0671`.

- Compilação, testes unitários e lint: sucesso confirmado.
- Emulador API 35: **5 testes passaram**, 0 falhas/erros e 4 ignorados (estes exigem flags live). Passaram layout, Media3 autorizado, pesquisa/ajustes, Shorts e detalhes/favoritos. XML do artefato `device-test-reports` conferido.
- Reprodução real YouTube: **falhou** no teste que exige avanço de tempo. Os dois vídeos públicos de teste retornaram erro IFrame **150**, tanto no host padrão como no host oficial sem cookies. O iframe agora mede **412x232**, viewport e body também **412x232** (antes eram 412x0). Isso comprova a correção do layout, mas não comprova reprodução. O teste de toque terminou sem falha porque é apenas diagnóstico e não exige playback; não é contado como sucesso de reprodução.
- O job live tem `continue-on-error`, portanto a cor geral do workflow não é prova de reprodução. O resultado específico do job é failure, com 1 falha em 2 testes e 0 ignorados. A causa exata da recusa 150 não é exposta pelo IFrame; não foi atribuída conclusivamente ao proprietário do vídeo ou à rede do runner. Nenhuma restrição foi contornada.

[Primeira execução](https://github.com/ionatasr0/Projeto-5369/actions/runs/43773598968): compilou, mas o novo teste de layout falhou com `ReferenceError: onYouTubeIframeAPIReady is not defined`. A ordem dos scripts foi corrigida no segundo commit; não se removeu o teste para ocultar o defeito. Na segunda execução, o teste mostrou que CSS `100vh` isolado ainda dava altura zero; o terceiro commit passou a esperar tamanho nativo e ajustar o iframe por dimensões numéricas do viewport.

Total positivo verificado: **29 testes unitários + 5 instrumentados + 2 de catálogo real**. **Reprodução YouTube e login real permanecem pendentes de aceitação**. Não há evidência de vídeo YouTube tocando neste ambiente.

## APK entregue

- Arquivo: `OndaTube-0.2.1-recovery-debug.apk`, 18.867.337 bytes.
- Saída original: `app/build/outputs/apk/debug/app-debug.apk`; cópia entregue: `artifacts/OndaTube-0.2.1-recovery-debug.apk`.
- Pacote: `dev.projeto5369`, versão 0.2.1 (3), minSdk 26 / targetSdk 35.
- SHA-256 do arquivo: `8171ec4004b2d0a5d87dbd292d24d6c38286617940cd17d05d7c2dd52514901c`.
- SHA-1 do certificado Debug: `7691fc64d3aca2369b8def9ce0e5804b1dfd7235`.
- A assinatura **difere do APK recebido** (`a7b94c083a33fb0f46d0a294902410eb03e46f4e`). Atualização por cima sem desinstalar exige recompilar/assinar com a chave original; ela não foi fornecida. Não foi validada atualização entre esses APKs.
- `googleWebClientId` não fornecido: este APK opera como visitante e mostra a configuração pendente ao tentar login. Não é um APK com login real validado.
- Nenhuma Release publicada. Upload público de APK pelo workflow continua desativado.

## Aceitação pendente e ações do proprietário

Configurar clientes Google próprios, consentimento/testadores e certificado do APK instalado; compilar com `-PgoogleWebClientId=...`; executar login inicial, cancelamento/retorno, reinício/restauração, logout, novo login e revogação real.

Validar em Galaxy M34/rede residencial: vídeo curto, longo e >1 h; troca, pausa/retomada, seek, fullscreen, Home, desconexão e indisponível. O M34 não está conectado a este ambiente. Não foram testados áudio físico, consumo de bateria ou comportamento One UI.

O login no OndaTube é identidade local, sem escopos YouTube/sincronização. Não autentica automaticamente o iframe. Para recursos privados, ainda é necessária autorização específica e, se houver backend, verificação de tokens no servidor.

## Commits de implementação

- `1dbc9bc8066279f71d4b6fcf1fda259474621f95`: player, integração Google própria, testes e documentação.
- `a72341678de744cd10c57165104a18b519229707`: ordem da API IFrame, cancelamento de metadados, HTTP sanitizado e diagnóstico live separado.

- `ece06715ae9514d98eb5480075308b32b482584e`: carregamento após layout nativo e ajuste numérico de iframe em ready/resize.

## Arquivos alterados

- `.github/workflows/android.yml`
- `README.md`
- `THIRD_PARTY_NOTICES.md`
- `app/build.gradle.kts`
- `app/src/androidTest/java/dev/projeto5369/EmbeddedLayoutTest.kt`
- `app/src/main/assets/licenses/THIRD_PARTY_NOTICES.md`
- `app/src/main/java/dev/projeto5369/AppViewModel.kt`
- `app/src/main/java/dev/projeto5369/Auth.kt`
- `app/src/main/java/dev/projeto5369/MainActivity.kt`
- `app/src/main/java/dev/projeto5369/Players.kt`
- `app/src/main/java/dev/projeto5369/Screens.kt`
- `app/src/test/java/dev/projeto5369/AccountSessionTest.kt`
- `core/src/main/kotlin/dev/projeto5369/core/Models.kt`
- `core/src/main/kotlin/dev/projeto5369/core/YouTubeCatalog.kt`
- `core/src/test/kotlin/dev/projeto5369/core/CatalogTest.kt`
- `core/src/test/kotlin/dev/projeto5369/core/EmbeddedStateTest.kt`
- `docs/INTEGRATION.md`
- `docs/LOGIN_SETUP.md`
- `docs/RECOVERY.md`
- `docs/RECOVERY_RESULTS.md`
- `docs/VALIDATION.md`
