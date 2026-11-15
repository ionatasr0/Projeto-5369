# Validação — Operação Primeiro APK

Data: 15/11/2026. Repositório encontrado vazio, desenvolvimento em `dev/primeiro-apk`. Não houve substituição de implementação anterior.

## Executado localmente

Ambiente Linux x86_64, Temurin JDK 21, SDK 35/Build Tools 35.0.0, Wrapper Gradle 8.11.1 com checksum oficial, AGP 8.9.2 e Kotlin 2.1.20.

```sh
./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest --stacktrace --console=plain
```

Resultado: **BUILD SUCCESSFUL**, 87 tarefas (30 executadas, 57 atualizadas), 1 min 6 s na execução final incremental. A primeira execução completa também passou, em 6 min 49 s.

| Verificação | Resultado efetivo |
| --- | --- |
| `CatalogTest` | 14 testes, 0 falhas, 0 erros, 0 ignorados |
| `LocalStoreTest` | 2 testes, 0 falhas, 0 erros, 0 ignorados |
| Lint Debug | 0 erros, 17 avisos |
| APK aplicativo | Gerado, 15.713.426 bytes |
| APK instrumentado | Compilado |
| Assinatura | Verificada por `apksigner`, APK Signature Scheme v2 |
| Manifest | `dev.projeto5369`, versão 0.1.0/1, min API 26, target/compile 35 |
| ABIs | arm64-v8a, armeabi-v7a, x86, x86_64 |
| Licenças no APK | Apache 2.0, BSD-3-Clause/Protobuf e notificações conferidas via ZIP |

Testes de catálogo usam fixtures **sintéticas próprias** e MockWebServer: busca, paginação com contexto público da página, URL encoding, duração, exclusão de Shorts, metadados, aba selecionada do canal, renderer moderno, cancelamento HTTP, 429, 403, redirect bloqueado, limite de resposta e continuação repetida. Não são captura ou evidência de respostas atuais do YouTube.

Persistência: arquivo DataStore real em diretório temporário, gravação de histórico/favoritos/ajustes/pesquisas, fechamento e recriação da instância, releitura e limpeza preservando favoritos. Player: validação de IDs no HTML e mapeamento de eventos oficiais; isso não prova reprodução.

Avisos de lint: 13 recomendações de atualizar versões, 1 recomendação de regras adicionais de extração/backup, 1 ícone ausente e 2 sugestões de KTX. Não foram suprimidos para mascarar erros. A interface permanece provisória.

## GitHub Actions

- [Run 1](https://github.com/ionatasr0/Projeto-5369/actions/runs/43758409547): falhou na preparação do SDK. Causa comprovada em log: a action tentou instalar o pacote removido `tools`. Corrigido no commit `eee6c19`, especificando os pacotes atuais e runner Ubuntu 24.04.
- [Run 2](https://github.com/ionatasr0/Projeto-5369/actions/runs/43758512517), commit `eee6c19`: build e 2 testes de navegação em emulador API 35 concluídos com sucesso.
- [Run 3](https://github.com/ionatasr0/Projeto-5369/actions/runs/43759027807), commit `6642dca`: build e **3 testes instrumentados passaram**, 0 falhas, 0 erros, 0 ignorados. XML baixado e conferido: `authorizedSampleRendersAndAdvances` (5,475 s), `searchLinkValidationAndLocalSettings` (3,668 s), `opensVideoDetailsWithoutCatalogAndSavesFavorite` (2,167 s). Build instrumentado e execução: 1 min 8 s. Relatórios disponíveis como `device-test-reports`; não há APK público.

Total comprovado: **19 testes passaram** (16 locais + 3 no emulador). Não foram testes executados no Samsung M34.

Após esse run, a auditoria de dependências completou os avisos Apache/BSD e a identificação de Protobuf nas configurações. O APK final foi recompilado com sucesso em 9 segundos. Essas últimas alterações só afetam textos/licenças, sem alterar lógica de catálogo, persistência ou player. O relatório de dependências/POMs está incluído no pacote de evidências.

O workflow condiciona o upload de APK a repositório privado, em respeito à distribuição privada solicitada. O repositório foi encontrado público; sua visibilidade não foi modificada. O APK é entregue diretamente no chat/workspace. Logs e relatórios são artefatos do Actions.

## Reprodução e limites de validação

- YouTube ao vivo: acesso recusado pelo proxy deste ambiente com HTTP 403 na abertura do túnel a `www.youtube.com`. Pesquisa, canal, detalhes, continuação e reprodução **não foram validados contra o YouTube real**.
- Player incorporado oficial: implementação real de IFrame API, controles/anúncios preservados, lifecycle, estados/erros e fallback externo. Falta comprovar imagem, áudio e avanço de tempo com vídeo permitido no dispositivo. Não confundir player pronto ou HTML gerado com reprodução validada.
- Media3: amostra autorizada acessível por HTTPS (HTTP 200 observado). Teste instrumentado **passou** em Android 15/API 35: primeiro frame decodificado, posição >= 1.000 ms, `isPlaying=true` e pausa confirmada. Não verifica áudio físico ou reprodução YouTube.
- Samsung Galaxy M34 5G: não conectado ao ambiente. Não há medidas de RAM, fluidez, bateria, áudio físico ou comportamento específico da One UI.
- Login: arquitetura preparada, implementação OAuth própria ainda depende de configuração Google Cloud/pacote/certificado/consentimento. Aplicativo funciona como visitante.
- SABR: investigado em documentação dos upstreams; motor não implementado, conforme escopo.

## Artefato local

`/workspace/artifacts/Projeto-5369-0.1.0-debug.apk`

SHA-256:

```text
e0f3edf5ad3c644d674e8b785f2cc1b53fa94e9c0b00bec30c7f077c759ff786
```

Logs e XMLs reais: `/workspace/artifacts/projeto5369-validation/`. O APK é assinado com certificado Debug, somente para testes privados. CI e builds locais podem produzir hashes diferentes por certificado Debug e ambiente.

## Aceitação no M34 ainda necessária

Instalar o APK, pesquisar termo com vídeos acima de 20 min, carregar segunda página, abrir detalhes/canal e retornar preservando os resultados. Reiniciar o app após favoritar e alterar filtros. Abrir vídeo permitido via URL, tocar no player oficial, confirmar imagem/áudio/tempo, pausar, ir ao background e voltar, confirmar histórico. Testar offline e um vídeo com incorporação proibida. Registrar evidência e resultado sem interpretar fallback externo como reprodução interna.
