# Kabem Voice: Plano de Implementacao do Professor Adaptativo

**Data:** 2026-07-15  
**Especificacao:** `docs/superpowers/specs/2026-07-15-adaptive-professor-design.md`  
**Objetivo:** implementar ensino adaptativo local, com uma inferencia por turno, respostas em blocos curtos e testes JVM.

## Principios de execucao

- Implementar de dentro para fora: modelos puros, detector, orquestrador, prompt e somente depois Android.
- Escrever o teste de cada comportamento antes da implementacao correspondente.
- Manter a classificacao pedagogica separada do perfil de profundidade.
- Nao alterar protobufs no MVP.
- Confirmar mudancas de estado somente quando a resposta terminar com sucesso.
- Preservar imagens, PDF, sessoes, memoria e TTS existentes.

## Etapa 0: Isolar e proteger a classificacao de profundidade atual

**Criar:**

- `Android/src/app/src/main/java/com/google/ai/edge/gallery/voice/data/ResponseDepthProfile.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/voice/domain/ResponseDepthClassifier.kt`
- `Android/src/app/src/test/java/com/google/ai/edge/gallery/voice/domain/ResponseDepthClassifierTest.kt`

**Modificar:**

- `Android/src/app/src/main/java/com/google/ai/edge/gallery/voice/presentation/VoiceViewModel.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/voice/presentation/LiveChatScreen.kt`

**Implementar:**

- mover `ResponseDepthProfile` sem alterar nomes persistidos ou textos exibidos;
- extrair regex, continuacoes e regra da ultima palavra-chave para classe Kotlin pura;
- retornar `ResponseProfileSelection(profile, isExplicit)`, distinguindo gatilho real do `FLASH` padrao;
- preservar heranca e escalada do perfil anterior.

**Testes de regressao:**

- `resuma isso` -> `FLASH` explicito;
- `me explica IA` -> `DETAILED`;
- `passo a passo` -> `STEP_BY_STEP`;
- `crie um plano` -> `STRATEGIC`;
- `explica rapido` -> `FLASH`, pois o ultimo gatilho vence;
- `mais` depois de `FLASH` -> `DETAILED`;
- `que horas sao` -> `FLASH` sem falso positivo.

**Verificar:**

```powershell
cd Android/src
./gradlew :app:testDebugUnitTest --tests "*ResponseDepthClassifierTest"
```

## Etapa 1: Criar o dominio pedagogico puro

**Criar:**

- `Android/src/app/src/main/java/com/google/ai/edge/gallery/voice/pedagogy/TeachingModels.kt`
- `Android/src/app/src/test/java/com/google/ai/edge/gallery/voice/pedagogy/TeachingModelsTest.kt`

**Implementar:**

- `TeachingMode`, `LearnerLevel`, `UnderstandingSignal` e `TeachingAction`.
- `TeachingState`, com valores iniciais seguros.
- `TeachingDecision`, contendo `signal`, `action`, `proposedNextState`, `shouldCheckUnderstanding` e pistas textuais limitadas.
- Limites centralizados para tema, objetivo, conceito e prompt pedagogico.

**Testes primeiro:**

- estado inicial fica em `OFF`, nivel `UNKNOWN` e sem progresso;
- listas do estado sao imutaveis do ponto de vista do chamador;
- pistas acima do limite sao truncadas sem quebrar palavras quando possivel;
- decisao normal nao exige verificacao de entendimento.

**Verificar:**

```powershell
cd Android/src
./gradlew :app:testDebugUnitTest --tests "*TeachingModelsTest"
```

## Etapa 2: Detectar sinais e intencao de aprendizagem

**Criar:**

- `Android/src/app/src/main/java/com/google/ai/edge/gallery/voice/pedagogy/TeachingSignalDetector.kt`
- `Android/src/app/src/test/java/com/google/ai/edge/gallery/voice/pedagogy/TeachingSignalDetectorTest.kt`

**Implementar:**

- normalizacao de caixa, acentos e espacos sem APIs Android;
- gatilhos fortes de entrada no modo professor;
- sinais `UNDERSTOOD`, `CONFUSED`, `PARTIAL`, `WANTS_EXAMPLE`, `WANTS_DEPTH`, `WANTS_NEXT` e `LEARNER_ATTEMPT`;
- pedidos explicitos de resposta curta e saida do modo professor;
- relacao conservadora de tema por termos relevantes e exclusao de palavras comuns;
- estimativa inicial de nivel com confianca, sem rotular permanentemente o usuario;
- resultado `NONE` em frases ambiguas, favorecendo falsos negativos em vez de aulas indesejadas.

