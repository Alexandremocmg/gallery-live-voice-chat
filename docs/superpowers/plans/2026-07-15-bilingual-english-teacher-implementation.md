# Kabem Voice: Plano de Implementacao do Professor de Ingles Bilingue

**Data:** 2026-07-15  
**Status:** implementação principal concluída; validação em aparelho pendente
**Escopo:** fala e escuta em portugues e ingles, pratica de pronuncia e operacao local

## Status de implementação em 2026-07-18

O núcleo bilíngue foi implementado como infraestrutura compartilhada entre a conversa comum e o Professor de Inglês. A decisão de idioma não pertence mais apenas ao estado da aula: `ConversationLanguageCoordinator` e `LanguageTurnResolver` combinam comando explícito, metadados do reconhecedor, análise textual e idioma estabelecido na sessão.

Fluxo implementado:

```text
SpeechRecognizer / texto digitado
  -> RecognitionLanguagePolicy
  -> ConversationLanguageCoordinator
  -> LanguageTurnResolver
  -> LanguageInstructionBuilder
  -> Gemma (uma inferência)
  -> BilingualSpeechSegmenter
  -> TTS local por bloco validado
```

Também estão concluídos:

- reconhecimento dinâmico `pt-BR`, `en-US` e `en-GB`;
- troca automática no Android 14+ somente com os dois pacotes instalados;
- explicação em português e demonstração em inglês no mesmo streaming;
- bloqueio de fallback TTS entre idiomas e de vozes que exigem rede;
- persistência do idioma da conversa e restauração de sessões legadas;
- chip de idioma e avisos distintos para reconhecimento, voz local e troca automática;
- diagnóstico por metadados sem registrar áudio ou texto privado.

O Pacote Inglês Offline separado para aparelhos sem ASR local continua fora do escopo atual. Em Android 12 e 13, o aplicativo mantém um idioma de reconhecimento por turno. A matriz em aparelho e modo avião permanece pendente; nenhum dispositivo estava conectado ao ADB nesta revisão.

## 1. Objetivo

Permitir que o Kabem:

- reconheca corretamente quando o usuario fala ingles;
- pronuncie trechos ingleses com uma voz inglesa local;
- explique em portugues e demonstre em ingles na mesma resposta;
- controle automaticamente o idioma do proximo turno;
- conduza exercicios de escuta, repeticao e conversacao;
- ofereca feedback de pronuncia honesto usando transcricao, confianca e audio;
- continue funcionando sem enviar audio para servidores durante o uso.

## 2. Diagnostico atual

`VoiceChatManager` fixa `pt-BR` tanto no reconhecedor quanto na escolha de voz. O resultado usa apenas a primeira transcricao e descarta alternativas, confianca e tempos por palavra.

O reconhecedor generico criado com `SpeechRecognizer.createSpeechRecognizer` nao oferece garantia de processamento local. A arquitetura final deve resolver esse ponto para portugues e ingles, nao apenas trocar o idioma.

Recursos que o projeto ja possui e devem ser reutilizados:

- Gemma 3n com suporte a audio em modelos permitidos;
- `audioClips` em `runInference` e `Content.AudioBytes` no LiteRT-LM;
- gravacao PCM com `AudioRecord` e conversao WAV;
- TTS local com selecao por qualidade e latencia;
- streaming de resposta por sentenca;
- Professor Adaptativo e memoria confirmada;
- infraestrutura de download e gerenciamento de modelos.

## 3. Arquitetura recomendada

```text
Professor Adaptativo
  -> LanguageLessonOrchestrator
       -> idioma da explicacao
       -> idioma da demonstracao
       -> idioma esperado no proximo microfone
       -> tipo de atividade
  -> SpeechBackendResolver
       -> Android on-device ASR (preferencial)
       -> pacote ASR local (fallback completo)
       -> Gemma audio (analise de exercicio)
  -> BilingualResponseParser
       -> texto visivel sem marcadores
       -> blocos pt-BR / en-US / en-GB
  -> LocalTtsScheduler
       -> voz e velocidade corretas por bloco
```

