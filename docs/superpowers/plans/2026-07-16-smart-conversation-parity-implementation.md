# Kabem Voice Smart Conversation Parity Implementation Plan

> **For Hermes:** Use the `subagent-driven-development` skill to implement this plan task-by-task, with a specification review and a code-quality review after each task.

**Goal:** Unificar a experiência de conversa textual e por voz para que o Kabem Voice ofereça entrada por teclado, revisão/edição, seleção, cópia, regeneração, compartilhamento e reprodução de respostas de forma contextual, sem deixar a interface permanentemente carregada.

**Architecture:** A conversa terá uma linha do tempo compartilhada entre o `ChatPanel` e o `LiveChatScreen`. Voz e teclado serão apenas modos de entrada diferentes. As ações de cada mensagem serão exibidas por toque longo ou menu contextual, enquanto a tela inicial continuará priorizando o botão de voz. O histórico persistido será a fonte única de verdade; `recognizedText` e `lastResponse` deixarão de ser a única representação visual da conversa.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, `StateFlow`, DataStore/Protobuf, LiteRT-LM, Android `ClipboardManager`, Android Sharesheet, testes unitários Kotlin e testes de UI Compose.

---

## Status da implementação — 16 de julho de 2026

**Estado:** implementação principal concluída na branch `feat/live-voice-chat`, com compilação, testes unitários e APK debug validados localmente.

### Entregue

- linha do tempo persistida para mensagens de voz e teclado;
- envio automático de transcrições normais e revisão manual apenas quando a confiança é baixa;
- IDs persistentes de mensagem, origem `VOICE`/`TEXT` e migração determinística de sessões legadas;
- ações contextuais centralizadas por policy: copiar, compartilhar, editar/reenviar, regenerar e ouvir novamente quando disponíveis;
- proteção transacional de edição/regeneração por sessão, revisão e geração, incluindo rollback em falha assíncrona;
- persistência versionada para impedir snapshots antigos de sobrescreverem revisões novas;
- Markdown persistido nas respostas do assistente;
- auto-scroll que respeita leitura manual, Snackbar, suporte a IME e contraste Material 3;
- detecção conservadora de turnos multimodais para impedir reescrita quando imagem, áudio ou PDF não podem ser reconstruídos com segurança.

### Limites intencionais

- edição e regeneração ficam indisponíveis em turnos multimodais cujo payload não possa ser reconstruído integralmente;
- `SELECT` e `SAVE_MEMORY` permanecem no enum de domínio, mas não são anunciados pela policy até possuírem uma ação de UI completa;
- houve reutilização do `MessageBodyText`, porém a migração integral do cartão e das ações do `ChatPanel` continua como trabalho posterior;
- a matriz manual em aparelho Android real e testes instrumentados Compose ainda precisam ser executados antes de declarar paridade total de release.

### Verificação executada

```bash
cd Android/src
./gradlew :app:testDebugUnitTest --no-daemon --rerun-tasks
./gradlew :app:assembleDebug --no-daemon
```

Resultados observados:

- `testDebugUnitTest`: `BUILD SUCCESSFUL`, 46 tarefas executadas;
- `assembleDebug`: `BUILD SUCCESSFUL`;
- APK: `Android/src/app/build/outputs/apk/debug/app-debug.apk`;
- `git diff --check`: sem erros.

---

## Contexto confirmado no código

- `ChatPanel.kt` já possui entrada textual, histórico, anexos, copiar resposta, seleção de texto, `run again`, benchmark e templates.
- `MessageBodyText.kt` já usa `SelectionContainer` para respostas não-Markdown e `LongPressCopyContainer` para mensagens do usuário.
- `LiveChatScreen.kt` atualmente mostra principalmente `recognizedText` e `lastResponse`; possui microfone, câmera, galeria, áudio, PDF, sessões e memória, mas não possui a mesma lista de mensagens nem ações textuais.
- `VoiceViewModel.kt` já mantém `sessionMessages`, persiste sessões e possui operações de nova sessão/interrupção, portanto a maior lacuna é a representação compartilhada na UI e o contrato das ações.
- O modo de voz deve preservar a experiência hands-free: ações avançadas precisam ser contextuais e não substituir o botão principal de falar.