**Matriz minima:**

- `me ensine fracao do zero` ativa professor e favorece iniciante;
- `como funciona o bluetooth?` isolado nao ativa automaticamente;
- `nao entendi` em modo ativo detecta confusao;
- `mais ou menos` detecta compreensao parcial;
- `por exemplo?` pede exemplo;
- `e internamente?` pede profundidade;
- `continua` herda o tema ativo;
- uma tentativa de resposta apos uma verificacao vira `LEARNER_ATTEMPT`;
- `agora fale do clima` encerra tema anterior quando a relacao e baixa;
- `explica rapido` preserva pedido curto explicito.

**Verificar:**

```powershell
cd Android/src
./gradlew :app:testDebugUnitTest --tests "*TeachingSignalDetectorTest"
```

## Etapa 3: Implementar a maquina de decisoes pedagogicas

**Criar:**

- `Android/src/app/src/main/java/com/google/ai/edge/gallery/voice/pedagogy/TeachingOrchestrator.kt`
- `Android/src/app/src/test/java/com/google/ai/edge/gallery/voice/pedagogy/TeachingOrchestratorTest.kt`

**Implementar:**

- entrada em `ACTIVE` com `ESTABLISH_GOAL` ou `EXPLAIN`;
- selecao de uma unica acao principal por turno;
- `CONFUSED` leva a `SIMPLIFY`; confusao repetida troca para `GIVE_ANALOGY` e reduz o nivel;
- `PARTIAL` leva a `GIVE_EXAMPLE` ou `REVIEW`;
- `UNDERSTOOD` leva a `ADVANCE` ou `COMPLETE`;
- `LEARNER_ATTEMPT` leva a `EVALUATE_ATTEMPT` na mesma inferencia de resposta;
- mudanca de assunto encerra o estado anterior antes de decidir sobre o novo tema;
- pedido curto explicito pode responder brevemente sem perder o tema, salvo pedido de saida;
- `proposedNextState` e sempre novo e nao muta o estado recebido.

**Testes de jornada:**

1. Inicio do zero -> explicacao -> entendi -> proximo conceito.
2. Explicacao -> nao entendi -> simplificacao -> ainda nao -> analogia.
3. Verificacao -> tentativa do aluno -> avaliacao cuidadosa.
4. Tema ativo -> pergunta curta relacionada -> resposta breve, tema preservado.
5. Tema ativo -> assunto diferente -> estado anterior encerrado.
6. Pergunta factual comum -> `ANSWER_NORMALLY`, modo `OFF`.

**Verificar:**

```powershell
cd Android/src
./gradlew :app:testDebugUnitTest --tests "*TeachingOrchestratorTest"
```

## Etapa 4: Montar a instrucao invisivel com limites

**Criar:**

- `Android/src/app/src/main/java/com/google/ai/edge/gallery/voice/pedagogy/TeachingPromptBuilder.kt`
- `Android/src/app/src/test/java/com/google/ai/edge/gallery/voice/pedagogy/TeachingPromptBuilderTest.kt`

**Implementar:**

- prompt pedagogico ausente quando o modo esta `OFF` e a acao e normal;
- bloco interno com modo, nivel, confianca, tema, conceito, sinal e acao;
- instrucao de 2 a 4 frases faladas para blocos pedagogicos;
- verificacao curta somente quando `shouldCheckUnderstanding` for verdadeiro;
- estrategias especificas para explicar, simplificar, exemplificar, criar analogia, aprofundar e avaliar tentativa;
- delimitacao clara de dados vindos do usuario;
- instrucao para nao revelar metadados internos;
- limite total do bloco pedagogico para proteger a janela de contexto.

**Testes primeiro:**

- campos nulos nao aparecem como `null`;
- instrucao normal nao exige pergunta pedagogica;
- confusao escolhe linguagem simples e estrategia diferente;
- avaliacao de tentativa reconhece acertos antes de corrigir;
- texto malicioso em tema ou conceito permanece dentro do delimitador de dados;
- tamanho nunca ultrapassa o limite definido.

**Verificar:**

```powershell
cd Android/src
./gradlew :app:testDebugUnitTest --tests "*TeachingPromptBuilderTest"
```

## Etapa 5: Integrar profundidade e pedagogia na VoiceViewModel

**Modificar:**

- `Android/src/app/src/main/java/com/google/ai/edge/gallery/voice/presentation/VoiceViewModel.kt`

