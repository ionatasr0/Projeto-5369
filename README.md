# OndaTube — Projeto 5369

Cliente Android Kotlin/Compose de vídeos longos. Versão 0.3.0: login por código e reprodução nativa Media3 integrados ao backend NewTube, mantendo a interface, o catálogo público e a biblioteca local do OndaTube.

## Compilar

Clonar com submódulos: `git clone --recurse-submodules https://github.com/ionatasr0/Projeto-5369.git`.

JDK 21, Gradle Wrapper 8.11.1, SDK Android 36 e Build Tools 35.0.0:

```sh
./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

APK em `app/build/outputs/apk/debug/app-debug.apk`. O Debug padrão contém **arm64-v8a**, adequado ao Galaxy M34. Para emulador x86_64, passar `-PnativeTestAbi=x86_64`; para outro ABI, passar o nome correspondente. Android mínimo 26; targetSdk 35. Não publicar Release automaticamente.

## Login e reprodução

Em **Você → Ajustes → Conta YouTube → Entrar na conta YouTube**, o aplicativo solicita um código ao serviço, mostra a página de verificação retornada pelo Google e acompanha a autorização. O usuário aprova na página do Google e retorna ao app. Esse fluxo não oficial deriva do cliente de TV usado pelo NewTube; o Google pode identificá-lo como acesso de TV. Não é o antigo Credential Manager dependente de um `googleWebClientId` do proprietário.

Watch e Shorts usam `PlayerView`/ExoPlayer nativos, sem IFrame como reprodução principal. O adaptador usa as fontes disponibilizadas pelo backend (DASH, HLS, progressivo ou SABR). O iframe anterior permanece como código histórico/testado, sem ser o caminho chamado por essas telas. O navegador abre somente por ação explícita de autorização ou fallback externo.

Restrições de conta, disponibilidade e proteção continuam podendo impedir fontes. Não há simulação de Premium, importação de cookies ou desbloqueio de vídeos privados. O host desabilita a opção upstream `isInlinePlaybackNoAd`; se uma resposta anunciar pre-roll que este adaptador não sabe reproduzir, informa a limitação em vez de descartá-lo silenciosamente. Não foram incorporados SponsorBlock, downloads ou reprodução em segundo plano.

Tokens e dados do backend são armazenados por EncryptedSharedPreferences/Android Keystore. A persistência upstream em arquivos simples foi redirecionada para esse armazenamento; logs upstream e de transporte são suprimidos. Os logs próprios contêm apenas etapas, ID público, categoria e primeiro quadro.

## Arquitetura

- `:core`: catálogo público, parser, regras de biblioteca e descoberta temática; sem dependência Android.
- `:app`: Compose, Navigation, DataStore, Coil, sessão por código, adaptadores e player nativo.
- `vendor/newtube`: fonte pública fixada na tag v1.17.1, com submódulos nos commits da referência.
- `native/`: builds compatíveis com o host, preparação auditável das fontes, privacidade/armazenamento e política de polling. Os submódulos upstream permanecem sem modificações.
- A Home mantém seleção pública por temas, atualizações rotativas e deduplicação. Login não transforma essa Home em recomendações oficiais personalizadas nem sincroniza automaticamente favoritos locais.

## Documentação

- [Integração nativa e validação 0.3.0](docs/NEWTUBE_NATIVE_030.md)
- [Notificações e licenças](THIRD_PARTY_NOTICES.md)
- [Feed e tela 0.2.2 — histórico](docs/FEED_PLAYER_022.md)
- [Recuperação 0.2.1 — histórico](docs/RECOVERY_RESULTS.md)

A compilação não comprova login completo nem reprodução real. Consultar os resultados executados e pendências no relatório da versão.