### Regra de backend

1. Android 12+ com reconhecimento local disponivel: usar `createOnDeviceSpeechRecognizer`.
2. Android 13+: consultar idiomas instalados com `checkRecognitionSupport` e permitir baixar o pacote antes do uso.
3. Android 8-11 ou aparelho sem reconhecimento local: usar o Pacote Ingles Offline quando instalado.
4. Exercicio com modelo Gemma compativel com audio: gravar WAV e enviar o audio na mesma inferencia que gera o feedback.
5. Sem backend local disponivel: nao cair silenciosamente em reconhecimento de rede; oferecer texto ou instalacao do pacote offline.

## 4. Etapa A: Modelos de idioma e testes de base

**Criar:**

- `voice/language/SpeechLocale.kt`
- `voice/language/SpeechCapability.kt`
- `voice/language/SpeechRecognitionResult.kt`
- `voice/language/EnglishLessonModels.kt`
- testes correspondentes em `app/src/test`.

**Modelos principais:**

```kotlin
enum class SpeechLocale(val tag: String) {
  PT_BR("pt-BR"),
  EN_US("en-US"),
  EN_GB("en-GB"),
}

enum class EnglishActivity {
  NONE, EXPLANATION, LISTENING, REPEAT, FREE_CONVERSATION, PRONUNCIATION_FEEDBACK
}

enum class RecognitionBackend {
  ANDROID_ON_DEVICE, LOCAL_LANGUAGE_PACK, GEMMA_AUDIO, UNAVAILABLE
}
```

`EnglishLessonState` deve guardar:

- dialeto preferido;
- atividade atual;
- frase esperada;
- idioma do proximo turno;
- velocidade de demonstracao;
- quantidade de tentativas;
- ultimo resultado de inteligibilidade;
- se audio bruto pode ser analisado pelo modelo ativo.

**Testes:**

- estado normal com entrada `pt-BR`;
- repeticao define proxima entrada `en-US`;
- conversa livre mantem entrada inglesa;
- encerramento volta ao portugues;
- preferencia `en-GB` e respeitada sem afetar o restante do professor.

## 5. Etapa B: Reconhecimento local dinamico

**Criar:**

- `voice/domain/SpeechRecognizerController.kt`
- `voice/domain/SpeechCapabilityResolver.kt`

**Modificar:**

- `voice/domain/VoiceChatManager.kt`
- `voice/presentation/VoiceViewModel.kt`

**Implementar:**

- remover o idioma fixo de `startListening`;
- receber `SpeechLocale` em cada inicio de escuta;
- API 31+: preferir `SpeechRecognizer.createOnDeviceSpeechRecognizer`;
- API 33+: consultar suporte e estado do pacote do idioma;
- API 33+: acionar download somente por comando explicito do usuario;
- definir `EXTRA_LANGUAGE` com tag BCP 47;
- definir `EXTRA_PREFER_OFFLINE = true` como reforco, nunca como unica garantia;
- solicitar ate cinco alternativas;
- capturar `CONFIDENCE_SCORES` quando fornecido;
- API 34+: solicitar confianca e tempo por palavra;
- API 34+: permitir troca automatica apenas na conversa bilingue livre;
- em repeticao e avaliacao, forcar ingles e desativar troca automatica;
- manter recriacao segura apos `ERROR_RECOGNIZER_BUSY`;
- distinguir idioma nao suportado, nao instalado e backend indisponivel.

**Privacidade:**

- o modo offline estrito nunca usa `createSpeechRecognizer` como fallback silencioso;
- o estado da UI informa `Ingles offline pronto`, `Pacote necessario` ou `Indisponivel`;
- nenhum audio e persistido fora de uma atividade de pronuncia confirmada;
- gravacoes temporarias sao removidas depois da inferencia.

**Testes:**

- matriz de API 26, 31, 33 e 34;
- backend local disponivel e indisponivel;
- pacote instalado, pendente e ausente;
- repeticao sempre usa ingles;
- conversa normal continua em portugues;
- erro de rede nunca aparece quando o backend local foi selecionado.

