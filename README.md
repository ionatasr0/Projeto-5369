# Projeto 5369 — primeiro APK

Cliente Android Kotlin/Compose de vídeos longos, com interface provisória. Distribuição pretendida: APK privado. O repositório foi encontrado **público e vazio** em 15/11/2026; sua visibilidade não foi alterada. Não publicar o APK em Releases públicas.

## Compilar

JDK 21, SDK Android 35 e Gradle Wrapper 8.11.1:

```sh
./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Instalar com `adb install -r app-debug.apk`. Android 8.0/API 26 ou superior; referência de produto: Samsung Galaxy M34 5G. Não há medição de desempenho no M34 nesta entrega.

O workflow [Primeiro APK](.github/workflows/android.yml) compila, testa, executa lint, gera APK e registra logs/relatórios. O segundo job usa emulador API 35 para navegação. Artefatos expiram em sete dias. O upload do APK é condicionado a repositório privado; em repositório público ele é apenas compilado, para preservar a distribuição privada solicitada. Logs e relatórios continuam disponíveis; não foi criada uma release.

## Usar

- Home: consulta temática explícita, com filtro local padrão de 20 minutos. Não representa recomendações personalizadas oficiais.
- Pesquisa: termos livres, resultados paginados e pesquisas recentes. Se uma página não tiver vídeos acima do filtro, usar “Carregar mais” ou ajustar a duração.
- Abrir por URL/ID: permite chegar aos detalhes e ao player oficial mesmo sem catálogo disponível. Não converte um vídeo indisponível em disponível.
- Detalhes: reprodução, canal e favoritos locais.
- Biblioteca: favoritos e histórico de reprodução iniciada, locais ao aparelho.
- Ajustes: duração mínima, ocultar assistidos, limpar histórico e teste Media3 com amostra autorizada.

## Arquitetura

- `:core`: modelos Kotlin, `Catalog`, cursores opacos, parser público YouTube isolado, transporte OkHttp cancelável, erros classificados, regras da biblioteca e estados de reprodução. Sem dependência Android.
- `:app`: telas Material 3, Navigation Compose, `AppViewModel` com estados separados de catálogo, DataStore, Coil e dois adaptadores de reprodução.
- YouTube: IFrame Player API oficial em WebView com identificação pelo pacote Android, anúncios/controles originais e fallback externo.
- Media3: fontes HTTPS autorizadas com suporte instalado a progressivo/DASH/HLS. Não recebe URLs extraídas do YouTube.
- Autenticação: contrato separado `AccountAuth`; primeiro APK funciona como visitante. OAuth de conta depende de configuração própria.

Rede assíncrona, parsing em `Dispatchers.Default`, páginas sob demanda, imagens com cache de memória/disco do Coil, corpo limitado a 8 MiB. Biblioteca limitada a 200 entradas de histórico, 500 favoritos e 20 pesquisas. Cancelamento ao substituir consulta e ao encerrar o ViewModel. Players pausam no background e liberam recursos ao sair.

## Estado de validação

Consultar [VALIDATION.md](docs/VALIDATION.md) para resultados efetivamente executados. Testes de contratos com respostas sintéticas não comprovam operação ao vivo do YouTube. Não declarar reprodução real validada sem evidência de imagem, áudio e avanço de tempo no dispositivo.

## Documentação

- [Integração, bloqueios e login](docs/INTEGRATION.md)
- [Estudo de referências e licenças](docs/REFERENCES.md)
- [Contratos para o Claude](docs/CLAUDE_UI_CONTRACT.md)
- [Notificações de dependências](THIRD_PARTY_NOTICES.md)

O código original do Projeto 5369 é reservado ao titular do projeto; nenhuma licença aberta foi concedida para ele. Dependências mantêm suas próprias licenças.
