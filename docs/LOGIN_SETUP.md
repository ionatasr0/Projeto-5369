# Configuração do login Google — OndaTube

O código anterior tinha apenas `OfficialOAuthPending`; não existiam pedido de autorização, botão funcional, callback nem sessão a reparar. A implementação usa Credential Manager + Google ID, com seleção nativa de conta, sem WebView de login ou cliente emprestado do NewTube.

## O proprietário precisa configurar

1. Criar/selecionar projeto próprio no Google Cloud/Google Auth Platform.
2. Configurar Branding, Audience e consentimento. Se estiver em testes, incluir as contas de teste. Observar restrições de organização/idade da conta.
3. Criar cliente OAuth **Android** com pacote `dev.projeto5369` e SHA-1 do certificado que assina o APK instalado. Registrar certificados de debug e release separadamente. O SHA-1 muda se a chave mudar; o nome de versão não muda esse requisito.
4. Criar cliente OAuth **Web application** no mesmo projeto. Copiar apenas o **client ID público**, terminado em `.apps.googleusercontent.com`. O Credential Manager usa esse ID como audiência. Não embutir client secret no APK.
5. Compilar: `./gradlew :app:assembleDebug -PgoogleWebClientId=SEU_CLIENT_ID.apps.googleusercontent.com`. Alternativamente definir `googleWebClientId` no `~/.gradle/gradle.properties` do ambiente de compilação. Não é necessário compartilhar senha ou token.
6. Conferir o certificado do APK gerado: `$ANDROID_HOME/build-tools/35.0.0/apksigner verify --print-certs app/build/outputs/apk/debug/app-debug.apk`. Registrar o SHA-1 exibido no cliente Android. `./gradlew :app:signingReport` também permite conferência.
7. No aparelho com Google Play Services atualizado, abrir **Você → Ajustes → Conta Google → Entrar com o Google**.

O APK sem `googleWebClientId` continua compilando e funcionando como visitante. O botão explica a configuração pendente; isso não é login validado.

## Escopo e sessão

- A identidade retornada pelo provedor Google atualiza o perfil local. Nenhum escopo YouTube é pedido; não há sincronização de biblioteca ou acesso a recursos privados nesta entrega.
- O ID token retornado não é salvo, enviado ao catálogo nem registrado. Nome da conta fica apenas em memória. Somente o bit não sensível de consentimento para restauração fica em SharedPreferences, com backup Android desativado.
- Na próxima criação do app, a restauração consulta novamente o provedor, limitada a contas previamente autorizadas e credenciais imediatamente disponíveis. Pode existir confirmação/seleção do provedor. Sem credencial válida, permanece visitante; o nome salvo nunca é usado como prova de sessão.
- Cancelamento, timeout de 60 s, provedor indisponível e erros deixam a tela utilizável. Logout desativa a restauração local e chama `clearCredentialState`; a falha da limpeza é informada e permite tentar novamente.
- Logout local não revoga autorização na conta Google. Revogação é feita pelo proprietário da conta nas conexões de apps do Google. Não existe refresh token próprio neste app; o provedor gerencia suas credenciais.
- Se um backend ou recurso privado for adicionado, validar ID tokens no backend (assinatura, emissor, audiência, expiração e nonce quando aplicável) e implementar a autorização específica antes de confiar nesses dados no servidor.

**O login no OndaTube não autentica o iframe do YouTube**. O player oficial mantém sessão e restrições próprias. Não usar o token Google ID como cookie do iframe ou como autorização de stream.

## Validação real obrigatória após configurar

Login inicial; cancelar o seletor; repetir login; voltar do provedor; encerrar e abrir o app; restaurar; sair; abrir novamente sem restaurar; entrar novamente; revogar a conexão e confirmar que não há sessão local fictícia. Uma conta sem autorização não recebe acesso privado: nenhum recurso privado foi implementado.

Referências: https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation e https://developer.android.com/identity/sign-in/credential-manager .