## 6. Etapa C: Voz local bilingue

**Criar:**

- `voice/domain/LocalVoiceCatalog.kt`
- `voice/domain/LocalTtsScheduler.kt`
- `voice/language/SpeechChunk.kt`

**Modificar:**

- `voice/domain/VoiceChatManager.kt`
- `data/TtsVoiceMode.kt`

**Implementar:**

- catalogar vozes locais para `pt-BR`, `en-US` e `en-GB`;
- excluir qualquer voz com `isNetworkConnectionRequired = true`;
- ordenar por pais exato, qualidade e menor latencia;
- manter cache por idioma;
- `speak` passa a receber `SpeechChunk(text, locale, rate)`;
- serializar a fila: selecionar a voz somente quando o bloco anterior terminar;
- preservar `QUEUE_FLUSH` apenas para o primeiro bloco de uma resposta;
- manter velocidade portuguesa conforme preferencia atual;
- ingles natural em `0.90-0.93`;
- demonstracao lenta em `0.78-0.84`;
- ao faltar voz inglesa local, nao usar voz portuguesa; informar que o pacote de voz precisa ser instalado.

**Testes:**

- selecao da melhor voz em cada idioma;
- voz de rede nunca e escolhida;
- `en-US` nao cai em `pt-BR`;
- fila alterna portugues, ingles e portugues sem trocar a voz antes da hora;
- parada e interrupcao limpam fila e contadores.

## 7. Etapa D: Parser de resposta bilingue em streaming

**Criar:**

- `voice/language/BilingualResponseParser.kt`
- `voice/language/LanguageTaggedPromptBuilder.kt`

**Formato interno:**

```text
[[pt-BR]]A expressao significa como voce esta.
[[en-US]]How are you?
[[pt-BR]]Agora tente repetir.
```

**Implementar:**

- o prompt exige marcadores somente quando houver mais de um idioma;
- os marcadores nunca aparecem na resposta visivel ou no historico;
- o parser aceita marcadores quebrados entre tokens de streaming;
- texto portugues vira `SpeechChunk(PT_BR)`;
- texto ingles vira `SpeechChunk(EN_US/EN_GB)`;
- cada bloco continua sendo drenado por sentenca;
- marcador invalido tem fallback limitado e nunca e falado;
- manter separadamente `rawModelText`, `displayText` e fila de fala;
- resposta sem marcadores usa o idioma base do turno.

**Testes criticos:**

- dividir a mesma resposta em todos os possiveis pontos entre caracteres;
- marcador incompleto no fim;
- resposta inteiramente inglesa;
- explicacao portuguesa com varias frases inglesas;
- pontuacao, contracoes e apostrofos ingleses;
- nenhum marcador salvo em sessao ou enviado ao TTS.

## 8. Etapa E: Orquestrador do Professor de Ingles

**Criar:**

- `voice/language/EnglishLessonDetector.kt`
- `voice/language/EnglishLessonOrchestrator.kt`
- `voice/language/EnglishTeachingPromptBuilder.kt`

**Integrar com:**

- `TeachingOrchestrator`;
- `TeachingPromptBuilder`;
- `VoiceViewModel`;
- memoria confirmada.

**Intencoes:**

- `quero estudar ingles` -> explicacao bilingue;
- `vamos conversar em ingles` -> conversa livre em ingles;
- `como se pronuncia` -> demonstracao inglesa;
- `fale mais devagar` -> repeticao lenta;
- `quero repetir` -> proximo microfone em ingles;
- `corrija minha pronuncia` -> gravacao e avaliacao;
- `volte para portugues` -> encerra modo ingles.

**Comportamento:**

- explicacoes permanecem em portugues por padrao;
- exemplos e frases-alvo usam voz inglesa;
- apenas uma dificuldade por bloco;
- depois de demonstrar, o Kabem pergunta se o usuario quer ouvir novamente ou repetir;
- em repeticao, o proximo turno e ingles automaticamente;
- dialeto padrao `en-US`, com `en-GB` nas preferencias;
- preferencias pedagogicas confirmadas continuam valendo.