## Decisões de produto

1. **Modelo mental:** o usuário interage com uma conversa, não com duas telas independentes.
2. **Entrada padrão:** voz continua sendo a ação primária no Kabem Voice.
3. **Entrada alternativa:** teclado aparece por botão contextual, com opção de torná-lo o modo padrão.
4. **Primeiras ações visíveis:** `Copiar`, `Editar` e `Mais`.
5. **Conversa contínua:** o resultado final do reconhecimento é enviado automaticamente; a revisão não interrompe o fluxo normal.
6. **Ações em `Mais`:** regenerar, compartilhar, ouvir novamente, salvar na memória e remover/ramificar.
7. **Edição de mensagem:** ao editar uma mensagem do usuário, as respostas posteriores devem ser invalidadas ou tratadas como uma nova ramificação; nunca manter uma resposta antiga como se ainda correspondesse à pergunta editada.
8. **Download/recursos:** esta entrega não deve iniciar downloads automáticos de modelos; o planner de capacidades será integrado em uma etapa separada.
9. **Privacidade:** copiar e compartilhar são ações locais explícitas; nenhum texto deve sair do aparelho sem uma ação deliberada do usuário.

---

## Critérios de aceitação gerais

- O usuário consegue iniciar uma conversa por voz como hoje, sem abrir o teclado.
- O usuário consegue alternar para teclado e enviar uma mensagem textual no mesmo histórico.
- O usuário consegue revisar uma transcrição quando o reconhecimento indicar baixa confiança.
- Mensagens do usuário e do assistente aparecem em uma linha do tempo rolável e persistente.
- Uma resposta finalizada pode ser copiada, selecionada e compartilhada.
- Uma mensagem do usuário pode ser editada e reenviada sem deixar respostas incompatíveis no histórico ativo.
- Uma resposta finalizada pode ser regenerada e ouvida novamente.
- Durante geração ou gravação, somente ações seguras ficam habilitadas (`Parar`, cancelar ou descartar).
- A interface inicial não mostra todos os controles ao mesmo tempo.
- O modo offline não dispara compartilhamento, download ou rede implicitamente.

---

## Fase 0 — Preparação e contratos

### Task 0.1: Criar o modelo de mensagem visual compartilhado

**Objetivo:** Definir um modelo estável para a UI representar mensagens de voz e texto sem duplicar o estado existente do runtime.

**Arquivos:**
- Criar: `Android/src/app/src/main/java/com/google/ai/edge/gallery/voice/conversation/ConversationUiMessage.kt`
- Consultar: `Android/src/app/src/main/java/com/google/ai/edge/gallery/voice/presentation/VoiceViewModel.kt`
- Consultar: `Android/src/app/src/main/proto/*.proto`
- Testar: `Android/src/app/src/test/java/com/google/ai/edge/gallery/voice/conversation/ConversationUiMessageTest.kt`

**Implementação:**

Criar tipos para:

```kotlin
enum class ConversationMessageSide { USER, ASSISTANT, SYSTEM }

enum class ConversationMessageStatus { COMPLETE, STREAMING, ERROR, CANCELLED }

data class ConversationUiMessage(
    val id: String,
    val side: ConversationMessageSide,
    val text: String,
    val status: ConversationMessageStatus,
    val createdAtMs: Long,
    val source: MessageSource,
    val canEdit: Boolean,
)
```

Não duplicar o texto em múltiplos estados mutáveis sem necessidade. Definir como o ID persistido do Protobuf será convertido para o ID da UI; quando não existir ID, usar um ID determinístico/gerado no momento de persistência.

**Testes:**