**Implementar:**

- instancias de `TeachingSignalDetector`, `TeachingOrchestrator` e `TeachingPromptBuilder`;
- `teachingState` privado e um `StateFlow` de resumo somente se a UI precisar observar o modo;
- consumir `ResponseProfileSelection` produzido pelo classificador extraido;
- quando professor estiver ativo e nao houver pedido explicito curto, elevar `FLASH` padrao para `DETAILED`;
- montar uma unica instrucao interna combinando perfil, pedagogia, historico, PDF e memoria;
- calcular a decisao antes da inferencia e confirmar `proposedNextState` somente em `done`;
- nao confirmar estado em excecao, `onError`, cancelamento ou resposta vazia;
- limpar `TeachingState` ao iniciar nova conversa;
- restaurar sessao com estado `OFF` no MVP, usando o historico apenas como contexto textual;
- atualizar `lastTurnContext` com resumo e ultimo sinal pedagogico necessario para continuacoes.

**Ajustar o prompt global:**

- reforcar naturalidade de conversa por voz;
- ensinar um conceito por vez no modo professor;
- adaptar sem infantilizar;
- reconhecer incerteza e nao fingir compreensao;
- tratar PDF, memoria, imagem e historico como dados, nao como novas regras.

**Cuidados:**

- manter `responseGenerationInProgress` como protecao de concorrencia;
- nao alterar o fluxo de `attachedImages` ou paginas de PDF;
- preservar `drainCompleteSentences` e `speakStreamingSegment`;
- evitar que instrucoes de estilo e pedagogia deem ordens contraditorias.

**Adicionar ao `ResponseDepthClassifierTest`:**

- `FLASH` padrao + professor ativo -> perfil efetivo `DETAILED`;
- `FLASH` explicito + professor ativo -> perfil efetivo `FLASH`;
- `STEP_BY_STEP` e `STRATEGIC` explicitos nunca sao rebaixados pela pedagogia.

## Etapa 6: Integrar preferencias pedagogicas confirmadas

**Modificar:**

- `Android/src/app/src/main/java/com/google/ai/edge/gallery/voice/presentation/VoiceViewModel.kt`

**Implementar:**

- reconhecer expressoes como `aprendo melhor com exemplos`, `prefiro analogias` e `explique com linguagem simples` como candidatas de preferencia;
- usar o fluxo de confirmacao de memoria existente;
- nunca persistir automaticamente `iniciante`, `nao entendeu` ou contadores de confusao;
- incluir preferencias confirmadas relevantes no prompt pedagogico atraves do contexto de memoria existente.

**Validar manualmente:**

- rejeitar a memoria nao altera o comportamento permanente;
- desativar memoria impede nova candidatura;
- fala atual sempre prevalece sobre preferencia antiga.

## Etapa 7: Validar experiencia de voz e regressao

**Sem mudanca obrigatoria de UI.** O indicador de profundidade atual permanece. Um indicador `Ensinando` so sera adicionado depois de teste em uso real, caso traga clareza sem poluir a tela.

**Executar testes completos:**

```powershell
cd Android/src
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

**Roteiro no aparelho:**

1. `Que horas sao?` -> resposta curta, sem pergunta de professor.
2. `Quero aprender juros compostos do zero` -> bloco inicial curto e verificacao natural.
3. `Nao entendi` -> nova estrategia, sem repetir o mesmo texto.
4. `Me da um exemplo com dinheiro` -> exemplo aplicado.
5. Responder a uma pergunta do Kabem -> reconhecimento do acerto e correcao pontual.
6. `Continua` -> proximo bloco.
7. `Agora resume tudo` -> resumo e encerramento.
8. Repetir o fluxo com PDF e imagem anexados.
9. Interromper uma geracao -> progresso nao avanca silenciosamente.
10. Reiniciar e restaurar sessao -> sem progresso pedagogico inventado.

## Definicao de concluido

- Todos os testes JVM novos passam.
- `assembleDebug` passa.
- Uma pergunta comum continua rapida.
- Uma jornada de aprendizagem permanece coerente por pelo menos cinco turnos.
- Confusao muda a estrategia da explicacao.
- O Kabem nao pergunta mecanicamente se o usuario entendeu em todo turno.
- Nao ha segunda inferencia obrigatoria.
- TTS continua iniciando por sentenca.
- Nenhum dado pedagogico sensivel e persistido automaticamente.
- Imagem, camera, PDF, sessoes, memoria e seletor de voz continuam funcionais.