## 9. Etapa F: Pratica com audio no Gemma 3n

**Extrair e reutilizar:**

- logica de `AudioRecord` de `AudioRecorderPanel`;
- `AudioClip` e geracao WAV;
- `audioClips` do runtime existente.

**Criar:**

- `voice/domain/VoiceAudioRecorder.kt`
- `voice/language/PronunciationAttempt.kt`
- `voice/language/IntelligibilityAnalyzer.kt`

**Fluxo:**

1. Kabem demonstra a frase com voz inglesa.
2. Estado muda para `REPEAT`.
3. Microfone grava PCM/WAV em vez de abrir o reconhecedor comum.
4. Avaliador local compara frase esperada com transcricao e alternativas, quando disponiveis.
5. Se o modelo ativo aceita audio, a mesma inferencia recebe WAV, frase esperada e dados locais.
6. Gemma produz feedback curto em portugues e, quando util, nova demonstracao em ingles.
7. Audio temporario e descartado.

**Rubrica inicial:**

- frase compreensivel ou nao;
- palavras omitidas, adicionadas ou substituidas;
- pausas longas e ritmo aproximado;
- uma orientacao pratica por tentativa;
- reconhecer primeiro o que ficou claro;
- nunca inventar fonemas ou afirmar precisao inexistente;
- nao mostrar porcentagem de pronuncia no MVP.

**Fallback sem audio no modelo:**

- comparar texto esperado com ate cinco alternativas de ASR;
- usar alinhamento de palavras e confianca;
- informar que o feedback avalia inteligibilidade, nao sotaque detalhado.

## 10. Etapa G: Pacote Ingles Offline

Esta etapa garante cobertura consistente em Android 8-11 e aparelhos sem ASR local do sistema.

**Tecnologia candidata:** Sherpa-ONNX para Android/Kotlin.

**Antes de escolher o modelo:**

- testar pelo menos um modelo ingles pequeno nao streaming e um streaming;
- medir tamanho, memoria, tempo para primeiro resultado e consumo de bateria;
- validar licenca do modelo, nao apenas da biblioteca;
- testar em aparelho basico, intermediario e topo de linha;
- escolher entre modelo embarcado e download opcional.

**Integracao:**

- pacote separado do APK principal;
- download explicito com tamanho informado;
- checksum e versao no allowlist;
- status, atualizacao e remocao nas preferencias;
- VAD local para encerrar fala sem silencio excessivo;
- runtime sem rede depois da instalacao.

O Pacote Ingles deve implementar a mesma interface `SpeechRecognitionBackend`, permitindo troca sem alterar ViewModel ou UI.

## 11. Etapa H: Interface

**Live Voice:**

- indicador discreto `PT`, `EN` ou `Repeticao` perto do estado de voz;
- sem seletor permanente na tela principal;
- durante exercicio, comandos com icones: ouvir, ouvir devagar, repetir e parar;
- texto reconhecido mostra o idioma usado;
- falha de pacote oferece uma acao clara, sem erro tecnico.

**Preferencias:**

- idioma automatico ativado por padrao;
- dialeto: `Ingles americano` ou `Ingles britanico`;
- voz: `Natural` ou `Economica`, preservando a configuracao atual;
- status da voz inglesa local;
- status do reconhecimento ingles offline;
- gerenciar Pacote Ingles Offline.

## 12. Persistencia e memoria

- dialeto e preferencia explicita podem ser persistidos;
- frase praticada, audio e dificuldade nao sao memorias permanentes;
- audio nunca entra no backup;
- historico salva apenas texto limpo e metadados de idioma;
- preferencias como `aprendo ingles melhor repetindo` usam confirmacao existente;
- nao persistir avaliacao de nivel ou suposta qualidade do sotaque.

Uma alteracao de protobuf so deve ocorrer quando os metadados de idioma forem necessarios para restaurar uma sessao. Adicionar campos novos, sem renumerar os existentes.

## 13. Testes de integracao e aparelho