- Converter mensagem de usuário e assistente corretamente.
- Preservar mensagem em streaming como `STREAMING`.
- Não permitir edição de mensagem do assistente.
- Preservar ordenação por `createdAtMs`/ordem persistida.

**Verificação:**

```bash
cd Android/src
./gradlew :app:testDebugUnitTest --tests '*ConversationUiMessageTest' --no-daemon
```

---

### Task 0.2: Definir o contrato de ações de mensagem

**Objetivo:** Centralizar as ações disponíveis e as regras que habilitam/desabilitam cada ação.

**Arquivos:**
- Criar: `Android/src/app/src/main/java/com/google/ai/edge/gallery/voice/conversation/ConversationMessageAction.kt`
- Criar: `Android/src/app/src/main/java/com/google/ai/edge/gallery/voice/conversation/ConversationMessageActionPolicy.kt`
- Testar: `Android/src/app/src/test/java/com/google/ai/edge/gallery/voice/conversation/ConversationMessageActionPolicyTest.kt`

**Contrato:**

```kotlin
enum class ConversationMessageAction {
    COPY,
    SELECT,
    EDIT_AND_RESEND,
    REGENERATE,
    SHARE,
    SPEAK_AGAIN,
    SAVE_MEMORY,
}
```

A política deve considerar:

- lado da mensagem;
- status (`COMPLETE` ou `STREAMING`);
- se existe modelo TTS funcional;
- se a edição está em andamento;
- se existe texto não vazio;
- se a geração está ativa.

**Regras mínimas:**

- `EDIT_AND_RESEND`: somente usuário, mensagem completa, texto não vazio.
- `REGENERATE`: somente assistente, mensagem completa e com uma pergunta de usuário associada.
- `SPEAK_AGAIN`: somente assistente, mensagem completa e TTS disponível.
- `COPY`, `SELECT` e `SHARE`: somente texto completo e não vazio.
- Nenhuma ação de conteúdo durante `STREAMING`, exceto `Parar` em um controlador separado.

---

## Fase 1 — Linha do tempo compartilhada

### Task 1.1: Extrair o cartão de mensagem reutilizável

**Objetivo:** Reutilizar a renderização textual existente sem copiar a lógica do `ChatPanel` para a tela de voz.

**Arquivos:**
- Criar: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/common/conversation/ConversationMessageCard.kt`
- Reutilizar: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/common/chat/MessageBodyText.kt`
- Reutilizar: `Android/src/main/java/com/google/ai/edge/gallery/ui/common/chat/MessageBubbleShape.kt`
- Testar: `Android/src/app/src/androidTest/java/com/google/ai/edge/gallery/ui/common/conversation/ConversationMessageCardTest.kt`

**Implementação:**

- Separar apresentação de dados e ações.
- Usar o mesmo suporte a Markdown já presente no `MessageBodyText`.
- Usar `SelectionContainer` para respostas.
- Preservar acessibilidade (`contentDescription`, leitura de resposta finalizada e labels em português).
- Não exibir menu completo permanentemente.

**Verificação:**

- Preview Compose para mensagem do usuário, resposta Markdown e resposta em streaming.
- Teste semântico confirma que a mensagem possui texto acessível.

---

### Task 1.2: Criar a lista de conversa do modo de voz

**Objetivo:** Substituir a exibição isolada de `recognizedText`/`lastResponse` por uma linha do tempo rolável.

**Arquivos:**
- Criar: `Android/src/app/src/main/java/com/google/ai/edge/gallery/voice/presentation/VoiceConversationTimeline.kt`
- Modificar: `Android/src/app/src/main/java/com/google/ai/edge/gallery/voice/presentation/LiveChatScreen.kt`
- Modificar: `Android/src/app/src/main/java/com/google/ai/edge/gallery/voice/presentation/VoiceViewModel.kt`

**Implementação:**

