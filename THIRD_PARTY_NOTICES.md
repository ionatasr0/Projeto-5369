# Notificações de terceiros

Código original do Projeto 5369: todos os direitos reservados ao titular do projeto. Esta declaração não substitui as licenças dos componentes abaixo.

Bibliotecas distribuídas no APK, conforme dependências Gradle:

| Componente | Licença | Origem |
| --- | --- | --- |
| Kotlin stdlib, kotlinx.coroutines, kotlinx.serialization | Apache-2.0 | https://github.com/JetBrains/kotlin e https://github.com/Kotlin |
| AndroidX Activity, Compose, Material 3, Lifecycle, Navigation, DataStore | Apache-2.0 | https://android.googlesource.com/platform/frameworks/support/ |
| AndroidX Media3 (ExoPlayer, DASH, HLS, UI) | Apache-2.0 | https://github.com/androidx/media |
| OkHttp e Okio | Apache-2.0 | https://github.com/square/okhttp e https://github.com/square/okio |
| Coil | Apache-2.0 | https://github.com/coil-kt/coil |
| Accompanist DrawablePainter | Apache-2.0 | https://github.com/google/accompanist |
| Guava, failureaccess e listenablefuture | Apache-2.0 | https://github.com/google/guava |
| JSpecify annotations | Apache-2.0 | https://github.com/jspecify/jspecify |
| AndroidX annotations, collection, concurrent, core e demais transitivas AndroidX (exceto Protobuf abaixo) | Apache-2.0 | Android Open Source Project |
| DataStore preferences external Protobuf (Google) | BSD-3-Clause | https://github.com/protocolbuffers/protobuf |
| JetBrains annotations | Apache-2.0 | https://github.com/JetBrains/java-annotations |

Texto Apache 2.0 incluído no APK em `assets/licenses/Apache-2.0.txt`; copyright e licença Protobuf em `assets/licenses/Protobuf-BSD-3-Clause.txt`. Copyrights permanecem com os respectivos autores. Não foram modificadas as bibliotecas upstream. Gradle Wrapper: Apache-2.0, Gradle Inc. e contribuidores, ferramenta de compilação incluída no repositório.

JUnit (EPL-1.0), Hamcrest (BSD), MockWebServer (Apache-2.0), Robolectric (MIT) e AndroidX Test (Apache-2.0) são somente ferramentas de teste, não dependências da versão de produção. A resolução completa pode ser inspecionada com `./gradlew :app:dependencies --configuration debugRuntimeClasspath`.

A amostra externa Big Buck Bunny é de (c) 2008 Blender Foundation, https://www.bigbuckbunny.org/, CC BY 3.0: https://creativecommons.org/licenses/by/3.0/. A mídia não é empacotada no APK; a tela de teste referencia uma amostra publicada na coleção de testes ExoPlayer do Google.

YouTube e Google são marcas de seus titulares. O player incorporado e as miniaturas são fornecidos por serviços externos. Este aplicativo não é um cliente oficial do YouTube.

Nenhum código de SmartTube, MediaServiceCore, NewTube, Flow ou NewPipeExtractor foi copiado ou incluído. Licenças de referência documentadas em `docs/REFERENCES.md` não são atribuídas ao código original deste projeto.
