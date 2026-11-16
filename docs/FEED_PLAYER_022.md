# OndaTube 0.2.2 — feed e tela de vídeo

Código: `57f0a3b9be3f146327cff402d143b5e079bc926d`, branch `fix/login-video-playback`.

## Causas e mudanças

- A Home vinha sempre da mesma busca temática fixa. A seleção Tudo agora intercala três buscas públicas de um conjunto rotativo. Cada tema específico combina a busca original com uma alternativa temática. A edição avança entre atualizações e é persistida para a próxima abertura.
- O merge remove IDs repetidos, intercala fontes/canais e prioriza vídeos que não estavam na seleção anterior e não foram assistidos. A Home não repete, no feed principal, os vídeos já mostrados na seção Continue assistindo. Não há promessa de catálogo totalmente diferente em toda atualização: resultados públicos podem se sobrepor.
- Botão Atualizar no início. As continuações de cada fonte são paginadas em rodízio, sem misturar tokens entre consultas.
- A seguir deixou de reaproveitar Home/pesquisa. Faz consulta pública separada pelo assunto do vídeo selecionado, sem incluir o próprio vídeo; o estado é associado ao ID aberto e acompanha o retorno entre telas. É busca contextual, não recomendação personalizada da conta YouTube.
- Player fixo no topo, em 16:9 e sem recortar os controles oficiais. Detalhes/canal/descrição e outros vídeos rolam abaixo. Cabeçalho simplificado; ação de abrir no YouTube não fica duplicada entre player e informações.

## Login: continua pendente

Nenhum cliente Google próprio foi fornecido. O APK não possui `googleWebClientId`; portanto **não autentica conta Google**. A mensagem foi tornada compreensível para o usuário, mas isso não é uma correção de autenticação. É necessário configurar cliente Web e Android no mesmo projeto Google Cloud, consentimento e SHA-1 da assinatura, conforme `LOGIN_SETUP.md`.

## Verificação

- Build local completo: BUILD SUCCESSFUL, 1 min 17 s, 87 tarefas (17 executadas, 70 atualizadas).
- 34 testes unitários passaram; zero falhas/erros. Incluem cinco regressões de rotação de consultas, deduplicação, intercalação, priorização e fontes vazias. Seis testes live ficaram ignorados na execução normal.
- Lint: zero erros, nove avisos.
- Teste live adicional da descoberta mista: passou em 15 s. Respostas reais das três buscas trouxeram `[20, 16, 16]` itens, combinados em **52 IDs únicos** e múltiplos canais. Isso valida catálogo, não reprodução.
- CI do código 57f0a3b: build/lint e cinco testes instrumentados passaram, com quatro testes opcionais ignorados. Execução: https://github.com/ionatasr0/Projeto-5369/actions/runs/43778355688 .
- O diagnóstico live dessa execução falhou: erro IFrame 150 nos dois vídeos de teste e hosts oficiais. Não é sucesso de reprodução.
- A coleta inicial de screenshots falhou porque o app foi desinstalado pelo runner; a retenção do APK de teste foi habilitada em b735275. Conferência visual: https://github.com/ionatasr0/Projeto-5369/actions/runs/43778933252 . Build e navegação passaram novamente; ui-home.png e ui-watch.png foram recuperadas e inspecionadas. As capturas mostram carregamento inicial: confirmam disposição do cabeçalho, vídeo 16:9, ações e detalhes roláveis, não catálogo concluído nem reprodução. Não substituem revisão no M34.
- O diagnóstico de playback real continua disponível por workflow_dispatch, para evitar repetir tentativas com restrição externa a cada alteração de código.
- O erro 150 do player oficial observado anteriormente não foi contornado. Login real, reprodução no M34 e compatibilidade visual nesse aparelho não são declarados validados nesta entrega.

## APK

`artifacts/OndaTube-0.2.2-debug.apk`, versão 0.2.2 (4), pacote `dev.projeto5369`.

SHA-256: `bfbafbe2dc0f9be23d0573a5f1517b64a5e56b24b833598bdc6aaae9f31c2eb4`.

Assinatura verificada. Usa o mesmo certificado do APK 0.2.1 entregue nesta conversa (SHA-1 `7691fc64d3aca2369b8def9ce0e5804b1dfd7235`); a assinatura continua diferente do APK 0.2.0 original. Nenhuma Release publicada.