- Expor no `VoiceViewModel` um `StateFlow<List<ConversationUiMessage>>` derivado de `sessionMessages`.
- Atualizar a lista quando uma transcrição é confirmada, quando a geração inicia, quando a resposta termina e quando ocorre erro/cancelamento.
- Manter o texto reconhecido em estado separado apenas durante a captura/revisão; depois do envio, a fonte visual deve ser a lista de mensagens.
- Fazer a lista rolar automaticamente para a última mensagem sem impedir rolagem manual.
- Manter o cartão de estado da voz (`ouvindo`, `pensando`, `falando`) fora da lista.

**Riscos a cobrir:**

- Não inserir mensagens duplicadas quando o modelo emite múltiplas atualizações de streaming.
- Não perder mensagens quando a tela recompõe.
- Não mostrar resposta antiga depois de `startNewSession()`.

**Verificação:**

- Iniciar nova sessão, enviar dois turnos e confirmar quatro mensagens na ordem correta.
- Fechar e reabrir a sessão; confirmar que o histórico persistido reaparece.

---

## Fase 2 — Entrada textual e revisão de voz

### Task 2.1: Criar a barra de entrada híbrida

**Objetivo:** Permitir alternância entre teclado e microfone sem alterar a ação primária da tela.

**Arquivos:**
- Criar: `Android/src/main/java/com/google/ai/edge/gallery/ui/common/conversation/ConversationInputBar.kt`
- Modificar: `Android/src/app/src/main/java/com/google/ai/edge/gallery/voice/presentation/LiveChatScreen.kt`
- Modificar: `Android/src/app/src/main/java/com/google/ai/edge/gallery/voice/presentation/VoiceViewModel.kt`
- Testar: `Android/src/app/src/androidTest/java/com/google/ai/edge/gallery/ui/common/conversation/ConversationInputBarTest.kt`

**Comportamento:**

- Estado padrão: botão grande de microfone.
- Botão secundário: alternar para teclado.
- Modo teclado: `TextField` multilinha, enviar, anexar e voltar ao modo voz.
- Desabilitar envio vazio.
- Limpar texto somente após confirmação de envio.
- Esconder teclado ao enviar.
- Permitir cancelar edição sem alterar o histórico.

**Acessibilidade:**

- Labels: `Usar teclado`, `Usar microfone`, `Enviar mensagem`, `Cancelar edição`.
- O botão de voz não deve ser apenas um elemento clicável sem semântica.

---

### Task 2.2: Implementar revisão de transcrição

**Objetivo:** Manter a conversa contínua, enviando automaticamente transcrições confiáveis e oferecendo correção quando o reconhecimento estiver incerto.

**Arquivos:**
- Modificar: `Android/src/app/src/main/java/com/google/ai/edge/gallery/voice/presentation/LiveChatScreen.kt`
- Modificar: `Android/src/app/src/main/java/com/google/ai/edge/gallery/voice/presentation/VoiceViewModel.kt`
- Modificar: arquivo de preferências/DataStore já usado para configurações do Kabem Voice
- Testar: `Android/src/app/src/test/java/com/google/ai/edge/gallery/voice/language/SpeechReviewPolicyTest.kt`

**Estados:**

```text
LISTENING → GENERATING → SPEAKING
     └─ baixa confiança → TRANSCRIPT_REVIEW → GENERATING
```

**Implementação:**

- Criar `SpeechReviewPolicy` com limiar de baixa confiança (`0.55`).
- Usar revisão automática somente se houver score de confiança disponível e abaixo do limiar; caso não haja, enviar automaticamente.
- Exibir `Você disse:` com `Editar`, `Enviar` e `Descartar`.
- Para reconhecimento confiável, enviar a transcrição diretamente ao runtime sem exibir a barra de revisão.
- Nunca adicionar uma transcrição em revisão ao runtime antes de `Enviar`.
- Se o usuário tocar no microfone enquanto revisa, preservar o texto ou solicitar descarte explícito.

**Verificação:**

- Corrigir erro de transcrição e confirmar que somente o texto corrigido chega ao modelo.
- Descartar transcrição e confirmar que nenhum turno é persistido.
- Reiniciar o app durante revisão e confirmar que a revisão pendente não vira mensagem enviada.