**Automatizados:**

- todos os testes atuais continuam passando;
- parser bilingue com streaming fragmentado;
- selecao de backend por API e capacidade;
- maquina de estados do professor de ingles;
- selecao e fila de vozes;
- comparacao de frase esperada e alternativas;
- cancelamento, erro e troca de sessao nao deixam idioma preso.

**No aparelho, em modo aviao:**

1. Pergunta comum em portugues.
2. `Quero estudar ingles do zero`.
3. Demonstracao de `How are you?` com voz inglesa.
4. Explicacao seguinte com voz portuguesa.
5. Repeticao reconhecida em ingles.
6. `Nao entendi` explicado em portugues.
7. `Fale mais devagar` com voz inglesa lenta.
8. Conversa livre de cinco turnos em ingles.
9. Feedback de pronuncia com Gemma audio.
10. Interrupcao durante TTS e durante gravacao.
11. Reinicio de sessao volta ao idioma correto.
12. Mesmo fluxo sem pacote ingles para validar mensagens de fallback.

**Frases de regressao:**

- `three` / `thirteen`;
- `ship` / `sheep`;
- `can` / `can't`;
- `world`;
- `comfortable`;
- `I would like a cup of coffee`;
- frases misturadas como `Hoje vamos praticar How are you?`.

## 14. Criterios de aceite

- trecho ingles nunca e sintetizado com voz `pt-BR`;
- pratica inglesa nunca e reconhecida com modelo portugues;
- explicacao bilingue alterna vozes sem falar marcadores;
- primeira frase continua chegando ao TTS por streaming;
- nao existe chamada adicional obrigatoria ao LLM em conversa comum;
- exercicio com audio usa uma unica inferencia para analisar e responder;
- modo offline estrito nao cai silenciosamente em reconhecimento de rede;
- API 26-30 funciona com Pacote Ingles instalado;
- audio temporario e removido depois do turno;
- nenhuma nota fonetica falsa e exibida;
- imagens, PDF, sessoes, memoria, camera e voz atual continuam funcionando;
- testes JVM passam e `assembleDebug` conclui.

## 15. Ordem de entrega recomendada

1. Etapas A, B e C: infraestrutura de idioma, STT local e vozes dinamicas.
2. Etapa D: streaming bilingue seguro.
3. Etapa E: Professor de Ingles e controle do proximo turno.
4. Etapa F: gravacao e feedback com Gemma 3n.
5. Etapa H: interface e preferencias.
6. Validacao completa em Android 12+.
7. Etapa G: Pacote Ingles para cobertura Android 8-11 e aparelhos sem ASR local.
8. Benchmark, testes em modo aviao e APK final.

## 16. Pontos de controle

### Entrega 1: fala bilingue correta

O Kabem entende e fala ingles com os recursos locais instalados, sem avaliacao de pronuncia.

### Entrega 2: professor de ingles

O Kabem conduz escuta, repeticao e conversa com alternancia automatica de idioma.

### Entrega 3: feedback de pronuncia

O Kabem usa audio no Gemma e dados de reconhecimento para feedback de inteligibilidade.

### Entrega 4: cobertura offline completa

Pacote ASR local cobre aparelhos sem reconhecimento on-device adequado.

## Referencias tecnicas

- Android `SpeechRecognizer`: https://developer.android.com/reference/android/speech/SpeechRecognizer
- Android `RecognizerIntent`: https://developer.android.com/reference/android/speech/RecognizerIntent
- Android `RecognitionSupport`: https://developer.android.com/reference/android/speech/RecognitionSupport
- Android TTS `Voice`: https://developer.android.com/reference/android/speech/tts/Voice
- Gemma 3n audio: https://ai.google.dev/gemma/docs/gemma-3n
- LiteRT-LM multimodal Kotlin: https://github.com/google-ai-edge/LiteRT-LM/blob/main/docs/api/kotlin/getting_started.md
- Sherpa-ONNX Android: https://k2-fsa.github.io/sherpa/onnx/android/prebuilt-apk.html