---

## Fase 3 — Ações contextuais

### Task 3.1: Adicionar menu contextual de mensagem

**Objetivo:** Mostrar ações somente quando o usuário interagir com uma mensagem.

**Arquivos:**
- Criar: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/common/conversation/ConversationMessageActions.kt`
- Modificar: `ConversationMessageCard.kt`
- Modificar: `ConversationMessageActionPolicy.kt`
- Testar: `ConversationMessageActionsTest.kt`

**UX:**

- Toque longo ou botão de overflow abre `ModalBottomSheet`.
- Ações prioritárias: `Copiar`, `Editar` para usuário, `Mais` para o restante.
- Ações indisponíveis não devem aparecer, em vez de aparecerem desabilitadas sem explicação.
- Mostrar Snackbar após copiar: `Mensagem copiada`.

**Segurança/privacidade:**

- Compartilhar deve exigir ação explícita.
- Não copiar automaticamente respostas para a área de transferência.
- Não registrar conteúdo da mensagem em logs.

---

### Task 3.2: Reutilizar copiar e seleção existentes

**Objetivo:** Garantir paridade visual e comportamental com o `ChatPanel`.

**Arquivos:**
- Modificar: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/common/chat/MessageBodyText.kt` somente se necessário
- Reutilizar: `ClipboardManager`, `ClipData`, `SelectionContainer`, `LongPressCopyContainer`
- Testar: teste de UI para copiar e selecionar

**Implementação:**

- Extrair, se necessário, `copyToClipboard` para um helper de UI local compartilhado.
- Preservar `message` como label do clipboard.
- Usar `SelectionContainer` também para respostas Markdown; se o renderer atual não suportar seleção, criar uma versão textual acessível sem quebrar a renderização visual.
- Confirmar que copiar durante streaming está bloqueado.

---

### Task 3.3: Implementar compartilhar e ouvir novamente

**Objetivo:** Adicionar ações úteis para respostas finalizadas.

**Arquivos:**
- Criar: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/common/conversation/ConversationShare.kt`
- Modificar: `VoiceViewModel.kt`
- Modificar: `LiveChatScreen.kt`
- Testar: `ConversationShareTest.kt`

**Implementação:**

- Compartilhar via `Intent.ACTION_SEND` e Android Sharesheet.
- Compartilhar texto puro, sem incluir dados internos, IDs, prompts de sistema ou metadados sensíveis.
- `Ouvir novamente` deve chamar o mecanismo TTS já existente, sem regenerar a resposta.
- Desabilitar `Ouvir novamente` se TTS estiver indisponível.
- Cancelar reprodução anterior antes de iniciar outra.

---

## Fase 4 — Editar, reenviar e regenerar

### Task 4.1: Editar mensagem do usuário com invalidação segura

**Objetivo:** Permitir correção de uma pergunta sem deixar respostas incompatíveis ativas.

**Arquivos:**
- Modificar: `VoiceViewModel.kt`
- Criar: `Android/src/app/src/main/java/com/google/ai/edge/gallery/voice/conversation/ConversationEditController.kt`
- Testar: `ConversationEditControllerTest.kt`

**Fluxo:**

1. Usuário escolhe `Editar`.
2. O texto entra na barra de entrada.
3. A tela exibe `Editando mensagem`.
4. Usuário confirma `Editar e reenviar`.
5. Mensagens posteriores são removidas da sessão ativa ou viram uma nova ramificação explícita.
6. O runtime é reinicializado com o contexto anterior à mensagem editada.
7. A nova mensagem é enviada.
8. A sessão é persistida somente depois de o novo turno ser aceito.

**Proteções:**

- Não permitir editar mensagem enquanto geração estiver ativa.
- Não apagar dados persistidos antes de a nova operação ser validada.
- Em caso de falha, restaurar a sessão anterior e mostrar erro acionável.

---

### Task 4.2: Regenerar resposta

**Objetivo:** Permitir gerar uma nova resposta para a mesma mensagem do usuário.

**Arquivos:**
- Modificar: `VoiceViewModel.kt`
- Modificar: `ConversationMessageActionPolicy.kt`
- Testar: `RegenerateResponseTest.kt`

**Implementação:**

- Identificar o par pergunta/resposta correto.
- Remover ou substituir somente a resposta alvo.
- Preservar a pergunta original.
- Incrementar um identificador de geração para impedir que streaming antigo sobrescreva o novo.
- Cancelar TTS da resposta anterior antes de gerar novamente.

---

## Fase 5 — Integração com o `ChatPanel`

### Task 5.1: Migrar o `ChatPanel` para o contrato compartilhado sem regressão

**Objetivo:** Fazer o modo “Testar modelo” usar as mesmas políticas e componentes de ação.

**Arquivos:**
- Modificar: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/common/chat/ChatPanel.kt`
- Modificar: `ConversationMessageCard.kt`
- Modificar: `ConversationMessageActionPolicy.kt`

**Cuidados:**

- Preservar benchmark, templates, anexos e comportamento de `run again`.
- Não remover suporte a `ChatMessage` específico de imagem, áudio, webview ou painel de progresso.
- Fazer a adaptação somente para mensagens textuais na primeira entrega.
- Manter o modelo tradicional como fonte de estado durante a migração.

**Verificação:**

- Enviar texto, copiar resposta, selecionar resposta, executar novamente e benchmark.
- Comparar visualmente antes/depois em modo claro e escuro.

---

### Task 5.2: Unificar preferências de entrada

**Objetivo:** Permitir que o usuário escolha o modo de entrada sem duplicar configurações.

**Arquivos:**
- Criar/ajustar preferência no DataStore usado pelo app
- Modificar: `LiveChatScreen.kt`
- Modificar: `ChatPanel.kt` somente se a preferência também for global
- Testar: `InputModePreferenceTest.kt`

**Opções:**

```text
Entrada principal:
- Voz
- Texto
- Perguntar ao abrir
```

A preferência não deve alterar o funcionamento offline nem iniciar downloads.

---

## Fase 6 — Robustez, acessibilidade e falhas

### Task 6.1: Formalizar a máquina de estados da conversa

**Objetivo:** Impedir conflitos entre microfone, teclado, TTS, geração e edição.

**Arquivos:**
- Reutilizar: `VoiceConversationStateMachine.kt`
- Modificar: `VoiceConversationPhase.kt`
- Testar: testes de transições existentes e novos casos

**Estados mínimos:**

```text
IDLE
LISTENING
TRANSCRIPT_REVIEW
EDITING_MESSAGE
GENERATING
SPEAKING
ERROR
```

**Casos obrigatórios:**

- tocar microfone durante geração;
- abrir teclado durante fala;
- editar durante streaming;
- iniciar nova sessão durante TTS;
- cancelar revisão;
- perder foco ou sair da tela;
- trocar de modelo durante conversa.

Cada transição inválida deve ser ignorada de forma segura ou gerar uma ação clara para o usuário.

---

### Task 6.2: Adicionar testes de acessibilidade e UX

**Objetivo:** Garantir que a interface contextual seja utilizável com leitor de tela e teclado do Android.

**Arquivos:**
- Testes Compose em `Android/src/app/src/androidTest/java/com/google/ai/edge/gallery/ui/common/conversation/`
- Ajustes nos composables de conversa

**Verificações:**

- todos os ícones têm `contentDescription`;
- ações do bottom sheet têm labels claros;
- mensagem finalizada é anunciada uma vez;
- streaming não gera anúncios a cada token;
- foco retorna à entrada após cancelar edição;
- contraste e tamanho mínimo de toque seguem Material 3;
- rolagem não fica bloqueada pelo teclado.

---

## Fase 7 — Testes de integração e release

### Task 7.1: Criar matriz manual de aceitação

**Objetivo:** Validar o fluxo completo em aparelho real. O ambiente de desenvolvimento já possui JDK/Android SDK e executou os gates locais, mas não possui dispositivo Android conectado para a matriz manual.

**Matriz:**

| Cenário | Resultado esperado |
|---|---|
| Voz → resposta → copiar | Texto correto no clipboard e Snackbar |
| Voz → reconhecimento confiável | Texto é enviado automaticamente ao modelo |
| Voz → baixa confiança → editar transcrição → enviar | Somente texto corrigido chega ao modelo |
| Teclado → enviar | Mensagem entra na mesma linha do tempo |
| Toque longo na resposta | Menu contextual aparece |
| Selecionar resposta | Seleção funciona sem copiar automaticamente |
| Editar pergunta antiga | Respostas posteriores são invalidadas/ramificadas |
| Regenerar | Pergunta permanece e resposta muda |
| Ouvir novamente | TTS reproduz texto existente sem nova inferência |
| Compartilhar | Sharesheet recebe somente texto explícito |
| Nova sessão | Histórico ativo é limpo sem apagar sessões salvas |
| Reinício do app | Sessão reaparece sem mensagens duplicadas |
| Modelo em streaming | Ações perigosas ficam indisponíveis |
| Sem TTS | `Ouvir novamente` não aparece |
| Modo offline | Nenhuma ação acessa rede implicitamente |
| Pouco espaço/erro de modelo | Conversa informa falha sem perder histórico |

---

### Task 7.2: Executar gates locais e no GitHub Actions

**Objetivo:** Verificar compilação, testes unitários e APK antes de liberar a funcionalidade.

**Comandos:**

```bash
cd Android/src
./gradlew :app:testDebugUnitTest --no-daemon
./gradlew :app:assembleDebug --no-daemon
```

**Resultado esperado:**

- testes unitários passam;
- APK debug é gerado em `Android/src/app/build/outputs/apk/debug/app-debug.apk`;
- GitHub Actions publica o artefato com nome único por commit;
- teste manual no celular confirma a matriz da Task 7.1.

Os gates locais foram executados com JDK 21 e Android SDK 37. O GitHub Actions e a matriz manual em aparelho continuam sendo validações independentes necessárias antes de uma release.

---

## Ordem de commits sugerida

1. `test: add shared conversation message contracts`
2. `feat: add contextual message action policy`
3. `refactor: extract reusable conversation message card`
4. `feat: render persisted voice conversation timeline`
5. `feat: add hybrid text and voice input bar`
6. `feat: add transcript review before sending`
7. `feat: add contextual copy selection and share actions`
8. `feat: support edit resend and response regeneration`
9. `refactor: reuse conversation actions in chat panel`
10. `test: cover conversation state transitions and acceptance flows`

Cada commit deve compilar ou, quando o ambiente impedir compilação, ser acompanhado pelo resultado equivalente do GitHub Actions.

## Fora do escopo desta entrega

- seleção automática de modelos por intenção;
- download automático de modelos por capacidade;
- sincronização em nuvem;
- colaboração entre dispositivos;
- edição de respostas do assistente como se fossem mensagens do usuário;
- compartilhamento automático;
- telemetria de conteúdo de conversa.

Esses itens podem ser tratados depois que a camada compartilhada de conversa estiver estável.

## Definição de pronto

A funcionalidade estará pronta quando:

- o modo de voz e o modo “Testar modelo” usarem os mesmos contratos de mensagem e ações;
- teclado e microfone puderem ser usados na mesma conversa;
- transcrições puderem ser revisadas antes do envio;
- copiar, selecionar, compartilhar, editar, regenerar e ouvir novamente funcionarem conforme as políticas;
- histórico e novas sessões não produzirem duplicações ou perda de contexto;
- testes unitários, testes Compose e build do APK passarem no GitHub Actions;
- a matriz manual for validada em pelo menos um aparelho Android real;
- a interface continuar simples no estado inicial, com ações avançadas apenas sob demanda.
