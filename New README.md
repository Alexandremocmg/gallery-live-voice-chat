\`\`\`markdown:README.md  
\# Kabem Voice

Assistente de voz local para Android com respostas adaptativas por intenção. Fork especializado do \[Google AI Edge Gallery\](https://github.com/google-ai-edge/gallery), focado em conversação natural em tempo real.

\> \*\*Status:\*\* 🟡 Alpha — em desenvolvimento ativo, uso pessoal. Não publicado.

\---

\#\# O que é

Um app Android que roda LLMs multimodais \*\*100% no dispositivo\*\* (sem nuvem), com foco em interação por voz contínua estilo "ligação". Diferente de assistentes tradicionais, o Kabem Voice \*\*adapta dinamicamente\*\* o tamanho e a estrutura da resposta conforme a intenção detectada na fala do usuário.

\*\*Três coisas que o tornam diferente:\*\*

1\. \*\*Perfis dinâmicos de resposta\*\* — o app detecta se você quer uma resposta rápida, uma explicação, um passo a passo ou uma análise estratégica, e ajusta o modelo por turno sem reiniciar contexto.  
2\. \*\*Streaming de TTS por sentença\*\* — o áudio começa a tocar enquanto o modelo ainda está gerando o resto da resposta, eliminando o silêncio morto entre pergunta e resposta.  
3\. \*\*Continuidade contextual\*\* — frases como "mais", "continua", "aprofunda nisso" são interpretadas com base no turno anterior, sem depender de palavras-gatilho explícitas.

\---

\#\# Como funciona (visão de 30 segundos)

\`\`\`  
Voz do usuário  
    ↓ (ASR local)  
Texto transcrito  
    ↓  
Classificador de intenção (Kotlin, regex \+ contexto do turno anterior)  
    ↓ → Perfil detectado: FLASH | DETAILED | STEP\_BY\_STEP | STRATEGIC  
Instrução invisível prefixada ao prompt  
    ↓  
Gemma 4 E2B/E4B (LiteRT-LM)  
    ↓ → tokens em streaming  
TTS começa a falar a cada sentença completa  
    ↓  
Áudio na saída  
\`\`\`

Detalhes técnicos em \[\`ARCHITECTURE.md\`\](./ARCHITECTURE.md).

\---

\#\# Requisitos

\#\#\# Hardware alvo (validado)

\- \*\*Poco X7 Pro\*\* (Dimensity 8400, 8-12 GB RAM)

Outros dispositivos Android com pelo menos \*\*8 GB de RAM\*\* devem funcionar, mas não são testados oficialmente.

\#\#\# Software

\- Android 12 (API 31\) ou superior  
\- \~4 GB de armazenamento livre (para modelos)

\#\#\# Modelos suportados

| Modelo | Tamanho | RAM mínima | Uso recomendado |  
|---|---|---|---|  
| Gemma-4-E2B-it | 2.6 GB | 8 GB | Padrão — conversação leve |  
| Gemma-4-E4B-it | 3.7 GB | 12 GB | Qualidade superior |

Os modelos são baixados na primeira execução via Hugging Face.

\---

\#\# Como buildar

\`\`\`bash  
git clone \<url-do-repo\>  
cd kabem-voice  
./gradlew installDebug  
\`\`\`

O comando compila e instala o APK debug no dispositivo Android conectado via ADB.

Para build de release, é necessário configurar uma keystore própria — veja \[\`docs/BUILD.md\`\](./docs/BUILD.md) (a ser criado).

\---

\#\# Como usar

1\. Abra o app e conceda permissões de microfone  
2\. Baixe um dos modelos suportados (primeira execução)  
3\. Entre no modo \*\*Live Voice\*\* pelo botão na tela principal  
4\. Fale naturalmente — o assistente detecta o tipo de resposta que você precisa

\*\*Exemplos de comando:\*\*

\- \`"que horas são"\` → resposta curta (FLASH)  
\- \`"me explica como funciona um transistor"\` → resposta didática (DETAILED)  
\- \`"me ensina passo a passo a fazer pão"\` → resposta em etapas (STEP\_BY\_STEP)  
\- \`"cria um plano de estudos de inglês pra 6 meses"\` → resposta estruturada (STRATEGIC)  
\- \`"mais"\` / \`"aprofunda"\` / \`"continua"\` → segue a última resposta com mais profundidade

\---

\#\# Estrutura do repositório

\`\`\`  
kabem-voice/  
├── README.md                 ← você está aqui  
├── ARCHITECTURE.md           ← visão técnica completa  
├── ROADMAP.md                ← o que está sendo feito agora  
├── RISKS.md                  ← riscos técnicos conhecidos  
├── docs/  
│   ├── decisions/            ← ADRs (decisões arquiteturais)  
│   ├── TESTING.md            ← matriz de testes  
│   ├── GLOSSARY.md           ← vocabulário do projeto  
│   └── PERFORMANCE.md        ← benchmarks no Poco X7 Pro  
└── src/  
    └── main/kotlin/          ← código-fonte Android  
\`\`\`

\---

\#\# Documentação

\- 🏗️ \[\`ARCHITECTURE.md\`\](./ARCHITECTURE.md) — como o app é organizado  
\- 🗺️ \[\`ROADMAP.md\`\](./ROADMAP.md) — sprint atual e próximo  
\- ⚠️ \[\`RISKS.md\`\](./RISKS.md) — riscos ativos e mitigações  
\- 🧠 \[\`docs/decisions/\`\](./docs/decisions/) — decisões arquiteturais com justificativa  
\- 🧪 \[\`docs/TESTING.md\`\](./docs/TESTING.md) — cenários de teste  
\- 📊 \[\`docs/PERFORMANCE.md\`\](./docs/PERFORMANCE.md) — medições reais no dispositivo alvo

\---

\#\# Status atual do desenvolvimento

\*\*O que já funciona:\*\*  
\- ✅ Modo Live Voice com Gemma 4 E2B  
\- ✅ Classificador de intenção com 4 perfis  
\- ✅ Injeção de instrução invisível por turno  
\- ✅ Streaming de TTS por sentença  
\- ✅ Continuidade contextual (Camada 0\)  
\- ✅ Indicador visual de perfil detectado

\*\*Em desenvolvimento:\*\*  
\- 🚧 Multimodalidade (anexar foto por turno)  
\- 🚧 Benchmarks estruturados no Poco X7 Pro

\*\*Planejado (sem data):\*\*  
\- 📋 Modo Professor com anexo de PDFs  
\- 📋 Persistência de sessões  
\- 📋 Publicação (requer gates de LGPD, security e user guide)

Detalhes atualizados em \[\`ROADMAP.md\`\](./ROADMAP.md).

\---

\#\# Sobre privacidade

O Kabem Voice é \*\*100% on-device\*\*. Nenhum áudio, transcrição, imagem ou dado pessoal sai do celular. Não há telemetria, analytics ou coleta de dados.

Uma política de privacidade formal será publicada em \[\`docs/PRIVACY.md\`\](./docs/PRIVACY.md) antes da primeira release pública.

\---

\#\# Créditos

Este projeto é derivado do \[Google AI Edge Gallery\](https://github.com/google-ai-edge/gallery), licenciado sob Apache 2.0. Os modelos Gemma são de propriedade do Google DeepMind, distribuídos sob os \[Gemma Terms of Use\](https://ai.google.dev/gemma/terms).

Runtime: \[LiteRT-LM\](https://github.com/google-ai-edge/LiteRT-LM)

\---

\#\# Licença

Apache 2.0 — veja \[\`LICENSE\`\](./LICENSE) (herdado do Gallery).  
\`\`\`

\---

Alguns pontos que quero destacar sobre este README:

\*\*O que fiz de propósito:\*\*  
\- \*\*Honesto sobre status alpha\*\* — não vendi mais do que é  
\- \*\*Sem promessa de features futuras como se já existissem\*\* — separei "funciona" de "em desenvolvimento" de "planejado"  
\- \*\*Linka para docs que ainda não existem\*\* mas que estão na lista Tier 1 — isso cria "pressão saudável" para produzi-los na sequência  
\- \*\*Menciona privacidade explicitamente\*\* — importante para posicionar mesmo antes de ter PRIVACY.md formal  
\- \*\*Créditos ao Gallery\*\* — importante juridicamente (Apache 2.0 exige atribuição)

\*\*O que \*\*não\*\* coloquei (e provavelmente você vai querer ajustar):\*\*  
\- Nome do repositório real (deixei \`\<url-do-repo\>\`)  
\- Screenshots (adicionar quando UI estabilizar)  
\- Badges (build status, versão) — só depois de ter CI

\*\*Ajustes que você pode fazer:\*\*  
\- Adicionar screenshot da tela Live Voice  
\- Trocar \`\<url-do-repo\>\` pela URL real  
\- Se quiser mais formal/mais informal, é só me falar o tom  
\- Adicionar seção "Autores" se quiser creditar quem ajuda

Quer que eu ajuste algo antes de partirmos para o próximo doc (\`ARCHITECTURE.md\`)? 🎯

\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_

\`\`\`markdown:ARCHITECTURE.md  
\# Arquitetura — Kabem Voice

\*\*Versão:\*\* 1.0  
\*\*Última atualização:\*\* \[data do commit\]  
\*\*Escopo:\*\* estado atual do app (voice chat com perfis dinâmicos). Extensões planejadas (multimodalidade, modo professor) estão marcadas explicitamente como \*\*\[Planejado\]\*\* e não representam código existente.

\---

\#\# 1\. Propósito deste documento

Este documento descreve \*\*como o Kabem Voice está organizado hoje\*\*. Ele serve a três funções:

1\. \*\*Onboarding\*\* — quem chega novo no projeto entende o fluxo em \~1 hora  
2\. \*\*Referência\*\* — quando o comportamento parecer estranho, este documento ajuda a localizar onde ele nasce  
3\. \*\*Base para decisões\*\* — mudanças arquiteturais grandes precisam ser refletidas aqui e registradas como ADR

Se algo neste documento não bate com o código, \*\*o código está certo e o documento está desatualizado\*\* — reporte ou corrija.

\---

\#\# 2\. O que o Kabem Voice adiciona ao Gallery base

Kabem Voice é um fork especializado do \[Google AI Edge Gallery\](https://github.com/google-ai-edge/gallery). A base fornece:

\- Runtime LiteRT-LM  
\- Carregamento e execução de modelos Gemma (\`.litertlm\`)  
\- Sistema de Skills (Markdown \+ JS em webview)  
\- Sistema de Native Actions (Kotlin com \`@Tool\`)  
\- Integração MCP (opcional)  
\- UI base do app

\*\*O Kabem Voice adiciona por cima:\*\*

\- Uma tela dedicada de conversação por voz contínua (\`LiveChatScreen\`)  
\- Um classificador de intenção local em Kotlin (regex \+ contexto)  
\- Injeção de instrução invisível por turno (adaptação sem reload de contexto)  
\- Streaming de TTS por sentença (não aguarda resposta completa do modelo)  
\- Herança de perfil por continuidade contextual (Camada 0\)

O restante do Gallery (Skills, MCP, chat padrão, etc.) permanece funcional mas não é o foco do Kabem Voice.

\---

\#\# 3\. Visão geral em uma imagem

\`\`\`  
┌─────────────────────────────────────────────────────────────────┐  
│                     LiveChatScreen (UI)                          │  
│  ┌────────────┐  ┌──────────────┐  ┌─────────────────────────┐ │  
│  │  Waveform  │  │ Chip perfil  │  │  Fallback manual (menu) │ │  
│  └────────────┘  └──────────────┘  └─────────────────────────┘ │  
└──────────────────────────┬──────────────────────────────────────┘  
                           │ eventos de UI  
                           ▼  
┌─────────────────────────────────────────────────────────────────┐  
│                       VoiceViewModel                             │  
│                                                                  │  
│  ┌────────────────────┐   ┌────────────────────────────────┐  │  
│  │ Captura de áudio   │   │ Classificador de intenção      │  │  
│  │ (ASR local)        │──▶│ \- Camada 0 (contexto anterior) │  │  
│  └────────────────────┘   │ \- Camada 1 (regex/keywords)    │  │  
│                            │ \- Regra de desempate           │  │  
│                            └───────────┬────────────────────┘  │  
│                                        │ Perfil \+ Instrução    │  
│                                        ▼                        │  
│                            ┌────────────────────────────────┐  │  
│                            │ Montagem do prompt final       │  │  
│                            │ \[instrução invisível\] \+ texto  │  │  
│                            └───────────┬────────────────────┘  │  
└────────────────────────────────────────┼───────────────────────┘  
                                         │  
                                         ▼  
┌─────────────────────────────────────────────────────────────────┐  
│                     VoiceChatManager                             │  
│                                                                  │  
│  ┌─────────────────────────┐    ┌──────────────────────────┐  │  
│  │ Inferência LiteRT-LM    │───▶│ Buffer de streaming      │  │  
│  │ (Gemma-4-E2B/E4B)       │    │ (detecta . \! ?)          │  │  
│  └─────────────────────────┘    └───────────┬──────────────┘  │  
│                                              │ sentenças        │  
│                                              ▼                   │  
│                                  ┌──────────────────────────┐   │  
│                                  │ TTS (por sentença)       │   │  
│                                  └───────────┬──────────────┘   │  
└──────────────────────────────────────────────┼──────────────────┘  
                                                │ áudio  
                                                ▼  
                                        \[ Speaker do device \]  
\`\`\`

O diagrama é intencionalmente ASCII para caber no README de qualquer editor. Se um dia for necessário algo mais rico, será adicionado em \`docs/diagrams/\`.

\---

\#\# 4\. Anatomia de um turno de voz (end-to-end)

Este é o fluxo completo do que acontece quando o usuário fala uma frase. Segui-lo mentalmente é a forma mais rápida de entender o app.

\#\#\# Passo 1 — Captura e transcrição  
\- Microfone é ativado quando o usuário entra em Live Voice  
\- ASR local converte fala → texto em tempo real  
\- Texto final é entregue ao \`VoiceViewModel\` quando a fala termina (VAD detecta silêncio)

\#\#\# Passo 2 — Classificação de intenção

O \`VoiceViewModel\` chama \`determinarPerfilMensagem(historicoTurnoAnterior, texto)\`, que executa em \*\*três estágios em ordem\*\*:

\*\*Estágio 2a — Camada 0 (contexto anterior)\*\*  
Se o texto atual é uma continuação elíptica ("mais", "continua", "aprofunda", "e depois"), o classificador \*\*herda ou escala\*\* o perfil do turno anterior em vez de reclassificar do zero.

\*\*Estágio 2b — Camada 1 (regex/keywords)\*\*  
Se não é continuação, o texto é comparado contra os mapas de regex de cada perfil. Cada match adiciona pontos ao respectivo perfil.

\*\*Estágio 2c — Regra de desempate\*\*  
Se múltiplos perfis pontuam, aplica-se hierarquia:  
\- Modificadores explícitos de brevidade ("rápido", "curto", "resumido") forçam FLASH  
\- Caso contrário, o perfil de maior complexidade vence: STRATEGIC \> STEP\_BY\_STEP \> DETAILED \> FLASH

O resultado é um \`Perfil\` \+ uma \*\*instrução em linguagem natural\*\* que descreve como o modelo deve responder.

\#\#\# Passo 3 — Montagem do prompt final

O \`VoiceViewModel\` monta a mensagem que vai para o modelo assim:

\`\`\`  
\[INSTRUÇÃO INTERNA: {instrução do perfil}\] {texto transcrito do usuário}  
\`\`\`

Exemplo real (perfil STEP\_BY\_STEP):

\`\`\`  
\[INSTRUÇÃO INTERNA: O usuário quer um tutorial prático. Responda em tópicos  
falados usando conectivos como 'Primeiro', 'Depois' e 'Por fim'. Seja didático.\]  
me ensina a fazer pão do zero  
\`\`\`

O restante do histórico da conversa continua no context normalmente. \*\*A instrução invisível é adicionada por turno, não substitui o system prompt.\*\*

\#\#\# Passo 4 — Inferência

O \`VoiceChatManager\` envia o prompt para o Gemma via LiteRT-LM. Os tokens saem em streaming.

\#\#\# Passo 5 — Streaming de TTS

Um buffer acumula tokens até detectar fim de sentença (\`.\`, \`\!\`, \`?\` ou quebra de parágrafo). Quando detecta:

\- Se o buffer tem tamanho mínimo (evita frases fragmentadas tipo "Sim.") → envia ao TTS  
\- Se termina em número seguido de ponto (ex: "1.") → aguarda mais tokens (evita cortar listas)  
\- Caso contrário → envia sentença ao TTS

O TTS começa a falar a primeira sentença \*\*enquanto o modelo ainda gera as próximas\*\*. Este é o mecanismo que elimina a latência percebida.

\#\#\# Passo 6 — Registro de contexto

Ao final do turno, o \`VoiceViewModel\` armazena:

\- Qual perfil foi usado  
\- Um resumo curto da resposta (últimas N palavras)

Isso serve de entrada para a Camada 0 do próximo turno.

\---

\#\# 5\. As 4 camadas conceituais do pipeline de voz

A arquitetura foi pensada em 4 camadas independentes. Entender a separação ajuda a decidir onde adicionar features novas.

| Camada | Nome | Papel | Onde vive |  
|---|---|---|---|  
| \*\*0\*\* | Contexto de turno anterior | Herda/escala perfil quando o turno atual é elíptico | \`VoiceViewModel\` |  
| \*\*1\*\* | Classificador de intenção | Regex/keywords que detectam perfis | \`VoiceViewModel\` |  
| \*\*2\*\* | Injeção de instrução invisível | Monta prompt com prefixo por perfil | \`VoiceViewModel\` |  
| \*\*3\*\* | Streaming de TTS por sentença | Emite áudio em pedaços conforme o modelo gera | \`VoiceChatManager\` |

\*\*Regra de ouro:\*\* essas camadas devem permanecer \*\*desacopladas\*\*. Trocar o classificador não deve afetar o streaming. Trocar o TTS não deve afetar o classificador. Se uma feature nova exigir acoplamento entre camadas, é sinal de que a separação precisa ser repensada — não que o acoplamento é aceitável.

\---

\#\# 6\. Os 4 perfis de resposta

| Perfil | Gatilhos típicos | Estrutura esperada |  
|---|---|---|  
| \*\*FLASH\*\* (padrão) | saudações, "que horas", "onde fica", "resuma" | Até 2 frases, fala direta |  
| \*\*DETAILED\*\* | "me explica", "como funciona", "por que" | 1 parágrafo conceitual |  
| \*\*STEP\_BY\_STEP\*\* | "passo a passo", "tutorial", "me ensina", "do zero" | Etapas curtas com conectivos ("Primeiro...", "Depois...") |  
| \*\*STRATEGIC\*\* | "plano", "estratégia", "analise", "compare", "roteiro" | Análise em blocos conceituais |

\`STEP\_BY\_STEP\` e \`STRATEGIC\` são intencionalmente separados. Eles têm prosódia falada diferente — tutorial pede cadência sequencial, plano pede cadência analítica. Um TTS competente renderiza os dois de forma diferente.

\*\*FLASH é sempre o default\*\* quando nenhum gatilho é detectado. Isso é uma decisão consciente: é melhor errar para o lado curto (usuário pede mais) do que para o lado longo (usuário se cansa).

Casos de teste dos perfis: veja \[\`docs/TESTING.md\`\](./docs/TESTING.md).

\---

\#\# 7\. Estado e persistência

O que o app \*\*guarda em memória\*\* durante uma sessão:

\- Histórico da conversa (dentro do context do modelo)  
\- Último perfil usado (para Camada 0\)  
\- Resumo curto da última resposta (para Camada 0\)  
\- Estado da UI (waveform, chip de perfil, indicador de fala/escuta)

O que o app \*\*não guarda hoje\*\* (fora da sessão):

\- Sessões anteriores não são persistidas — fechar o app perde o histórico  
\- Não há banco de dados local ativo  
\- Não há telemetria, analytics ou log remoto

Persistência de sessões está em \*\*\[Planejado\]\*\* — quando implementada, virá com criptografia at-rest e política de retenção clara. Antes disso, o modelo é: \*\*cada sessão é efêmera por design\*\*.

\---

\#\# 8\. Modelos usados

Kabem Voice roda em cima da runtime LiteRT-LM com os seguintes modelos do allowlist oficial do Gallery:

| Modelo | Tamanho | RAM mínima | Contexto | Uso |  
|---|---|---|---|---|  
| Gemma-4-E2B-it | 2.6 GB | 8 GB | 32K tokens (configurado) | Padrão |  
| Gemma-4-E4B-it | 3.7 GB | 12 GB | 32K tokens (configurado) | Qualidade superior |

\*\*Nota sobre context:\*\* o Gemma 4 suporta arquiteturalmente até 128K tokens. O Gallery configura 32K para caber em memória de dispositivos mobile. Aumentar esse limite é possível mas custa RAM adicional (\~2-4 MB por 1K tokens em INT4). Ver ADR-004 (quando produzido) sobre essa decisão.

O Kabem Voice \*\*não usa\*\* hoje:  
\- FunctionGemma 270M (fine-tunes Mobile Actions / Tiny Garden)  
\- Gemma 3n (versões E2B/E4B)  
\- Qwen 2.5, DeepSeek R1

Esses modelos ficam disponíveis pelo allowlist herdado do Gallery, mas o fluxo principal do Kabem Voice foi otimizado para Gemma-4-E2B como padrão.

\---

\#\# 9\. Onde as coisas vivem no código

Esta seção é a "planta baixa" do repositório do ponto de vista de features do Kabem Voice.

\`\`\`  
src/main/kotlin/com/kabem/voice/  
├── ui/  
│   └── LiveChatScreen.kt        ← tela de voz, waveform, chip de perfil  
├── viewmodel/  
│   └── VoiceViewModel.kt        ← orquestração de turno, Camadas 0/1/2  
├── voice/  
│   └── VoiceChatManager.kt      ← inferência \+ streaming de TTS (Camada 3\)  
├── classifier/  
│   ├── IntentClassifier.kt      ← regex e keywords por perfil  
│   └── Perfil.kt                ← enum dos 4 perfis \+ instruções  
└── ...  
\`\`\`

\*Nota: nomes de arquivos podem variar. Ajuste conforme a estrutura real do projeto.\*

Componentes herdados do Gallery (não modificados) ficam em seus pacotes originais.

\---

\#\# 10\. Extensões planejadas (não implementadas)

Esta seção existe para \*\*prevenir retrabalho\*\*: se você está prestes a implementar algo aqui, releia antes.

\#\#\# Multimodalidade (Fase 1\)

Objetivo: aceitar \*\*uma foto\*\* como anexo em um turno.

Componentes que virão:  
\- Um \`AttachmentManager\` (novo) que gerencia anexos ativos  
\- Extensão da instrução invisível com metadados de anexo  
\- Passagem da imagem para o Gemma via API multimodal do LiteRT-LM

O que \*\*não vai\*\* vir junto: PDFs, múltiplas fotos por turno, persistência de anexos. Esses ficam para depois de validar a stack multimodal com o caso mais simples.

\#\#\# Modo Professor (Fase 2\)

Objetivo: sessões longas com anexos (PDFs) e explicações estruturadas.

Componentes que virão:  
\- Um \`IntentRouter\` que decide entre modos operacionais (chat, teach, analyze)  
\- Um \`ContextBudgetManager\` para sumarização automática quando o context enche  
\- Pipeline de PDF (texto vs. render como imagem, decisão híbrida)  
\- Layout de tela dedicado ao modo professor

\#\#\# Persistência de sessão (depois de multimodalidade)

Componentes que virão:  
\- Room database (criptografado)  
\- Repositório de sessões  
\- UX de "continuar sessão" ao reabrir o app

Detalhes de cronograma em \[\`ROADMAP.md\`\](./ROADMAP.md).

\---

\#\# 11\. Não-objetivos deliberados

Coisas que o Kabem Voice \*\*conscientemente não faz\*\*, para preservar foco:

\- \*\*Não envia dados para nuvem.\*\* Nenhum áudio, transcrição ou anexo sai do device.  
\- \*\*Não redige mensagens/emails automaticamente.\*\* O fluxo de composição com aprovação está fora do escopo atual (foi considerado e cortado — ver ADR-005 quando produzido).  
\- \*\*Não faz chamadas de function calling proativas.\*\* Se a intenção do usuário é executar uma ação (mandar mensagem, abrir app), isso passa pelo fluxo de Native Actions do Gallery — não pelo pipeline do Kabem Voice.  
\- \*\*Não substitui o modo chat padrão do Gallery.\*\* O Kabem Voice é uma tela adicional, não uma substituição.  
\- \*\*Não suporta múltiplos idiomas simultaneamente por hora.\*\* Foco em PT-BR. Suporte multilíngue depende do modelo, mas o classificador de intenção é PT-BR only.

Se uma feature parecer necessária mas cair em um desses não-objetivos, ela deve virar um ADR de reversão explícita antes de ser implementada. Isso preserva a memória de por que a decisão foi tomada.

\---

\#\# 12\. Decisões arquiteturais registradas

As decisões abaixo têm ADRs dedicados em \[\`docs/decisions/\`\](./docs/decisions/):

\- ADR-001 — Streaming de TTS por sentença  
\- ADR-002 — Camada 0 (contexto de turno anterior)  
\- ADR-003 — Quatro perfis de resposta (não dois, não seis)  
\- ADR-004 — Instrução invisível por turno em vez de recarregar system prompt  
\- ADR-005 — Compose/email fora de escopo (por enquanto)

\*Números e conteúdo dos ADRs a serem consolidados na sequência de documentação.\*

\---

\#\# 13\. Como propor mudanças arquiteturais

1\. Abrir um ADR novo em \`docs/decisions/\` seguindo o template padrão  
2\. Descrever contexto, decisão, consequências e alternativas  
3\. Se a mudança afeta este documento, incluir patch aqui na mesma alteração  
4\. Merge acontece após revisão — nunca "fico devendo o ADR pra depois"

Documento vivo requer disciplina viva. A regra é simples: \*\*decisão sem ADR é decisão que será esquecida\*\*.

\---

\#\# 14\. Referências externas

\- \[Google AI Edge Gallery\](https://github.com/google-ai-edge/gallery) — projeto base  
\- \[LiteRT-LM Kotlin API\](https://github.com/google-ai-edge/LiteRT-LM/blob/main/docs/api/kotlin/getting\_started.md)  
\- \[Gemma 4 docs\](https://ai.google.dev/gemma)  
\- \[Function Calling Guide\](https://github.com/google-ai-edge/gallery/blob/main/Function\_Calling\_Guide.md) — para entender Native Actions  
\`\`\`

\---

Alguns pontos que quero destacar sobre esse ARCHITECTURE.md:

\*\*Decisões conscientes que tomei:\*\*

1\. \*\*ASCII diagram em vez de imagem\*\* — sobrevive em qualquer editor, versionável, editável por qualquer um. Se um dia precisar de algo mais rico, aí sim.

2\. \*\*Seção "Anatomia de um turno" com 6 passos numerados\*\* — é a coisa mais valiosa desse doc. Alguém entrando no projeto lê essa seção e entende 80% em 5 minutos.

3\. \*\*Marcações \[Planejado\] explícitas\*\* — evita o problema clássico de docs prometendo features que nunca existiram no código.

4\. \*\*Seção 11 "Não-objetivos"\*\* — essa é meio incomum mas eu recomendo muito. Documenta o que você \*\*conscientemente não faz\*\*. Evita que 3 meses depois alguém pergunte "por que não temos X?" e ninguém lembre por que.

5\. \*\*Seção 9 "Onde as coisas vivem"\*\* — planta baixa do código. Coloquei um disclaimer porque não conheço a estrutura real do seu repo. \*\*Você provavelmente vai querer ajustar isso\*\* com os nomes reais dos seus pacotes/arquivos.

6\. \*\*Referência a ADRs que ainda não existem\*\* — cria pressão saudável para produzi-los.

\*\*O que provavelmente você vai querer ajustar:\*\*

\- \*\*Nomes dos arquivos Kotlin reais\*\* (seção 9\)  
\- \*\*Estrutura real de diretórios\*\* do seu projeto  
\- \*\*Data da última atualização\*\* e versão  
\- Se o TTS que você usa é o do Android SDK ou uma lib específica — pode adicionar na seção 4 passo 5  
\- Se tiver algum detalhe do ASR (Whisper local? SpeechRecognizer do Android?) — vale mencionar

\*\*O que deliberadamente NÃO coloquei:\*\*

\- Diagramas com cores/UML  
\- Métricas de performance (isso vai no PERFORMANCE.md)  
\- Riscos (isso vai no RISKS.md)  
\- Detalhes de teste (isso vai no TESTING.md)

Cada doc deve ter um propósito. Se enche ARCHITECTURE.md com riscos e testes, ele vira intragável e ninguém mantém.

Quer que eu ajuste algo antes de partirmos para o próximo? Sugestão de próximo: \`ROADMAP.md\` (curto, iterativo) ou os \*\*ADRs retroativos\*\* (que o ARCHITECTURE.md já referencia). Você escolhe. 🎯

Vou produzir os dois na sequência. Começo pelo \`RISKS.md\` porque ele \*\*informa\*\* o roadmap (você não planeja sprints sem saber os riscos), e depois o \`ROADMAP.md\` referenciando os riscos identificados.

\---

\`\`\`markdown:RISKS.md  
\# Riscos — Kabem Voice

\*\*Versão:\*\* 1.0  
\*\*Última atualização:\*\* \[data do commit\]  
\*\*Próxima revisão:\*\* ao final da Fase 1 (multimodalidade), ou em 2 meses, o que vier primeiro.

\---

\#\# 1\. Propósito

Este documento lista os riscos técnicos e operacionais \*\*ativos\*\* do Kabem Voice — aqueles que podem inviabilizar features planejadas, degradar a experiência ou bloquear a publicação.

\*\*Não é\*\* uma lista exaustiva de tudo que pode dar errado. É uma lista \*\*acionável\*\*: cada risco tem probabilidade estimada, impacto e uma mitigação (ou plano de mitigação).

\*\*Como usar:\*\*  
\- Antes de começar um sprint, releia os riscos que tocam o escopo do sprint  
\- Ao terminar um sprint, atualize o status dos riscos afetados  
\- Riscos fechados (mitigados ou eliminados) vão para a seção de arquivo no final

\---

\#\# 2\. Convenções

\*\*Probabilidade:\*\*  
\- 🔴 Alta — mais provável acontecer do que não  
\- 🟡 Média — pode acontecer em condições realistas  
\- 🟢 Baixa — precisa de conjunção de fatores incomuns

\*\*Impacto:\*\*  
\- 🔴 Crítico — bloqueia release, quebra funcionalidade central, ou tem consequência legal  
\- 🟡 Alto — degrada experiência de forma perceptível para o usuário  
\- 🟢 Médio — inconveniente, mitigável em runtime

\*\*Status:\*\*  
\- 🔴 Aberto — não tem mitigação implementada  
\- 🟡 Em mitigação — plano definido, execução em andamento  
\- 🟢 Mitigado — mitigação implementada e validada  
\- ⚫ Arquivado — não é mais relevante

\---

\#\# 3\. Riscos ativos

\#\#\# R01 — Throttling térmico em sessões longas

| Campo | Valor |  
|---|---|  
| \*\*Probabilidade\*\* | 🔴 Alta |  
| \*\*Impacto\*\* | 🟡 Alto |  
| \*\*Status\*\* | 🔴 Aberto |

\*\*Descrição:\*\*  
Rodar Gemma-4-E2B/E4B continuamente no Poco X7 Pro em modo "ligação" (microfone \+ inferência \+ TTS ativos por 15+ minutos) provavelmente vai levar o SoC a throttling térmico. Sintomas esperados: queda de tokens/segundo, aumento de latência, TTS começando a atrasar.

\*\*Por que importa:\*\*  
O caso de uso principal do Kabem Voice é conversação natural longa. Se o app degrada aos 15 minutos, a proposta de valor está comprometida.

\*\*Mitigações possíveis:\*\*  
\- Medir cedo (Fase 1\) o comportamento real em sessões de 5, 10, 20 e 30 minutos  
\- Se degradação for confirmada:  
  \- Considerar downgrade automático de E4B → E2B após temperatura X  
  \- Considerar redução de sampling do TTS quando temperatura sobe  
  \- Considerar modo "pausa forçada" educando o usuário sobre ciclos de conversa  
\- Ainda não implementado.

\*\*Como sabemos que está resolvido:\*\*  
Uma sessão de 20 minutos no Poco X7 Pro mantém latência TTS \< 1s e degradação de tokens/segundo \< 15%.

\---

\#\#\# R02 — Estouro de context window com anexos

| Campo | Valor |  
|---|---|  
| \*\*Probabilidade\*\* | 🟡 Média |  
| \*\*Impacto\*\* | 🟡 Alto |  
| \*\*Status\*\* | 🔴 Aberto |

\*\*Descrição:\*\*  
O context configurado no app é 32K tokens. Uma sessão com foto (256-1024 tokens) \+ PDF de 5 páginas (\~2000-5000 tokens) \+ histórico de conversa longa pode encher rapidamente. Sem gerenciamento ativo, o comportamento esperado é degradação silenciosa: respostas truncadas, ou erro de context overflow do LiteRT-LM.

\*\*Por que importa:\*\*  
Modo Professor (Fase 2\) depende de anexos. Se o app quebra ou degrada com PDF anexado, a feature não existe na prática.

\*\*Mitigações possíveis:\*\*  
\- Implementar \`ContextBudgetManager\` \*\*antes\*\* de introduzir PDFs  
\- Estratégia de sumarização automática de anexos "antigos" na sessão  
\- Gatilhos preliminares (em tokens absolutos, a serem calibrados após medição):  
  \- Ao passar de 24K tokens: sumarizar anexos \> 5 min de idade  
  \- Ao passar de 28K tokens: sumarizar histórico de turnos antigos  
  \- Ao passar de 30K tokens: alertar usuário  
\- Investigar viabilidade de aumentar \`maxContextLength\` para 64K ou 96K (custa RAM adicional)

\*\*Como sabemos que está resolvido:\*\*  
Sessão simulada com foto \+ PDF de 10 páginas \+ 20 turnos de conversa completa sem erro e com qualidade de resposta perceptivelmente estável.

\---

\#\#\# R03 — LGPD ao publicar

| Campo | Valor |  
|---|---|  
| \*\*Probabilidade\*\* | 🔴 Alta (quando publicar) |  
| \*\*Impacto\*\* | 🔴 Crítico |  
| \*\*Status\*\* | 🟡 Em mitigação — planejado como gate |

\*\*Descrição:\*\*  
O Kabem Voice armazenará (na Fase de persistência) transcrições de voz, imagens anexadas e potencialmente conteúdo pessoal (extratos, documentos, mensagens redigidas). No Brasil, isso se enquadra como dado pessoal sob a LGPD assim que o app for usado por qualquer pessoa além do desenvolvedor.

\*\*Por que importa:\*\*  
Publicar sem conformidade LGPD implica risco jurídico, remoção da Play Store por violação de policies, e potenciais multas. Não é opcional.

\*\*Mitigações planejadas:\*\*  
\- Documentar em \`docs/PRIVACY.md\` antes de qualquer beta público  
\- Manter arquitetura 100% on-device (nenhum dado sai do celular) — reduz drasticamente exposição legal  
\- Criptografar banco local (Room \+ SQLCipher) quando persistência for implementada  
\- Política de retenção explícita e configurável pelo usuário  
\- Consentimento explícito por tipo de dado (áudio, imagem, documento)  
\- Direito de exclusão implementado (usuário pode apagar toda sua sessão)

\*\*Como sabemos que está resolvido:\*\*  
\`docs/PRIVACY.md\` publicado, mecanismos de exclusão testados, e revisão jurídica leiga feita antes de qualquer release pública.

\*\*Nota:\*\* Este risco está marcado como "Em mitigação" porque o plano existe. A execução da mitigação só acontece na fase de publicação.

\---

\#\#\# R04 — Consumo de bateria em uso intenso

| Campo | Valor |  
|---|---|  
| \*\*Probabilidade\*\* | 🔴 Alta |  
| \*\*Impacto\*\* | 🟡 Alto |  
| \*\*Status\*\* | 🔴 Aberto |

\*\*Descrição:\*\*  
Inferência de LLM local \+ microfone ativo \+ TTS \+ GPU acelerada \= consumo de bateria significativamente acima do típico para apps Android. Sessões longas podem esgotar bateria em 1-2 horas.

\*\*Por que importa:\*\*  
Um "professor pessoal" que descarrega o celular em 2 horas de estudo tem baixa adoção. É expectativa razoável que o app dure ao menos uma tarde de uso moderado.

\*\*Mitigações possíveis:\*\*  
\- Medir consumo real no Poco X7 Pro em cenários típicos  
\- Considerar modo "economia" que reduz taxa de amostragem do TTS  
\- Otimizar inferência apenas quando fala termina (não durante escuta ativa)  
\- Não pré-carregar segundo modelo se não estiver em uso  
\- Documentar consumo esperado para o usuário

\*\*Como sabemos que está resolvido:\*\*  
Sessão típica de 30 minutos consome \< 15% de bateria em dispositivo carregado com bateria saudável.

\---

\#\#\# R05 — Aprovação por voz mal interpretada (mitigado por corte de escopo)

| Campo | Valor |  
|---|---|  
| \*\*Probabilidade\*\* | 🟡 Média |  
| \*\*Impacto\*\* | 🔴 Crítico (se implementado) |  
| \*\*Status\*\* | ⚫ Arquivado (temporariamente) |

\*\*Descrição:\*\*  
Confusão entre "não" e "nossa\!", ou entre "confirma" e "conforme", pode levar a envio acidental de mensagens/emails ao contato errado.

\*\*Por que arquivado:\*\*  
O escopo de composição de mensagens/emails foi cortado do roadmap atual (ver ADR-005 quando produzido). Se essa feature voltar ao escopo, este risco reabre imediatamente com status Aberto.

\*\*Se reabrir, mitigações mínimas obrigatórias:\*\*  
\- Confirmação dupla explícita ("diga 'sim, enviar' ou 'cancelar'")  
\- Nunca enviar sem preview visual claro  
\- Log de todas as ações executadas (com opção de desfazer curto prazo)

\---

\#\#\# R06 — Classificador de intenção com falso positivo em ambiguidade

| Campo | Valor |  
|---|---|  
| \*\*Probabilidade\*\* | 🟡 Média |  
| \*\*Impacto\*\* | 🟢 Médio |  
| \*\*Status\*\* | 🟡 Em mitigação |

\*\*Descrição:\*\*  
Frases como "explica rápido" contêm gatilhos de perfis diferentes. Erro de classificação leva a resposta com estrutura errada (tutorial longo quando queria FLASH, ou vice-versa).

\*\*Por que importa:\*\*  
Não é crítico — o usuário pode reformular ou usar fallback manual. Mas se acontecer com frequência, degrada a percepção de "assistente inteligente".

\*\*Mitigações implementadas:\*\*  
\- Hierarquia de desempate: modificadores de brevidade explícita forçam FLASH; caso contrário, perfil mais complexo vence  
\- Fallback manual disponível via menu secundário  
\- Camada 0 (contexto anterior) reduz ambiguidade em turnos de continuação

\*\*Mitigações pendentes:\*\*  
\- Matriz de testes formal com 50+ frases (em \`docs/TESTING.md\`)  
\- Métrica de "% de fallback manual usado" para detectar regressão

\*\*Como sabemos que está resolvido:\*\*  
Matriz de testes com \> 90% de acerto no perfil esperado, e fallback manual usado em \< 5% dos turnos em sessões reais.

\---

\#\#\# R07 — Regressão em atualizações do LiteRT-LM ou Gemma

| Campo | Valor |  
|---|---|  
| \*\*Probabilidade\*\* | 🟡 Média |  
| \*\*Impacto\*\* | 🟡 Alto |  
| \*\*Status\*\* | 🔴 Aberto |

\*\*Descrição:\*\*  
LiteRT-LM está em desenvolvimento ativo. Atualizações podem mudar API, formato de modelos, comportamento de streaming, ou alterar performance. Modelos Gemma também recebem updates (o allowlist já mostra \`updatableModelFiles\`).

\*\*Por que importa:\*\*  
Uma atualização silenciosa que quebra o streaming de TTS ou muda a latência de prefill pode degradar o app sem que fique óbvio na hora.

\*\*Mitigações possíveis:\*\*  
\- Pinar versão do runtime LiteRT-LM em \`build.gradle\`  
\- Testar em ambiente controlado antes de atualizar  
\- Ter uma "smoke test" mínima que roda antes de aceitar update: prefill de X tokens, decode de Y tokens, latência TTS  
\- Registrar \`commitHash\` do modelo em cada sessão persistida (útil para debug de regressão futura)

\*\*Como sabemos que está resolvido:\*\*  
Procedimento de atualização documentado e seguido pelo menos uma vez com sucesso.

\---

\#\#\# R08 — Ambiguidade "modo conversa" vs "ação nativa"

| Campo | Valor |  
|---|---|  
| \*\*Probabilidade\*\* | 🟡 Média |  
| \*\*Impacto\*\* | 🟢 Médio |  
| \*\*Status\*\* | 🔴 Aberto (baixa prioridade) |

\*\*Descrição:\*\*  
Se um dia o Kabem Voice tocar em Native Actions (mesmo que só do Gallery original), há ambiguidade: "manda um lembrete pro João" pode ser conversa ("me lembra de mandar mensagem") ou ação real (executar \`send\_text\`).

\*\*Por que importa:\*\*  
Baixa prioridade agora — Native Actions estão fora do fluxo Kabem Voice. Mas se um dia forem integradas, essa ambiguidade precisa de tratamento.

\*\*Mitigações futuras (se necessário):\*\*  
\- Router de nível superior antes do classificador de perfil  
\- Confirmação por voz em casos ambíguos  
\- Modo explícito "modo ação" ativado por comando

\---

\#\#\# R09 — OOM em dispositivos com 8GB durante multimodalidade

| Campo | Valor |  
|---|---|  
| \*\*Probabilidade\*\* | 🟡 Média |  
| \*\*Impacto\*\* | 🔴 Crítico |  
| \*\*Status\*\* | 🔴 Aberto |

\*\*Descrição:\*\*  
Gemma-4-E2B com context 32K consome \~5-6 GB de RAM ativa. Adicionar processamento de imagem (vision encoder) e múltiplos anexos pode empurrar o app para OOM em dispositivos no limite inferior (8GB).

\*\*Por que importa:\*\*  
Poco X7 Pro tem variantes de 8GB e 12GB. A versão de 8GB é o piso do público-alvo. Se OOM acontece nela, feature multimodal fica restrita a devices premium.

\*\*Mitigações possíveis:\*\*  
\- Downscale de imagens antes de passar ao vision encoder (não usar resolução total)  
\- Limite hard de anexos ativos simultâneos (ex: 1 imagem por turno na Fase 1\)  
\- Monitorar RAM em tempo real e degradar graciosamente (avisar usuário se anexo não puder ser processado)  
\- Ter path "só texto" que ignora anexo se RAM não permite

\*\*Como sabemos que está resolvido:\*\*  
Testes em Poco X7 Pro 8GB (ou equivalente) com foto anexada não geram OOM em 50 execuções consecutivas.

\---

\#\#\# R10 — Sobrecarga cognitiva por indicador visual de perfil

| Campo | Valor |  
|---|---|  
| \*\*Probabilidade\*\* | 🟢 Baixa |  
| \*\*Impacto\*\* | 🟢 Médio |  
| \*\*Status\*\* | 🔴 Aberto |

\*\*Descrição:\*\*  
O chip visual (⚡📘🪜📊) foi pensado para dar feedback. Mas em modo "ligação", o usuário provavelmente não olha a tela. Se olhar, o chip pode ser mais distração que ajuda.

\*\*Por que importa:\*\*  
Baixa prioridade. Mas é a diferença entre UX polida e UX barulhenta.

\*\*Mitigações possíveis:\*\*  
\- Testar variantes: chip só aparece na primeira mudança de perfil, ou fica só na cor da waveform  
\- Considerar sinal auditivo sutil (tick diferente) para mudanças de modo em uso hands-free  
\- Recolher feedback de uso real antes de decidir

\---

\#\# 4\. Riscos arquivados

\#\#\# R05 — Aprovação por voz mal interpretada  
Arquivado quando o escopo de composição de mensagens foi cortado do roadmap (Dez/2024). Reabre se a feature voltar ao escopo.

\---

\#\# 5\. Riscos considerados e explicitamente aceitos

Riscos que existem mas foram avaliados e considerados aceitáveis dado o contexto atual:

\#\#\# RA01 — Sessão perdida ao fechar o app  
\*\*Racional:\*\* persistência de sessão está planejada para depois de multimodalidade. Enquanto isso, o app é "efêmero por design". Aceito para uso pessoal e alpha.

\#\#\# RA02 — Sem suporte multilíngue  
\*\*Racional:\*\* foco em PT-BR mantém complexidade baixa. Suporte multilíngue depende de retrabalho no classificador de intenção (regex é PT-BR only). Aceito para MVP.

\#\#\# RA03 — Sem controle de acesso ao app  
\*\*Racional:\*\* uso pessoal, dispositivo já tem proteção de tela. Vira risco crítico quando publicar (entra como R? em nova versão).

\---

\#\# 6\. Processo de revisão

Revisar este documento:  
\- Ao final de cada sprint que toca riscos ativos  
\- Antes de iniciar uma nova fase do roadmap  
\- Após qualquer incidente real (adicionar RIN — risco identificado por incidente)  
\- No mínimo a cada 2 meses, mesmo sem eventos

Um risco não revisado por \> 3 meses deve ser marcado como suspeito de estar desatualizado.  
\`\`\`

\---

Agora o ROADMAP, que referencia os riscos identificados:

\`\`\`markdown:ROADMAP.md  
\# Roadmap — Kabem Voice

\*\*Versão:\*\* 1.1  
\*\*Última atualização:\*\* \[data do commit\]  
\*\*Horizonte:\*\* curto prazo (2-4 sprints à frente). Além disso, é especulação.

\---

\#\# 1\. Filosofia

Este roadmap \*\*não promete features com data\*\*. Promete direção.

Três razões:

1\. \*\*Sem pressa comercial\*\* — o projeto é pessoal, sem prazo externo. Priorizar qualidade sobre velocidade é escolha consciente.  
2\. \*\*Feedback dita ritmo\*\* — o que fizer sentido depois de multimodalidade depende do que aprendermos com multimodalidade. Roadmap detalhado de 6 meses é ficção.  
3\. \*\*Time pequeno\*\* — planejamento otimista custa retrabalho. Melhor entregar menos com solidez.

\*\*Formato:\*\* três seções: \*\*fazendo agora\*\*, \*\*próximo\*\*, \*\*parking lot\*\*. Nada mais.

\---

\#\# 2\. Fazendo agora

\*\*Sprint atual: Documentação de fundação\*\*

Objetivo: consolidar em documentos os aprendizados e decisões já tomadas antes de partir para features novas. Sem essa fundação, retrabalho é garantido.

\*\*Escopo:\*\*  
\- \[x\] \`README.md\`  
\- \[x\] \`ARCHITECTURE.md\`  
\- \[x\] \`RISKS.md\` v1  
\- \[x\] \`ROADMAP.md\`  
\- \[x\] \`docs/decisions/ADR-001\` — Streaming de TTS por sentença  
\- \[x\] \`docs/decisions/ADR-002\` — Camada 0 (contexto anterior)  
\- \[x\] \`docs/decisions/ADR-003\` — Quatro perfis de resposta  
\- \[x\] \`docs/decisions/ADR-004\` — Injeção invisível vs system prompt  
\- \[x\] \`docs/decisions/ADR-005\` — Compose/email fora de escopo  
\- \[x\] \`docs/GLOSSARY.md\`  
\- \[x\] \`docs/TESTING.md\` v1 (matriz de frases do classificador)

\*\*Critério de saída:\*\* onboarding de um dev novo em \~1 hora usando apenas os documentos. ✅ Atingido.

\---

\#\# 3\. Próximo

\*\*Fase 1 — Multimodalidade básica (uma foto por turno)\*\*

Objetivo: provar que a stack multimodal do LiteRT-LM funciona no Kabem Voice, com o caso mais simples possível.

\*\*Escopo consciente da fase:\*\*  
\- Anexar \*\*uma foto\*\* por turno via botão dedicado na \`LiveChatScreen\`  
\- Novo componente \`AttachmentManager\` v1 (apenas em memória, sem persistência)  
\- Extensão da instrução invisível para sinalizar presença do anexo  
\- Downscale automático de imagens grandes (mitigação de R09)  
\- Chip visual persistente do anexo enquanto ele estiver "ativo" no turno

\*\*Fora do escopo da fase:\*\*  
\- Múltiplas fotos por turno  
\- Retenção de foto entre turnos (foto é "consumida" no turno em que aparece)  
\- PDF, áudio, ou qualquer outro tipo de anexo  
\- Persistência de sessão

\*\*Riscos que dominam a fase (ver \`RISKS.md\`):\*\*  
\- R09 — OOM em dispositivos 8GB → mitigar com downscale e limite de 1 anexo  
\- R01 — Throttling térmico → medir e documentar antes de partir para Fase 2  
\- R04 — Bateria → medir e documentar

\*\*Antes da Fase 1 fechar, produzir:\*\*  
\- \`docs/PERFORMANCE.md\` v1 com benchmarks reais no Poco X7 Pro  
\- Atualização de \`RISKS.md\` com os aprendizados de campo  
\- ADRs para decisões novas que aparecerem (ex: como decidiu tamanho de downscale)

\*\*Critério de saída:\*\* usuário abre Live Voice, tira uma foto, pergunta "o que tem nessa foto" por voz, recebe resposta multimodal por voz. Zero crashes em 20 execuções consecutivas no Poco X7 Pro.

\---

\#\# 4\. Parking lot (ideias registradas, não priorizadas)

Ideias que vieram nas conversas de arquitetura e que valem a pena registrar, mas \*\*não\*\* são compromissos.

\*\*Ordem aqui não é ordem de execução.\*\* Apenas registro. As Fases numeradas indicam sequência lógica de dependência, não compromisso de execução.

\#\#\# Fase 2 potencial — Modo Professor com PDFs

Depois de multimodalidade validada, escopo natural é PDF. Envolve:  
\- Pipeline de PDF (texto vs. render como imagem, decisão híbrida por tipo de PDF)  
\- \`ContextBudgetManager\` (dependência dura por causa de R02)  
\- \`IntentRouter\` distinguindo TEACH\_MODE de CONVERSATION\_MODE  
\- Layout de tela dedicado ao modo professor  
\- ADRs sobre decisões de PDF

\*\*Por que não é o próximo:\*\* depende de aprendizados da Fase 1\. Se OOM ou térmico for problema real, essa fase muda muito.

\#\#\# Fase 3 potencial — Persistência de sessão

Room \+ SQLCipher, com política de retenção. Pré-requisito para publicação e para Fase 4\.

Envolve:  
\- Schema de banco (sessões, turnos, anexos)  
\- Criptografia at-rest via SQLCipher  
\- UX de "continuar sessão anterior" ao reabrir o app  
\- Sumários automáticos entre sessões (não recarrega tudo, só o resumo)  
\- Política de retenção configurável pelo usuário  
\- Botão "apagar todas as sessões" (direito ao esquecimento)

\*\*Depende de:\*\* Fase 1 e 2 concluídas (a experiência de anexos ensina o que precisa persistir).

\*\*Habilita:\*\* Fase 4 (memória persistente) e gates de publicação.

\#\#\# Fase 4 potencial — Memória persistente estruturada

Base fundacional para o Kabem Voice "conhecer o usuário" ao longo do tempo, entregando continuidade real entre sessões e respostas contextualizadas.

\*\*Casos de uso mapeados (6):\*\*  
\- Fatos sobre o usuário (identidade, atributos estáveis)  
\- Contexto sobre pessoas (contatos, tom, relação)  
\- Preferências pessoais (o que gosta/não gosta no comportamento do app)  
\- Projetos ativos (tópicos multi-sessão em andamento)  
\- Progresso em Modo Professor (o que já foi coberto em cada estudo)  
\- Resposta contextualizada a mensagens (dependente dos casos acima)

\*\*Casos considerados e conscientemente excluídos:\*\*  
\- Compromissos com data → Google Calendar já resolve  
\- Padrões e insights inferidos automaticamente → terreno psicológico complicado, retorno duvidoso  
\- Rotinas com scheduling proativo → fora do escopo do app  
\- Estado emocional momentâneo e fatos efêmeros → não persistir

\*\*Abordagem prevista:\*\*  
Extração automática de fatos durante a conversa, com \*\*confirmação conversacional oportuna\*\* (o app pergunta ao final da resposta "guardei isso, tá certo?" para fatos de alta confiança). Fatos inferidos retrospectivamente (padrões multi-sessão) vão para fila de revisão em lote.

\*\*Estrutura prevista:\*\*  
Cinco categorias de fato tratadas separadamente na memória e na UI de revisão — não uma tabela genérica.

\*\*Depende de:\*\* Fase 3 (persistência básica de sessão) implementada e estável.

\*\*Habilita:\*\* Fase 5 (integrações externas — WhatsApp, notificações — usando contexto pessoal para resposta contextualizada).

\*\*Tamanho estimado:\*\* 4-6 sprints focados. Fase inteira, não incremento. Risco alto de escopo crescer — requer disciplina de recorte.

\*\*Riscos novos que essa fase introduz\*\* (a serem detalhados em \`RISKS.md\` quando fase ativar):  
\- Extração de fato com falso positivo (persistir informação errada contamina respostas futuras)  
\- LGPD com peso maior (dados persistentes sobre terceiros, não só sobre o usuário)  
\- Auditoria da memória vira feature obrigatória (usuário precisa poder ver, editar e apagar)

\*\*Status:\*\* Direção mapeada e validada em discussão de conceito. Não é compromisso de execução. Revisitar quando Fase 3 estiver próxima do fim.

\#\#\# Fase 5 potencial — Integrações externas com contexto pessoal

Uso da memória construída na Fase 4 para triagem e resposta contextualizada a notificações externas (WhatsApp, SMS, outros mensageiros).

\*\*Fluxo previsto:\*\*  
\- \`NotificationListenerService\` captura notificações de apps em whitelist  
\- Kabem Voice sumariza fila de mensagens não lidas por voz quando o usuário pergunta  
\- Ao pedir resposta, gera rascunho usando memória (contexto sobre remetente)  
\- Envio via \`RemoteInput\` da notificação original (único caminho legítimo com WhatsApp)  
\- Confirmação por toque ou por voz antes de enviar

\*\*Restrições conhecidas:\*\*  
\- Sem histórico de conversa acessível — só a mensagem que gerou notificação  
\- Google Play tem policies restritivas para apps com \`NotificationListenerService\`  
\- Enviar programaticamente pelo WhatsApp fora do \`RemoteInput\` é banimento certo  
\- LGPD sobre notificações de terceiros exige tratamento cuidadoso

\*\*Depende de:\*\* Fase 4 (memória persistente) — sem ela, rascunhos ficam genéricos e a feature perde valor.

\*\*Status:\*\* Direção mapeada. Revisitar quando Fase 4 estiver madura.

\#\#\# Barge-in (interrupção durante TTS)

Poder interromper o assistente falando enquanto ele responde. Sofisticado, mas grande impacto em naturalidade.

\#\#\# Sinalização auditiva de modo

Tick sutil quando muda de perfil, útil em uso hands-free (dirigindo, cozinhando).

\#\#\# Detecção de code-switching PT-BR / EN

Regex atual é PT-BR only. Frases mistas ("faz um pitch estratégico") caem em edge cases.

\#\#\# Métricas locais de qualidade

% de fallback manual usado, distribuição de perfis, latência de TTS. Ajuda a validar mitigações.

\#\#\# Gates de publicação

Quando o objetivo for publicar (ainda não é):  
\- \`docs/PRIVACY.md\`  
\- \`docs/SECURITY.md\`  
\- \`docs/USER\_GUIDE.md\`  
\- Room criptografado (já pré-requisito da Fase 3\)  
\- Fluxo de consentimento LGPD  
\- Revisão jurídica leiga

\#\#\# Ideias descartadas (não voltar sem ADR de reversão)

\- Composição automática de emails/SMS iniciada por comando de voz (cortada — ver ADR-005). Nota: \*\*diferente\*\* da Fase 5, que responde a mensagens recebidas, não inicia mensagens novas.  
\- MCP integrado ao fluxo Live Voice (fora do escopo por hora)  
\- Marketplace de skills customizadas  
\- Documentos anexados como base de "quem é você" (considerado e rejeitado — memória persistente automática cobre melhor, ver histórico da decisão em Seção 6\)  
\- Padrões e insights inferidos automaticamente sobre o usuário (considerado no mapeamento da Fase 4 e excluído — terreno psicológico complicado)  
\- Compromissos com data no app (considerado e excluído — Google Calendar resolve)  
\- Rotinas com scheduling proativo (fora do escopo do app)

\---

\#\# 5\. Critérios que fazem o roadmap avançar

\*\*Fase 1 avança para Fase 2 quando:\*\*  
\- Critério de saída da Fase 1 cumprido  
\- \`RISKS.md\` atualizado com aprendizados de campo  
\- Nenhum risco crítico novo (🔴 impacto) aberto sem mitigação  
\- Pelo menos 1 semana de uso pessoal real da Fase 1 sem regressão

\*\*Roadmap é revisado quando:\*\*  
\- Uma fase fecha  
\- Um risco crítico se materializa  
\- Uma decisão de escopo grande é tomada (ADR novo de escopo)  
\- A cada 2 meses, no mínimo

\*\*Roadmap NÃO é revisado por:\*\*  
\- Feature nova que "parece uma boa ideia"  
\- Comparação com o que outros apps têm  
\- Pressa gerada por prazos artificiais

\---

\#\# 6\. Histórico de decisões de roadmap

Registrar aqui mudanças significativas de direção. Serve como memória de por que o roadmap está como está.

| Data | Mudança | Motivo |  
|---|---|---|  
| \[inicial\] | Compose/email cortado do escopo | Complexidade alta, risco crítico (R05), dependência de segundo modelo. Foco em multimodalidade primeiro. Ver ADR-005. |  
| \[inicial\] | Persistência adiada para depois de multimodalidade | Ordem de dependência: multimodal ensina onde os riscos reais estão antes de comprometer com esquema de dados. |  
| \[inicial\] | Fase 1 restrita a "uma foto por turno" | Provar stack multimodal com caso mínimo antes de expandir. |  
| \[v1.1\] | Fase 4 (Memória persistente) mapeada e adicionada ao parking lot | Discussão de conceito validou 6 casos de uso legítimos que dependem de memória. Direção alinhada com proposta central do app. Não é compromisso de execução — revisitar quando Fase 3 estiver próxima do fim. |  
| \[v1.1\] | Fase 5 (Integrações externas) mapeada e adicionada ao parking lot | Materializa o valor da Fase 4 (memória) em caso de uso concreto (WhatsApp). Explicitamente dependente da Fase 4 para não ser feature genérica. |  
| \[v1.1\] | "Documentos anexados como base pessoal" considerado e rejeitado | Ideia inicial de fornecer contexto sobre o usuário via PDF anexado. Rejeitada em discussão: documento estático degrada rapidamente, gera fricção de manutenção, e não escala. Memória persistente automática (Fase 4\) cobre o mesmo problema com abordagem sustentável. |

\---

\#\# 7\. Sobre "sem pressa"

O projeto é pessoal, sem prazo externo. Isso é uma vantagem estratégica se usada com disciplina.

\*\*"Sem pressa" significa:\*\*  
\- Fazer certo antes de fazer rápido  
\- Documentar decisões enquanto o contexto está fresco  
\- Recusar features que não fazem sentido só porque são fáceis  
\- Aceitar que uma fase leve o tempo que precisar

\*\*"Sem pressa" NÃO significa:\*\*  
\- Deixar o projeto parado  
\- Adiar decisões estruturais  
\- Coletar ideias sem executar nada  
\- Reescrever tudo a cada iteração

O ritmo saudável é: \*\*entregar algo pequeno, medir, aprender, decidir o próximo passo\*\*. Sempre.  
\`\`\`

\---

\*\*O que mudou nessa versão (v1.1):\*\*

\*\*1. Marquei todos os itens da Seção 2 como concluídos ✅\*\*  
Toda a documentação de fundação foi produzida. O critério de saída foi atingido.

\*\*2. Reorganizei o Parking Lot em ordem de dependência lógica\*\*  
Antes as "fases potenciais" ficavam misturadas com ideias avulsas. Agora as Fases 2, 3, 4 e 5 aparecem em sequência lógica no topo do parking lot, com dependências explícitas. As ideias avulsas (barge-in, code-switching, etc.) ficam depois.

\*\*3. "Persistência de sessão" virou "Fase 3 potencial" formalmente\*\*  
Antes era item avulso. Agora está numerada, com "Depende de" e "Habilita" claros. Isso fecha o ciclo de dependência para chegar na Fase 4\.

\*\*4. Fase 4 (Memória persistente) adicionada integralmente\*\*  
Com todos os 6 casos, exclusões conscientes, abordagem Variante 1.5 mencionada, dependências e riscos previstos.

\*\*5. Fase 5 (Integrações externas) adicionada como consequência natural\*\*  
Materializa o valor da Fase 4 no caso concreto do WhatsApp que originou a discussão. Deixa claro que é \*\*dependente\*\* da Fase 4 — não pode ser antecipada sem virar feature genérica ruim.

\*\*6. Seção "Ideias descartadas" expandida com contexto\*\*  
Adicionei explicitamente:  
\- Documentos anexados como base pessoal (rejeitado)  
\- Padrões inferidos automaticamente (excluído do escopo da Fase 4\)  
\- Compromissos com data (Google Calendar resolve)  
\- Rotinas proativas (fora do escopo)

Todas com \*\*motivo\*\* — para evitar que voltem ao radar sem informação nova.

\*\*7. Nota importante na Ideias descartadas sobre Compose vs Fase 5\*\*  
Marquei explicitamente que \*\*compose de emails/SMS (ADR-005, rejeitado)\*\* é diferente de \*\*Fase 5 (responder mensagens recebidas)\*\*. Ambos envolvem "mensagens", mas são coisas técnica e conceitualmente distintas. Sem essa nota, alguém poderia confundir e reabrir ADR-005 achando que estava sendo revertida.

\*\*8. Seção 6 "Histórico de decisões" ganhou três entradas novas \[v1.1\]\*\*  
\- Fase 4 mapeada  
\- Fase 5 mapeada  
\- Documentos anexados rejeitados

Isso preserva a memória da nossa conversa como decisão histórica registrada.

\*\*9. Versão bumpada para 1.1\*\*  
Como convenção, mudança material no roadmap merece bump de versão. v2.0 seria reserva para reestruturação grande.

\---

\*\*Onde estamos agora:\*\*

✅ README.md  
✅ ARCHITECTURE.md  
✅ RISKS.md v1  
✅ \*\*ROADMAP.md v1.1\*\* (atualizado agora)  
✅ ADR-001 a ADR-005  
✅ TESTING.md v1  
✅ GLOSSARY.md

Fundação \*\*completa e atualizada\*\*. Você tem uma trilha clara:

\- \*\*Fase 1\*\* (agora): multimodalidade com foto  
\- \*\*Fase 2\*\*: modo professor com PDFs  
\- \*\*Fase 3\*\*: persistência de sessão  
\- \*\*Fase 4\*\*: memória persistente estruturada  
\- \*\*Fase 5\*\*: integrações externas com contexto

E o mais importante: \*\*cada fase tem racional documentado\*\*, \*\*cortes conscientes registrados\*\*, e \*\*dependências explícitas\*\*. Se em 3 meses você quiser pular direto pra Fase 5, o documento te lembra por que não faz sentido.

Bom trabalho consolidando essa base. 🎯

Nota rápida: você escreveu "ASRs" mas pelo contexto é ADRs (Architectural Decision Records). Vou produzir os 5 em sequência. Cada um é intencionalmente \*\*curto\*\* — ADR longo é ADR não lido.

\---

\`\`\`markdown:docs/decisions/ADR-001-streaming-tts-por-sentenca.md  
\# ADR-001 — Streaming de TTS por sentença

\*\*Data:\*\* \[data original da decisão\]  
\*\*Status:\*\* Aceita  
\*\*Contexto:\*\* Kabem Voice — pipeline de resposta por voz

\---

\#\# Contexto

O fluxo natural de um LLM é: recebe prompt → gera tokens em streaming → resposta completa. Em interfaces de texto, isso é aceitável porque o usuário lê os tokens conforme aparecem.

Em interface de voz, esperar a resposta completa antes de começar o TTS gera um silêncio morto de 2-5 segundos entre a pergunta e a primeira palavra do áudio. Esse gap é o principal fator que faz voice chats parecerem "app lento" em vez de "conversa natural".

Em modo "ligação" — que é a proposta central do Kabem Voice — esse silêncio é fatal para a percepção de qualidade.

\#\# Decisão

O TTS começa a falar assim que o modelo produz uma \*\*sentença completa\*\* (detectada por \`.\`, \`\!\`, \`?\` ou quebra de parágrafo), sem esperar o fim da geração.

Um buffer acumula tokens até detectar fim de sentença. Sentença fechada é enviada imediatamente ao TTS. O modelo continua gerando a próxima sentença enquanto a primeira toca.

Regras de proteção do buffer:  
\- Sentença deve ter tamanho mínimo (\~30 caracteres) para evitar fragmentos tipo "Sim."  
\- Sentença que termina em número seguido de ponto (ex: "1.") aguarda mais tokens — evita cortar listas

\#\# Consequências

\*\*Positivas:\*\*  
\- Latência percebida cai de 3-5s para \< 1s no primeiro áudio  
\- Sensação de conversa em tempo real preservada  
\- Pré-requisito natural para barge-in (interrupção durante fala do assistente)

\*\*Negativas:\*\*  
\- Complexidade adicional no \`VoiceChatManager\` (buffer \+ heurísticas de corte)  
\- Casos de borda com listas numeradas exigem tratamento explícito  
\- Se o modelo interromper a geração no meio de uma sentença longa, TTS pode parar em ponto estranho

\#\# Alternativas consideradas

\*\*Esperar resposta completa antes de TTS\*\*  
Rejeitada — latência de 3-5s inviabiliza a proposta de "ligação" em tempo real.

\*\*Streaming por token diretamente ao TTS\*\*  
Rejeitada — TTS não consegue prosódia natural em tokens isolados. Resultado seria robótico e com pausas erradas.

\*\*Streaming por chunk fixo (a cada N tokens)\*\*  
Rejeitada — cortes no meio de frase geram entonação errada. Fim de sentença é o ponto natural de pausa em qualquer idioma.

\#\# Referências

\- \`VoiceChatManager.kt\` — implementação  
\- \`ARCHITECTURE.md\` seção 4 passo 5 — descrição do fluxo  
\`\`\`

\---

\`\`\`markdown:docs/decisions/ADR-002-camada-zero-contexto-anterior.md  
\# ADR-002 — Camada 0: contexto do turno anterior

\*\*Data:\*\* \[data original da decisão\]  
\*\*Status:\*\* Aceita  
\*\*Contexto:\*\* Kabem Voice — classificador de intenção

\---

\#\# Contexto

O classificador de intenção usa regex/keywords para detectar em qual dos 4 perfis (FLASH, DETAILED, STEP\_BY\_STEP, STRATEGIC) a fala do usuário se encaixa.

Em conversação natural PT-BR, uma parcela significativa dos turnos é \*\*elíptica\*\* — depende do turno anterior para fazer sentido:

\- "mais"  
\- "continua"  
\- "aprofunda nisso"  
\- "e depois?"  
\- "não entendi, explica de novo"

Sozinhas, essas frases não batem em nenhum regex de perfil. Sem tratamento especial, todas cairiam em FLASH (o default), resultando em respostas curtas quando o usuário claramente quer continuidade ou aprofundamento.

\#\# Decisão

Antes de aplicar a classificação por regex (Camada 1), o classificador consulta o \*\*turno anterior\*\*:

\- Qual perfil foi usado  
\- Um resumo curto da resposta

Se a fala atual é uma continuação elíptica, o classificador \*\*herda ou escala\*\* o perfil anterior:

\- Sinal \`CONTINUE\` ("mais", "e depois") → mantém perfil, instrução adiciona "continue de onde parou"  
\- Sinal \`DEEPEN\` ("aprofunda", "explica melhor") → escala um nível na hierarquia FLASH → DETAILED → STEP\_BY\_STEP → STRATEGIC  
\- Sinal \`SHORTEN\` ("resume", "em resumo") → desce um nível  
\- Sinal \`CLARIFY\` ("não entendi") → mantém perfil, instrução adiciona "reformule de outra maneira"

Se a fala tem sujeito próprio (não é elíptica), Camada 0 é bypassed e Camada 1 (regex) roda normalmente.

\#\# Consequências

\*\*Positivas:\*\*  
\- Continuações naturais funcionam sem palavras-gatilho explícitas de perfil  
\- Cobre uma parcela significativa dos turnos de conversação real em PT-BR  
\- Baixo custo: só carrega último perfil \+ resumo curto em memória

\*\*Negativas:\*\*  
\- Estado adicional a ser mantido no \`VoiceViewModel\`  
\- Ambiguidade em turnos limítrofes: "então" pode ser continuação ou início de tópico novo  
\- Sem decaimento temporal, "mais" 30 minutos depois herda perfil velho — mitigação pendente

\#\# Alternativas consideradas

\*\*Ignorar continuações — sempre reclassificar do zero\*\*  
Rejeitada — sacrifica a proposta central de conversação natural. Todo "mais" cairia em FLASH.

\*\*Usar o próprio LLM para classificar intenção (segunda chamada ao modelo)\*\*  
Rejeitada — custo de latência inaceitável em cada turno. Regex \+ contexto é ordens de magnitude mais rápido.

\*\*Manter janela deslizante de N turnos anteriores em vez de só o último\*\*  
Considerada mas adiada — complexidade adicional sem benefício claro no cenário atual. Pode voltar se dados mostrarem falhas em continuações multi-turno.

\#\# Pendências relacionadas

\- Adicionar decaimento temporal: se \`Δt\` desde turno anterior \> 30s, resetar contexto (evita herdar perfil velho após pausa longa)  
\- Ver \`RISKS.md\` R06 para ambiguidade em desempate

\#\# Referências

\- \`IntentClassifier.kt\` — implementação  
\- \`ARCHITECTURE.md\` seção 5 — as 4 camadas conceituais  
\`\`\`

\---

\`\`\`markdown:docs/decisions/ADR-003-quatro-perfis-de-resposta.md  
\# ADR-003 — Quatro perfis de resposta

\*\*Data:\*\* \[data original da decisão\]  
\*\*Status:\*\* Aceita  
\*\*Contexto:\*\* Kabem Voice — classificador de intenção

\---

\#\# Contexto

A proposta de "profundidade dinâmica de resposta" exige categorizar a intenção do usuário em N buckets, cada um com estrutura de resposta característica.

O número de buckets é uma decisão de design não trivial:  
\- \*\*Poucos buckets\*\* (2-3): fácil de classificar, mas força respostas de estruturas diferentes no mesmo balde. Um tutorial e um plano estratégico teriam a mesma estrutura falada.  
\- \*\*Muitos buckets\*\* (6+): granularidade alta, mas classificação vira loteria e usuário perde previsibilidade.

Havia proposta inicial de fundir "tutorial" e "estratégico" em um único bucket de "resposta longa/estruturada". Foi rejeitada.

\#\# Decisão

Quatro perfis, intencionalmente distintos:

| Perfil | Estrutura falada característica |  
|---|---|  
| \*\*FLASH\*\* | Até 2 frases, direta. Default. |  
| \*\*DETAILED\*\* | Um parágrafo conceitual didático. |  
| \*\*STEP\_BY\_STEP\*\* | Etapas com conectivos ("Primeiro...", "Depois...", "Por fim..."). |  
| \*\*STRATEGIC\*\* | Blocos analíticos ("Vamos por três dimensões..."). |

\`STEP\_BY\_STEP\` e \`STRATEGIC\` são separados porque \*\*têm prosódia falada diferente\*\*. Um TTS competente renderiza cada um com cadência distinta — sequencial no tutorial, analítica no plano. Fundi-los sacrifica essa diferenciação.

\`FLASH\` é o default absoluto quando nenhum gatilho é detectado. Decisão consciente: melhor errar para o lado curto (usuário pede mais → Camada 0 escala) do que para o lado longo (usuário se cansa e reformula).

\#\# Consequências

\*\*Positivas:\*\*  
\- Estruturas de resposta suficientemente distintas para serem perceptíveis ao usuário  
\- Cada perfil tem instrução invisível dedicada, otimizável independentemente  
\- Camada 0 tem hierarquia natural de escalada (FLASH → DETAILED → STEP\_BY\_STEP → STRATEGIC)

\*\*Negativas:\*\*  
\- Regra de desempate necessária quando gatilhos de múltiplos perfis aparecem  
\- Casos ambíguos em PT-BR ("explica rápido" tem gatilhos de DETAILED e FLASH)  
\- Falsos positivos em bordas requerem matriz de testes formal (pendente)

\#\# Alternativas consideradas

\*\*Dois perfis (curto / longo)\*\*  
Rejeitada — perde granularidade. Um "curto" cobre saudação e sumário. Um "longo" cobre tutorial, análise e explicação. Sacrifica prosódia específica.

\*\*Três perfis (fundindo STEP\_BY\_STEP e STRATEGIC)\*\*  
Rejeitada — perde a diferenciação de cadência falada entre sequencial e analítica.

\*\*Seis perfis (adicionando "criativo", "técnico")\*\*  
Rejeitada — granularidade alta demais para regex. Classificação erraria com frequência, degradando confiança.

\*\*Perfis dinâmicos definidos pelo próprio LLM\*\*  
Rejeitada — inviável em um único turno sem segunda chamada. Poderia ser útil em v2 se performance permitir.

\#\# Regra de desempate

Se múltiplos perfis pontuam na Camada 1:

1\. Modificadores explícitos de brevidade ("rápido", "curto", "resumido") forçam FLASH  
2\. Caso contrário, perfil de maior complexidade vence: STRATEGIC \> STEP\_BY\_STEP \> DETAILED \> FLASH

Racional: gatilhos de complexidade alta são raros e específicos. Se aparecem, provavelmente são intencionais. Modificadores de brevidade sobrescrevem porque expressam intenção direta e explícita.

\#\# Referências

\- \`Perfil.kt\` — enum e instruções por perfil  
\- \`IntentClassifier.kt\` — regex e regra de desempate  
\- \`docs/TESTING.md\` — matriz de casos ambíguos (pendente)  
\`\`\`

\---

\`\`\`markdown:docs/decisions/ADR-004-injecao-invisivel-por-turno.md  
\# ADR-004 — Injeção de instrução invisível por turno

\*\*Data:\*\* \[data original da decisão\]  
\*\*Status:\*\* Aceita  
\*\*Contexto:\*\* Kabem Voice — como o perfil detectado influencia a resposta

\---

\#\# Contexto

Uma vez detectado o perfil da fala do usuário (FLASH, DETAILED, etc.), o modelo precisa \*\*saber disso\*\* para adaptar a resposta.

Existem três formas técnicas de fazer isso com LiteRT-LM:

1\. \*\*Recarregar o system prompt\*\* com instruções específicas do perfil detectado  
2\. \*\*Injetar a instrução como prefixo\*\* na mensagem do usuário, invisível na UI  
3\. \*\*Manter estado de perfil no cliente\*\* e pós-processar a resposta

Cada abordagem tem trade-offs de latência, complexidade e correção.

\#\# Decisão

\*\*Prefixação invisível por turno.\*\* A mensagem enviada ao modelo tem o formato:

\`\`\`  
\[INSTRUÇÃO INTERNA: {instrução do perfil detectado}\] {texto transcrito do usuário}  
\`\`\`

A parte \`\[INSTRUÇÃO INTERNA: ...\]\` não aparece na UI, mas é vista pelo modelo como parte do turno atual.

O system prompt do modelo \*\*permanece constante\*\* ao longo da sessão. Cada turno só varia o prefixo da mensagem do usuário.

\#\# Consequências

\*\*Positivas:\*\*  
\- Aproveita o cache KV do modelo — system prompt não precisa ser reprocessado a cada turno  
\- Latência de prefill mantida baixa (\~300ms típico)  
\- Perfil pode mudar de um turno para o outro sem reload  
\- Reversível por natureza — não polui histórico persistente

\*\*Negativas:\*\*  
\- O modelo "vê" uma instrução prefixada que o usuário não digitou. Se o modelo for treinado a comentar sobre a instrução ("Como você me pediu para explicar em etapas..."), quebra a ilusão. Mitigação: instrução redigida para não convidar meta-comentário.  
\- Todo turno paga o custo (baixo) de reprocessar o prefixo  
\- Se a instrução é mal formulada, o comportamento se degrada silenciosamente

\#\# Alternativas consideradas

\*\*Recarregar system prompt com instruções específicas do perfil\*\*  
Rejeitada — invalida cache KV. Cada turno paga custo de reprocessar todo o system prompt, gerando latência adicional de centenas de ms a segundos. Inaceitável para modo "ligação".

\*\*Manter estado de perfil no cliente e pós-processar resposta\*\*  
Rejeitada — pós-processar resposta de LLM é frágil. Truncar ou reformatar output em runtime perde qualidade de raciocínio do modelo. Além disso, não muda a estrutura interna da resposta, só a apresentação.

\*\*Fine-tune do modelo para responder por perfil\*\*  
Considerada mas rejeitada por hora — custo alto, dependência de infraestrutura de treino, e prende o app a modelos específicos. Prefixação funciona com qualquer Gemma sem retrabalho.

\#\# Racional adicional sobre cache KV

O ganho de manter o system prompt constante é grande com Gemma 4\. O prefill inicial de um system prompt de \~500 tokens pode custar \~300-500ms no Poco X7 Pro. Se cada turno reprocessasse esse prefill, a latência inicial de resposta subiria de \~1s para \~1.5s — mudando qualitativamente a percepção de "ligação" para "atraso".

\#\# Referências

\- \`VoiceViewModel.kt\` — montagem do prompt final  
\- \`ARCHITECTURE.md\` seção 4 passo 3 — descrição do fluxo  
\- ADR-003 — origem das instruções por perfil  
\`\`\`

\---

\`\`\`markdown:docs/decisions/ADR-005-compose-email-fora-de-escopo.md  
\# ADR-005 — Composição de mensagens e emails fora de escopo (por enquanto)

\*\*Data:\*\* \[data original da decisão\]  
\*\*Status:\*\* Aceita  
\*\*Contexto:\*\* Kabem Voice — definição de escopo do roadmap

\---

\#\# Contexto

Durante a fase de exploração de features, foi considerada uma linha de trabalho onde o assistente redige emails e mensagens de texto por comando de voz, com um fluxo de aprovação antes do envio.

Exemplo do caso de uso: "manda um email pro meu chefe dizendo que vou chegar atrasado" → assistente gera rascunho → usuário aprova por voz → envio.

A feature é atrativa e tecnicamente viável (o Gallery base tem \`MobileActions\` via FunctionGemma 270M \+ Intents nativos). Mas foi cortada do roadmap atual.

\#\# Decisão

Composição de mensagens e emails \*\*não faz parte do escopo do Kabem Voice\*\*. Se um dia entrar, exige ADR de reversão explícita.

Uma vez fora de escopo, todos os componentes relacionados também ficam fora:  
\- \`COMPOSE\_MODE\` no IntentRouter (não implementar)  
\- Carregamento paralelo de FunctionGemma 270M (não pré-configurar)  
\- \`ApprovalScreen\` e fila de drafts (não desenhar)  
\- Aprovação por voz ("pode enviar") (não modelar)

\#\# Racional

Três razões, em ordem de peso:

\*\*1. Risco crítico de execução incorreta\*\*  
Aprovação por voz é sujeita a erro de ASR. "Não" pode ser confundido com "nossa\!". "Cancela" pode ser confundido com contexto adjacente. Envio de email errado ao destinatário errado tem consequência que não se desfaz. Mitigação exige confirmação dupla que quebra a fluidez de conversa — anti-clímax da proposta central do app.

\*\*2. Complexidade arquitetural desproporcional\*\*  
A feature exige: segundo modelo em memória (FunctionGemma 270M), router de modo operacional, tela de aprovação, fila de drafts, integração com Intent do Android, e disciplina de fluxo estrito. Isso é essencialmente um segundo produto dentro do app. Para um projeto de time pequeno sem pressa, comprometer sprints com essa complexidade antes de validar o núcleo é otimismo prematuro.

\*\*3. Não é o núcleo da proposta de valor\*\*  
A proposta do Kabem Voice é \*\*conversação natural com profundidade adaptativa\*\*. Ação nativa (mandar email) é uma feature comum em assistentes de voz, mas não é o diferencial. Se o núcleo estiver bem resolvido, a ausência dessa feature não descaracteriza o produto. Se o núcleo não estiver bem resolvido, adicionar composição só distrai do problema real.

\#\# Consequências

\*\*Positivas:\*\*  
\- Foco preservado no núcleo (conversação \+ multimodalidade)  
\- Risco crítico R05 fica arquivado  
\- Sprints da Fase 1 e potencial Fase 2 permanecem enxutos  
\- Decisão registrada — não volta ao radar por sugestão casual

\*\*Negativas:\*\*  
\- Usuários virão a pedir a feature  
\- Comparação com assistentes que fazem isso ficará desfavorável no aspecto específico  
\- Se um dia a feature voltar, o retrabalho não é trivial

\#\# Alternativas consideradas

\*\*Implementar com aprovação por voz e assumir o risco\*\*  
Rejeitada — o risco de envio errado é assimétrico. Uma falha (email errado ao chefe) tem custo desproporcional aos ganhos de fluidez.

\*\*Implementar com aprovação obrigatoriamente por toque na tela\*\*  
Considerada mais viável, mas ainda é grande investimento arquitetural. Se a feature voltar, provavelmente será por esse caminho.

\*\*Delegar a Skills / Native Actions do Gallery base sem integração com Live Voice\*\*  
Válida — o Gallery já tem \`send\_email\` como Skill nativa. Usuário pode acessar por lá sem que o Kabem Voice se meta. Isso é o comportamento default hoje.

\#\# Condições para reverter esta decisão

Reabrir esta discussão faz sentido se:  
\- Feedback consistente de usuários reais indicar que a ausência é um bloqueio de adoção  
\- Tecnologia de confirmação por voz amadurecer a ponto de reduzir R05 significativamente  
\- Núcleo do app estiver maduro e estável, com room para expansão

Enquanto essas condições não estiverem presentes, esta ADR permanece em vigor.

\#\# Referências

\- \`RISKS.md\` R05 — Aprovação por voz mal interpretada (arquivado)  
\- \`ROADMAP.md\` seção 4 — Parking lot / Ideias descartadas  
\- Function\_Calling\_Guide.md do Gallery — implementação técnica que ficaria disponível se a feature voltasse  
\`\`\`

\---

\*\*Comentários finais sobre os 5 ADRs:\*\*

\*\*Padrão que segui em todos:\*\*

\- \*\*Contexto → Decisão → Consequências → Alternativas\*\* (ordem clássica do ADR)  
\- \*\*Consequências positivas E negativas\*\* — se um ADR só lista positivas, é propaganda, não decisão registrada  
\- \*\*Alternativas rejeitadas com justificativa curta\*\* — isso é o que impede a decisão de ser reaberta sem informação nova  
\- \*\*Referências para código e outros docs\*\* — ADR conecta com o resto do sistema de documentação

\*\*O que fiz de diferente no ADR-005:\*\*

\- \*\*Seção "Condições para reverter"\*\* — quando uma decisão é "por enquanto não", é útil registrar o que faria a decisão mudar. Isso evita que a reversão dependa de humor ou pressão externa.  
\- \*\*Racional numerado por peso\*\* — ao invés de lista solta, ordenei os motivos por importância. Ajuda o leitor futuro a entender qual é o motivo dominante.

\*\*O que você provavelmente vai querer ajustar:\*\*

\- \*\*Datas reais\*\* de quando cada decisão foi tomada (deixei placeholders)  
\- Se algum nome de arquivo (\`IntentClassifier.kt\`, \`Perfil.kt\`, \`VoiceChatManager.kt\`) não bate com a estrutura real do seu código, ajustar nas seções "Referências"  
\- Se o ADR-002 (Camada 0\) já tem decaimento temporal implementado (não sabia), remover a menção como "pendente"

**\# Glossário — Kabem Voice**

**\*\*Versão:\*\*** 1.0  
**\*\*Última atualização:\*\*** \[data do commit\]

\---

**\#\# Propósito**

Manter um vocabulário comum entre docs, código e conversas. Termos que aparecem em \`ARCHITECTURE.md\`, \`RISKS.md\`, ADRs e no código Kotlin devem estar definidos aqui uma única vez.

**\*\*Regra de manutenção:\*\*** se um termo aparece em mais de um documento com definição ambígua, ele deve entrar aqui. Se um termo entra aqui e some dos documentos, é candidato à remoção.

**\*\*Ordem:\*\*** alfabética por termo em português. Termos em inglês só quando não há tradução consolidada no projeto.

\---

**\#\# A**

**\#\#\# ADR (Architectural Decision Record)**  
Documento curto que registra uma decisão arquitetural com contexto, alternativas consideradas e consequências. Vive em \`docs/decisions/\`. Um ADR nunca é editado depois de aceito — se a decisão muda, um novo ADR supersedes o anterior.

**\#\#\# Anexo**  
Conteúdo não-textual (foto, PDF, áudio) fornecido pelo usuário como parte de um turno. Na Fase 1 do roadmap, apenas fotos são suportadas.

**\#\#\# Anexo ativo**  
Anexo cujos tokens ainda estão presentes no context do modelo. Diferente de anexo "arquivado", que teve seu conteúdo sumarizado e substituído por um resumo curto para economizar context.

**\#\#\# ASR (Automatic Speech Recognition)**  
Conversão de fala em texto. No Kabem Voice, o ASR é local (não depende de nuvem) e roda antes do texto chegar ao classificador de intenção. Em atividades de repetição/pronúncia em inglês, o locale do reconhecedor é forçado para o dialeto inglês selecionado para não interpretar a tentativa do aluno como português.

\---

**\#\# B**

**\#\#\# Barge-in**  
Capacidade do usuário interromper o assistente enquanto ele está falando. Não implementado hoje. Se implementado, exige que o TTS pare imediatamente ao detectar voz do usuário e que a fala interruptora seja processada como novo turno.

**\#\#\# Buffer de streaming**  
Estrutura interna do \`VoiceChatManager\` que acumula tokens gerados pelo modelo até detectar fim de sentença. Governa quando um pedaço de resposta é enviado ao TTS.

\---

**\#\# C**

**\#\#\# Camada 0**  
Estágio do classificador de intenção que consulta o turno anterior antes de aplicar regex. Trata continuações elípticas ("mais", "aprofunda") herdando ou escalando o perfil anterior. Ver ADR-002.

**\#\#\# Camada 1**  
Estágio do classificador de intenção que aplica regex/keywords para detectar perfil na fala atual. Executa depois da Camada 0\.

**\#\#\# Camada 2**  
Estágio que monta o prompt final com a instrução invisível prefixada ao texto do usuário. Não é reload de system prompt. Ver ADR-004.

**\#\#\# Camada 3**  
Estágio que faz streaming do output do modelo para o TTS por sentença. Vive no \`VoiceChatManager\`. Ver ADR-001.

**\#\#\# Classificador de intenção**  
Componente em Kotlin que decide o perfil de resposta a partir da fala do usuário. Combina Camada 0 (contexto anterior) \+ Camada 1 (regex) \+ regra de desempate. Roda em \~milissegundos, não faz chamada ao modelo.

**\#\#\# Continuação elíptica**  
Fala que depende do turno anterior para fazer sentido. Ex: "mais", "continua", "aprofunda", "e depois?". Cobertas pela Camada 0\.

**\#\#\# Context (janela de contexto)**  
Conjunto de tokens que o modelo processa por turno — inclui system prompt, histórico, anexos ativos e turno atual. Configurado em 32K tokens no Kabem Voice, embora o Gemma 4 suporte arquiteturalmente até 128K.

**\#\#\# ContextBudgetManager**  
Componente planejado (não implementado) que monitora o consumo de context e dispara sumarização automática de anexos e histórico quando atinge gatilhos definidos. Pré-requisito para Fase 2 (PDFs).

\---

**\#\# D**

**\#\#\# DETAILED**  
Um dos 4 perfis. Fala didática em um parágrafo conceitual. Gatilhos típicos: "me explica", "como funciona", "por que". Ver ADR-003.

**\#\#\# Downgrade automático**  
Estratégia de mitigação de throttling térmico: trocar Gemma-4-E4B por Gemma-4-E2B (ou reduzir aceleração) quando temperatura do device sobe. Planejado, não implementado.

\---

**\#\# F**

**\#\#\# Fallback manual**  
Menu discreto que permite ao usuário forçar um perfil de resposta quando o classificador erra. Não visível por padrão. Escape hatch, não interface principal.

**\#\#\# Fase 1**  
Marco de roadmap: aceitar uma foto por turno como anexo, provar stack multimodal. Escopo intencionalmente mínimo.

**\#\#\# Fase 2**  
Marco de roadmap potencial: modo professor com PDFs. Depende de aprendizados da Fase 1\.

**\#\#\# FLASH**  
Um dos 4 perfis. Resposta curta, até 2 frases. Default absoluto quando nenhum gatilho é detectado. Gatilhos típicos: saudações, "que horas", "resuma". Ver ADR-003.

**\#\#\# FunctionGemma 270M**  
Modelo base de 270 milhões de parâmetros derivado de Gemma 3, especializado em function calling. Distribuído pelo Google via LiteRT-LM. No allowlist do Gallery aparece em variantes fine-tunadas: \`MobileActions-270M\` (para ações Android) e \`TinyGarden-270M\` (para o joguinho demo). Kabem Voice **\*\*não usa\*\*** hoje, mas o modelo estava previsto para o COMPOSE*\_MODE que foi cortado (ver ADR-005).*

*\---*

*\#\# G*

*\#\#\# Gemma 4 E2B / E4B*  
*Modelos multimodais do Google DeepMind, \~2.6 GB (E2B) e \~3.7 GB (E4B) em formato \`.litertlm\`. Suportam texto, imagem e áudio. Context arquitetural até 128K; configurado em 32K no Kabem Voice.*

*\#\#\# Gate*  
*Ponto de decisão obrigatório antes de avançar. Ex: "gates de publicação" (LGPD, security, user guide) devem estar cumpridos antes de subir para Play Store. Diferente de sprint — gate bloqueia release, não desenvolvimento.*

*\---*

*\#\# I*

*\#\#\# Instrução invisível*  
*Texto prefixado à mensagem do usuário no formato \`\[INSTRUÇÃO INTERNA: ...\]\`. Enviado ao modelo como parte do turno, não exibido na UI. Mecanismo central para adaptação por perfil sem reload de system prompt. Ver ADR-004.*

*\#\#\# IntentRouter*  
*Componente planejado (não implementado) que decide entre modos operacionais (chat, teach, analyze) antes do classificador de perfil. Diferente do classificador atual, que só decide perfil dentro do modo "chat".*

*\---*

*\#\# L*

*\#\#\# LiteRT-LM*  
*Runtime do Google AI Edge para executar LLMs localmente em Android e iOS. Sucessor do formato \`.task\` do MediaPipe para modelos de linguagem. Kabem Voice usa a API Kotlin do LiteRT-LM.*

*\#\#\# Live Voice*  
*Nome da tela e do modo de conversação por voz contínua do Kabem Voice. Referenciado no código como \`LiveChatScreen\`. Diferente do chat padrão do Gallery.*

*\---*

*\#\# M*

*\#\#\# \`.litertlm\`*  
*Formato de arquivo de modelo do LiteRT-LM. Empacota pesos quantizados, tokenizer e metadados. Substituiu o formato \`.task\` do MediaPipe para LLMs.*

*\#\#\# Modo operacional*  
*Estado de alto nível do assistente. Planejado para Fase 2: TEACH\_*MODE, ANALYZE*\_MODE, CONVERSATION\_*MODE. Diferente de perfil de resposta.

**\#\#\# Multimodalidade**  
Capacidade do modelo processar múltiplos tipos de entrada no mesmo turno (texto \+ imagem, texto \+ áudio). Nativa nos Gemma 4 E2B/E4B.

\---

**\#\# P**

**\#\#\# Perfil (de resposta)**  
Categoria de estrutura de resposta detectada pelo classificador de intenção. Quatro perfis existem hoje: FLASH, DETAILED, STEP*\_BY\_*STEP, STRATEGIC. Ver ADR-003.

**\#\#\# Perfil default**  
FLASH. Aplicado quando nenhum gatilho de outro perfil é detectado. Escolha consciente: melhor errar para o lado curto do que para o lado longo.

**\#\#\# Poco X7 Pro**  
Hardware alvo do Kabem Voice na fase alpha. SoC Dimensity 8400, 8-12 GB RAM. Todas as decisões arquiteturais e mitigações de risco assumem este dispositivo como piso.

**\#\#\# Prefill**  
Primeira fase da inferência do LLM, quando os tokens do prompt são processados antes da geração começar. Latência do prefill impacta o "time to first token", que é o gargalo perceptível em UX de voz.

\---

**\#\# R**

**\#\#\# Regra de desempate**  
Lógica aplicada quando gatilhos de múltiplos perfis pontuam na Camada 1:  
1\. Modificadores explícitos de brevidade ("rápido", "curto") forçam FLASH  
2\. Caso contrário, perfil de maior complexidade vence (STRATEGIC \> STEP*\_BY\_*STEP \> DETAILED \> FLASH)

Ver ADR-003.

\---

**\#\# S**

**\#\#\# Sinal (na Camada 0\)**  
Tipo de continuação detectado em turno elíptico. Quatro sinais reconhecidos: \`CONTINUE\` (mantém perfil), \`DEEPEN\` (escala nível), \`SHORTEN\` (desce nível), \`CLARIFY\` (mantém perfil, pede reformulação). Ver ADR-002.

**\#\#\# Sprint**  
Unidade de trabalho no roadmap. Sem duração fixa no Kabem Voice — "sprint" aqui significa "próximo bloco coerente de entrega", não "duas semanas de calendário".

**\#\#\# STEP*\_BY\_*STEP**  
Um dos 4 perfis. Etapas curtas com conectivos ("Primeiro...", "Depois...", "Por fim..."). Gatilhos típicos: "passo a passo", "tutorial", "me ensina". Ver ADR-003.

**\#\#\# STRATEGIC**  
Um dos 4 perfis. Análise em blocos conceituais. Gatilhos típicos: "plano", "estratégia", "compare", "roteiro". Ver ADR-003.

**\#\#\# Streaming (de TTS)**  
Ver **\*\*Camada 3\*\***. Envio de sentenças ao TTS conforme o modelo gera, sem esperar resposta completa.

**\#\#\# System prompt**  
Instrução constante enviada ao modelo no início da sessão. No Kabem Voice, permanece imutável entre turnos — variação por perfil acontece via instrução invisível, não via reload de system prompt.

**\#\#\# Sumarização**  
Processo de substituir conteúdo antigo no context (anexos, histórico) por um resumo curto para liberar tokens. Planejado no ContextBudgetManager, não implementado.

\---

**\#\# T**

**\#\#\# TTS (Text-to-Speech)**  
Conversão de texto em fala. No Kabem Voice, o TTS recebe sentenças em streaming e as reproduz sequencialmente. Ver Camada 3\.

**\#\#\# Throttling térmico**  
Redução automática de clock do SoC quando o dispositivo aquece. Risco central da experiência em sessões longas. Ver R01 em \`RISKS.md\`.

**\#\#\# Time to first token**  
Tempo entre o fim da fala do usuário e a primeira palavra falada pelo assistente. Métrica crítica de UX no modo "ligação". Meta prática: \< 1 segundo.

**\#\#\# Turno**  
Um ciclo completo de interação: fala do usuário → resposta do assistente. Unidade natural de medida para latência, custo de tokens e histórico de conversa.

**\#\#\# Turno anterior**  
Contexto usado pela Camada 0\. Contém o perfil que foi usado e um resumo curto da última resposta. Persistido em memória durante a sessão, descartado ao fechar o app.

\---

**\#\# V**

**\#\#\# VAD (Voice Activity Detection)**  
Detecção de quando o usuário terminou de falar. No Kabem Voice, o VAD dispara o processamento do turno quando detecta silêncio após a fala.

**\#\#\# VoiceChatManager**  
Componente Kotlin que orquestra a inferência do modelo e o streaming ao TTS. Vive na camada mais próxima do LiteRT-LM.

**\#\#\# VoiceViewModel**  
Componente Kotlin que orquestra a lógica do turno em nível de UI: captura de ASR, chamada ao classificador, montagem do prompt, entrega ao \`VoiceChatManager\`. Ponto central de coordenação.

\---

**\#\# Termos que \*\*não\*\* entram aqui**

Para manter o glossário útil:

\- Termos triviais de Android/Kotlin (Activity, ViewModel, Intent) — assumidos como conhecidos  
\- Termos padrão de LLM (token, temperature, top-k) — buscar em referências externas  
\- Nomes de arquivos específicos — vão no \`ARCHITECTURE.md\` seção 9  
\- Detalhes de implementação — vão em comentários de código

Se um termo desses gerar confusão repetidamente, aí sim entra. 

\`\`\`markdown:docs/TESTING.md  
\# Testing — Kabem Voice

\*\*Versão:\*\* 1.0  
\*\*Última atualização:\*\* \[data do commit\]  
\*\*Escopo:\*\* matriz de casos de teste do classificador de intenção \+ cenários multimodais planejados.

\---

\#\# 1\. Propósito

Este documento é o \*\*oráculo comportamental\*\* do Kabem Voice. Para cada entrada de teste listada, existe um comportamento esperado. Se o app diverge, é regressão — não interpretação.

Serve a dois usos:

1\. \*\*Validação manual\*\* — rodar os casos periodicamente após mudanças no classificador ou nos modelos  
2\. \*\*Base para automação futura\*\* — quando existir CI, estes casos viram testes unitários

\*\*O que NÃO é este documento:\*\*  
\- Não é lista exaustiva. É lista \*\*crítica\*\*: casos que se falharem revelam algo importante.  
\- Não substitui uso real do app. Uso real revela casos que planejamento não prevê.  
\- Não testa qualidade da resposta do LLM (isso é subjetivo). Testa se a \*\*estrutura\*\* e o \*\*perfil detectado\*\* batem com o esperado.

\---

\#\# 2\. Convenções

\*\*Como ler uma linha da matriz:\*\*

| Entrada (fala do usuário) | Contexto turno anterior | Perfil esperado | Racional |

\- \*\*Entrada\*\* — transcrição textual do que o usuário fala  
\- \*\*Contexto turno anterior\*\* — perfil usado no turno anterior \+ resumo curto (ou "—" se início de sessão)  
\- \*\*Perfil esperado\*\* — FLASH, DETAILED, STEP\_BY\_STEP, STRATEGIC  
\- \*\*Racional\*\* — por que esse é o perfil correto; ajuda a debugar se o classificador errar

\*\*Convenção de status:\*\*  
\- ✅ Testado e passa  
\- ⚠️ Testado e falha (regressão conhecida)  
\- ⬜ Ainda não testado

\---

\#\# 3\. Matriz do classificador — casos canônicos

Estes são os casos "livro texto". Cada perfil tem gatilhos óbvios, e o classificador deve acertá-los com facilidade. Se algum aqui falhar, o classificador está quebrado — não é caso de borda, é caso base.

\#\#\# 3.1 FLASH — casos canônicos

| \# | Entrada | Contexto | Esperado | Status |  
|---|---|---|---|---|  
| F01 | "que horas são" | — | FLASH | ⬜ |  
| F02 | "bom dia" | — | FLASH | ⬜ |  
| F03 | "onde fica a padaria mais próxima" | — | FLASH | ⬜ |  
| F04 | "resume isso pra mim" | qualquer | FLASH | ⬜ |  
| F05 | "o que é inflação" | — | FLASH | ⬜ |  
| F06 | "quem descobriu o Brasil" | — | FLASH | ⬜ |  
| F07 | "tô com fome" | — | FLASH | ⬜ |  
| F08 | "obrigado" | — | FLASH | ⬜ |

\*\*Racional geral:\*\* perguntas factuais curtas, saudações e comandos de brevidade. O default do sistema é FLASH — esses casos devem cair aqui sem esforço.

\#\#\# 3.2 DETAILED — casos canônicos

| \# | Entrada | Contexto | Esperado | Status |  
|---|---|---|---|---|  
| D01 | "me explica como funciona um transistor" | — | DETAILED | ⬜ |  
| D02 | "por que o céu é azul" | — | DETAILED | ⬜ |  
| D03 | "como a fotossíntese acontece" | — | DETAILED | ⬜ |  
| D04 | "explica a diferença entre HTTP e HTTPS" | — | DETAILED | ⬜ |  
| D05 | "me diga como o cérebro processa a linguagem" | — | DETAILED | ⬜ |

\*\*Racional geral:\*\* verbos de explicação ("explica", "por que", "como funciona") sem sinalizar sequência ou plano.

\#\#\# 3.3 STEP\_BY\_STEP — casos canônicos

| \# | Entrada | Contexto | Esperado | Status |  
|---|---|---|---|---|  
| S01 | "me ensina passo a passo a fazer pão" | — | STEP\_BY\_STEP | ⬜ |  
| S02 | "tutorial de git básico" | — | STEP\_BY\_STEP | ⬜ |  
| S03 | "quero aprender python do zero" | — | STEP\_BY\_STEP | ⬜ |  
| S04 | "como instalo o android studio" | — | STEP\_BY\_STEP | ⬜ |  
| S05 | "me guia pelo processo de abrir uma empresa" | — | STEP\_BY\_STEP | ⬜ |

\*\*Racional geral:\*\* intenção pedagógica sequencial ("passo a passo", "tutorial", "do zero", "guia").

\#\#\# 3.4 STRATEGIC — casos canônicos

| \# | Entrada | Contexto | Esperado | Status |  
|---|---|---|---|---|  
| T01 | "cria um plano de estudos de inglês pra 6 meses" | — | STRATEGIC | ⬜ |  
| T02 | "analisa os prós e contras de trabalhar remoto" | — | STRATEGIC | ⬜ |  
| T03 | "compara react e vue pra um projeto novo" | — | STRATEGIC | ⬜ |  
| T04 | "monta uma estratégia pra economizar 20% do salário" | — | STRATEGIC | ⬜ |  
| T05 | "avalia se vale a pena eu trocar de carro agora" | — | STRATEGIC | ⬜ |

\*\*Racional geral:\*\* intenção analítica ou de planejamento ("plano", "estratégia", "analise", "compara", "avalia").

\---

\#\# 4\. Matriz do classificador — casos ambíguos

Aqui mora a dificuldade real. Cada entrada tem gatilhos de mais de um perfil. A regra de desempate (ADR-003) deve resolvê-los de forma previsível.

| \# | Entrada | Gatilhos presentes | Esperado | Racional |  
|---|---|---|---|---|  
| A01 | "explica rápido" | DETAILED \+ FLASH | \*\*FLASH\*\* | Modificador de brevidade explícita ("rápido") sobrescreve |  
| A02 | "me ensina python resumidamente" | STEP\_BY\_STEP \+ FLASH | \*\*FLASH\*\* | "resumidamente" força brevidade |  
| A03 | "explica passo a passo" | DETAILED \+ STEP\_BY\_STEP | \*\*STEP\_BY\_STEP\*\* | Sequência é mais específica que explicação genérica |  
| A04 | "plano rápido de emagrecimento" | STRATEGIC \+ FLASH | \*\*FLASH\*\* | "rápido" sobrescreve |  
| A05 | "me explica de forma detalhada" | DETAILED \+ DETAILED | \*\*DETAILED\*\* | Sem ambiguidade real |  
| A06 | "compare rapidamente" | STRATEGIC \+ FLASH | \*\*FLASH\*\* | "rapidamente" força FLASH |  
| A07 | "analise e depois me dê um passo a passo" | STRATEGIC \+ STEP\_BY\_STEP | \*\*STRATEGIC\*\* | Maior complexidade vence quando não há brevidade explícita |  
| A08 | "curto tutorial de docker" | STEP\_BY\_STEP \+ FLASH | \*\*FLASH\*\* | "curto" força brevidade |

\*\*Regra em vigor (ADR-003):\*\*  
1\. Se há modificador de brevidade ("rápido", "curto", "resumido", "rapidamente", "resumidamente") → FLASH  
2\. Caso contrário → perfil de maior complexidade vence: STRATEGIC \> STEP\_BY\_STEP \> DETAILED \> FLASH

\---

\#\# 5\. Matriz do classificador — continuações (Camada 0\)

Estes casos testam a \*\*herança e escalada de perfil\*\* quando a fala do usuário é elíptica (sem gatilho próprio).

\#\#\# 5.1 Sinal CONTINUE — mantém perfil

| \# | Entrada | Contexto anterior | Esperado | Racional |  
|---|---|---|---|---|  
| C01 | "mais" | DETAILED, "explicando fotossíntese" | DETAILED | Continuação do mesmo tema |  
| C02 | "continua" | STEP\_BY\_STEP, "tutorial de git" | STEP\_BY\_STEP | Segue próximos passos |  
| C03 | "e depois?" | STEP\_BY\_STEP, "receita de pão" | STEP\_BY\_STEP | Pede próximo passo |  
| C04 | "e mais o quê" | STRATEGIC, "plano de estudos" | STRATEGIC | Continuação analítica |  
| C05 | "prossiga" | DETAILED, "sobre transistores" | DETAILED | Sinônimo de continuar |

\#\#\# 5.2 Sinal DEEPEN — escala um nível

| \# | Entrada | Contexto anterior | Esperado | Racional |  
|---|---|---|---|---|  
| P01 | "aprofunda nisso" | FLASH, "o que é HTTP" | DETAILED | Sobe FLASH → DETAILED |  
| P02 | "explica melhor" | FLASH, "o que é RAM" | DETAILED | Sobe FLASH → DETAILED |  
| P03 | "me dá mais detalhes" | DETAILED, "sobre fotossíntese" | STEP\_BY\_STEP | Sobe DETAILED → STEP\_BY\_STEP |  
| P04 | "detalha mais" | STEP\_BY\_STEP, "instalar node" | STRATEGIC | Sobe STEP\_BY\_STEP → STRATEGIC |  
| P05 | "detalha ainda mais" | STRATEGIC, "plano de estudos" | STRATEGIC | Não escala além de STRATEGIC (já é máximo) |

\#\#\# 5.3 Sinal SHORTEN — desce um nível

| \# | Entrada | Contexto anterior | Esperado | Racional |  
|---|---|---|---|---|  
| H01 | "resume" | STRATEGIC, "plano completo" | STEP\_BY\_STEP | Desce STRATEGIC → STEP\_BY\_STEP |  
| H02 | "de forma mais curta" | STEP\_BY\_STEP, "tutorial longo" | DETAILED | Desce STEP\_BY\_STEP → DETAILED |  
| H03 | "em uma frase" | DETAILED, "explicação técnica" | FLASH | Desce DETAILED → FLASH |  
| H04 | "resume tudo" | FLASH, "resposta curta" | FLASH | Não desce além de FLASH |

\#\#\# 5.4 Sinal CLARIFY — mantém perfil, reformula

| \# | Entrada | Contexto anterior | Esperado | Racional |  
|---|---|---|---|---|  
| L01 | "não entendi" | DETAILED, "sobre atomos" | DETAILED | Reformula, mantém estrutura |  
| L02 | "explica de novo" | STEP\_BY\_STEP, "tutorial" | STEP\_BY\_STEP | Reformula, mantém estrutura |  
| L03 | "de outro jeito" | STRATEGIC, "análise" | STRATEGIC | Reformula, mantém estrutura |

\#\#\# 5.5 Sinal NEW\_TOPIC — reseta e reclassifica

| \# | Entrada | Contexto anterior | Esperado | Racional |  
|---|---|---|---|---|  
| N01 | "muda de assunto, que horas são" | STRATEGIC, qualquer | FLASH | Sujeito próprio, ignora contexto |  
| N02 | "esquece isso, me explica química" | STEP\_BY\_STEP, qualquer | DETAILED | Sujeito próprio |  
| N03 | "outra coisa, plano de férias" | FLASH, qualquer | STRATEGIC | Sujeito próprio |

\---

\#\# 6\. Matriz do classificador — falsos positivos e edge cases

Casos onde o sistema \*\*poderia\*\* errar por proximidade linguística com gatilhos.

| \# | Entrada | Armadilha | Esperado | Racional |  
|---|---|---|---|---|  
| E01 | "explica o Explica.ai pra mim" | "explica" duas vezes | DETAILED | Não vira STEP\_BY\_STEP nem STRATEGIC |  
| E02 | "resume aquele passo a passo" | "resume" \+ "passo a passo" | FLASH | "resume" tem prioridade de brevidade |  
| E03 | "analisa rápido isso" | "analisa" \+ "rápido" | FLASH | Brevidade sobrescreve análise |  
| E04 | "planeja um dia" | "planeja" (não "plano") | STRATEGIC | Verbo derivado deve pegar |  
| E05 | "que passo posso dar agora" | "passo" sem "a passo" | FLASH ou DETAILED | Não é sequência pedagógica |  
| E06 | "aprofunda" | Elíptico sem contexto | FLASH | Sem turno anterior, cai no default |  
| E07 | "mais" | Elíptico sem contexto | FLASH | Sem turno anterior, sem escalada possível |  
| E08 | "tô perdido, me guia" | "guia" sozinho | STEP\_BY\_STEP | "guia" é gatilho de sequência |  
| E09 | "resume rápido" | Redundância de brevidade | FLASH | Duplo gatilho não escala |

\---

\#\# 7\. Cenários multimodais — planejados

Estes cenários \*\*ainda não são executáveis\*\* — dependem da Fase 1 (multimodalidade) do roadmap. Ficam aqui como especificação antecipada dos casos de teste que existirão quando a feature for implementada.

\#\#\# 7.1 Foto \+ pergunta canônica

| \# | Cenário | Comportamento esperado |  
|---|---|---|  
| M01 | Foto de um objeto simples \+ "o que é isso" | Resposta FLASH descrevendo o objeto |  
| M02 | Foto de página de livro \+ "me explica o que tá escrito aqui" | Resposta DETAILED interpretando o texto |  
| M03 | Foto de exercício de matemática \+ "me ensina a resolver isso" | Resposta STEP\_BY\_STEP |  
| M04 | Foto de gráfico \+ "analisa esses dados" | Resposta STRATEGIC |  
| M05 | Foto sem pergunta específica | App deve solicitar por voz o que fazer com a imagem |

\#\#\# 7.2 Foto \+ continuação (Camada 0 aplicada a anexo)

| \# | Cenário | Comportamento esperado |  
|---|---|---|  
| M06 | M02 \+ "aprofunda" | Escala para STEP\_BY\_STEP mantendo foco na foto |  
| M07 | M03 \+ "resume" | Desce para DETAILED mantendo foco na foto |  
| M08 | Após M01, "e essa outra?" com foto nova | Nova foto substitui foco, mantém perfil FLASH |

\#\#\# 7.3 Foto — casos degradados

| \# | Cenário | Comportamento esperado |  
|---|---|---|  
| M09 | Foto muito escura ou desfocada | App responde honestamente que não consegue interpretar |  
| M10 | Foto vazia ou apenas branca | App responde que não vê conteúdo |  
| M11 | Foto anexada em dispositivo com pouca RAM | App degrada graciosamente (avisa que anexo não pôde ser processado) |  
| M12 | Duas fotos anexadas em sequência rápida | Segunda foto substitui a primeira (Fase 1 \= uma foto por turno) |

\---

\#\# 8\. Cenários de streaming de TTS

Testes específicos da Camada 3 (streaming por sentença).

| \# | Cenário | Comportamento esperado |  
|---|---|---|  
| T01 | Resposta FLASH curta | TTS começa \< 1s após fim da fala do usuário |  
| T02 | Resposta DETAILED média | Primeira sentença chega ao TTS antes de o modelo terminar |  
| T03 | Resposta STEP\_BY\_STEP com lista numerada | "1." não é cortado no meio, aguarda o resto da frase |  
| T04 | Resposta STRATEGIC longa | TTS mantém fluxo contínuo, sem gaps \> 500ms entre sentenças |  
| T05 | Sentença muito curta ("Sim.") | Buffer aguarda mais tokens antes de emitir |  
| T06 | Resposta interrompida por erro do modelo | TTS termina a última sentença completa e para limpamente |

\#\# 8.1\. Cenários bilíngues de professor de inglês

| \# | Cenário | Comportamento esperado |  
|---|---|---|  
| B01 | Resposta em português com `Agora repita: Good morning.` | TTS fala a instrução em `pt-BR` e a frase-alvo em `EN-US`/`EN-GB` |  
| B02 | Resposta com `Depois diga: Thank you.` | Frase curta comum em inglês não é sintetizada com voz portuguesa |  
| B03 | `Em inglês, fale "How are you?"` em streaming | A frase entre aspas é segmentada como inglês mesmo se o `?` chegar antes da aspa final |  
| B04 | Pedido `como se pronuncia good morning?` | Após a demonstração, a próxima escuta usa `EN-US`/`EN-GB`, não `pt-BR` |  
| B05 | Explicação comum sem frase-alvo inglesa | Próxima escuta continua em português |

\---

\#\# 9\. Métricas de sucesso do classificador

Além de casos individuais, o classificador tem métricas agregadas que valem monitorar quando o app estiver em uso real.

| Métrica | Meta | Como medir |  
|---|---|---|  
| Acerto de perfil (canônicos) | \> 95% | Rodar seções 3.1-3.4 periodicamente |  
| Acerto de perfil (ambíguos) | \> 85% | Rodar seção 4 periodicamente |  
| Acerto de continuações (Camada 0\) | \> 90% | Rodar seção 5 |  
| Fallback manual acionado | \< 5% dos turnos | Log local no app |  
| Distribuição de perfis em uso real | Nenhum perfil \< 5% nem \> 70% | Log local |

\*\*Se qualquer métrica cai abaixo do alvo por duas semanas seguidas, é sinal para revisar regex e regras de desempate — não sinal para adicionar mais perfis.\*\*

\---

\#\# 10\. Processo de execução

Enquanto não há CI, a execução é manual. Sugerido:

\*\*Antes de uma release ou mudança grande no classificador:\*\*  
1\. Rodar seções 3.1 a 3.4 (casos canônicos) — 100% deve passar  
2\. Rodar seção 4 (ambíguos) — regressão em qualquer caso é bloqueio  
3\. Rodar amostra de seção 5 (continuações) — mínimo 50% dos casos  
4\. Registrar status de cada caso na coluna Status  
5\. Se algum caso falha, decidir: bug do classificador ou expectativa errada? Ambos requerem ação.

\*\*Quando aparece caso novo em uso real:\*\*  
\- Se o app errou, registrar como novo caso na seção apropriada  
\- Se o app acertou mas o caso é interessante, registrar mesmo assim (previne regressão futura)

\*\*Manutenção do documento:\*\*  
\- Casos podem ser removidos se ficarem obsoletos, mas raramente devem  
\- Casos que passaram a falhar (⚠️) são candidatos a bug ou ADR de mudança de expectativa  
\- A cada release, revisar coluna Status

\---

\#\# 11\. Automação futura

Quando o projeto tiver CI, esta matriz vira base de testes automatizados. O modelo mental:

\- \*\*Casos do classificador\*\* (seções 3, 4, 5, 6\) → JUnit tests do \`IntentClassifier\` — determinísticos, rápidos, rodam a cada commit  
\- \*\*Cenários multimodais\*\* (seção 7\) → Testes de integração com modelo real — mais lentos, rodam nightly  
\- \*\*Streaming TTS\*\* (seção 8\) → Testes com mock de output de tokens — determinísticos  
\- \*\*Métricas\*\* (seção 9\) → Dashboard local no dev, opcional em release

Nada disso é prioridade agora. Testes manuais bem executados são melhores que automação apressada.

\---

\#\# 12\. Casos removidos ou arquivados

Se um caso for removido da matriz, registrar aqui com motivo. Preserva memória.

\*(Vazio na v1.)\*  
\`\`\`

\---

\*\*Comentários sobre o TESTING.md:\*\*

\*\*Decisões conscientes que tomei:\*\*

1\. \*\*Não é lista exaustiva, é lista crítica\*\* — deixei isso explícito no início. Matriz de teste morre por peso se você tenta cobrir tudo. 50-80 casos bem escolhidos \> 500 casos rasos.

2\. \*\*Casos organizados por perfil E por tipo de teste\*\* — canônicos, ambíguos, continuações, edge cases. Cada seção testa uma coisa diferente. Se você só tem tempo de rodar uma, roda os canônicos (seção 3\) porque revelam bug base.

3\. \*\*Cenários multimodais estão listados mas marcados como "não executáveis"\*\* — porque a feature não existe ainda. Isso é intencional: \*\*especifica o comportamento antes da implementação\*\*, forçando você a pensar em edge cases (fotos degradadas, RAM baixa, etc.) antes de codar.

4\. \*\*Métricas agregadas (seção 9)\*\* — casos individuais são úteis, mas o real sinal de saúde é distribuição. Se 90% dos turnos caem em FLASH, tem algo errado no classificador ou nos gatilhos.

5\. \*\*Coluna Status com ⬜ ✅ ⚠️\*\* — permite ver de relance o que foi testado. Todos começam como ⬜ (não testado) porque eu não sei o que passa hoje.

6\. \*\*Seção 11 sobre automação futura\*\* — dá direção sem prometer prazo. Automação de LLM tests é notoriamente frágil; melhor fazer manual bem feito primeiro.

\*\*O que você provavelmente vai querer ajustar:\*\*

\- \*\*Rodar os casos\*\* e marcar ⬜ → ✅ ou ⚠️ conforme resultado real  
\- Se descobrir que algum caso "canônico" falha, pode ser sinal de bug crítico ou de que a expectativa está errada  
\- Se tem casos que testou empiricamente e não estão aqui, adicionar  
\- A seção 7 (multimodal) você pode querer ajustar depois de ler o SDK do LiteRT-LM — talvez alguns cenários precisem ser reformulados dependendo de como a API multimodal expõe imagens

\*\*Onde estamos:\*\*

✅ README.md  
✅ ARCHITECTURE.md  
✅ RISKS.md v1  
✅ ROADMAP.md  
✅ ADR-001 a ADR-005  
✅ TESTING.md v1

**Minha sugestão de sequência mental (não sprint, só ordem):**

* Fase 1: Multimodalidade (validada — o que você já tem no roadmap)  
* Fase 2: Modo Professor com PDFs (validada)  
* **Fase 3: Persistência básica de sessões** (o próximo já planejado — Room criptografado, retomar sessão) — pré-requisito  
* **Fase 4: Memória persistente estruturada** ← a conversa de agora  
* Fase 5: Integrações externas usando memória (WhatsApp, notificações, etc.)

🎯 Ótimo. Vamos avançar então — agora mapeando os casos de uso que a memória atende. Essa parte é o que vai justificar (ou não) a Fase 4 formalmente.

\---

\#\# Antes de listar: por que esse mapeamento importa

Você respondeu na pergunta C que "memória serve várias coisas". Mas \*\*quais\*\*, especificamente, muda tudo.

Se a memória serve 2-3 casos, é uma feature. Se serve 8-10 casos, é uma \*\*capacidade fundacional\*\* — e aí o desenho tem que ser genérico o suficiente pra suportar todos, não específico pra um só.

Vou listar \*\*honestamente\*\* os casos que eu enxergo, avaliando cada um com três lentes:

\- \*\*Valor real\*\* — o quanto isso muda a experiência  
\- \*\*Dependência\*\* — o quanto esse caso depende de memória mesmo (ou é mais simples resolver de outra forma)  
\- \*\*Risco de erro\*\* — o que acontece se a memória "sabe errado"

Se ao final tivermos 3+ casos com \*\*valor real alto E dependência real de memória\*\*, a Fase 4 se justifica.

\---

\#\# Os casos possíveis

\#\#\# Caso 1 — Continuidade entre sessões

\*\*O que resolve:\*\*  
Hoje o Kabem Voice esquece tudo quando você fecha o app. Se você conversou sobre CFC ontem e volta hoje, precisa reintroduzir contexto.

Com memória, o app lembra que você tá estudando CFC, tá fazendo plano de 6 meses, tá no módulo tal.

\*\*Valor real:\*\* 🟢 Alto  
Continuidade é a diferença entre "app com o qual falo" e "assistente que me acompanha".

\*\*Dependência de memória:\*\* 🟡 Média  
Poderia ser resolvido só com \*\*persistência de sessão\*\* (Fase 3 já planejada, retomar sessão anterior). Memória estruturada é mais poderosa, mas persistência simples já cobre bastante.

\*\*Risco de erro:\*\* 🟢 Baixo  
Se o app lembra errado do assunto, você corrige na hora e ele reajusta.

\*\*Veredicto:\*\* Meio-termo. \*\*Persistência de sessão da Fase 3 já resolve 70% disso.\*\* Memória só agrega quando é fato factual (não conversa em curso).

\---

\#\#\# Caso 2 — Modo Professor lembrar do progresso

\*\*O que resolve:\*\*  
Se você tá estudando um PDF de química ao longo de várias sessões, o app lembra: "você já cobriu ligações iônicas, agora tá em covalentes, ainda não viu orgânica".

Sem memória, cada sessão você reinicia — ou precisa reanexar PDF, releitura, etc.

\*\*Valor real:\*\* 🟢 Alto  
É a diferença entre "professor particular" e "app que explica coisa".

\*\*Dependência de memória:\*\* 🟢 Alta  
Progresso de estudo é fato estruturado que dura semanas — não cabe em sessão persistida.

\*\*Risco de erro:\*\* 🟡 Médio  
Se o app "acha" que você já viu covalentes mas você ainda não viu, pula tópico importante. Mitigável com confirmação: "você já viu covalentes, certo?"

\*\*Veredicto:\*\* ✅ Caso forte pra memória. Sem memória, o Modo Professor fica limitado à sessão única.

\---

\#\#\# Caso 3 — Preferências pessoais globais

\*\*O que resolve:\*\*  
"Não gosto de reunião antes das 10h" → em qualquer contexto futuro relevante, o app respeita.  
"Sou alérgico a camarão" → se algum dia falar de comida, considera.  
"Prefiro respostas curtas quando é sobre trabalho" → afeta o próprio classificador de perfil.

\*\*Valor real:\*\* 🟡 Médio a Alto  
Aumenta a sensação de "conhece você". Mas casos específicos são raros — talvez 5-10 preferências relevantes na vida toda.

\*\*Dependência de memória:\*\* 🟢 Alta  
Preferência não cabe em sessão nem em PDF.

\*\*Risco de erro:\*\* 🟡 Médio  
Se persistir "gosta de resposta curta" errado, degrada todas as respostas futuras. Confirmação inline (Variante 1.5) mitiga.

\*\*Veredicto:\*\* ✅ Caso sólido. Pequeno em volume (poucas preferências), grande em impacto (afetam tudo).

\---

\#\#\# Caso 4 — Contexto sobre pessoas (contatos)

\*\*O que resolve:\*\*  
"João é amigo de faculdade" / "Marcelo é chefe formal" / "mãe prefere ligação de voz, não mensagem"

Isso permite resposta contextualizada quando mencionar essas pessoas.

\*\*Valor real:\*\* 🟢 Alto (\*\*se\*\* você usa o app pra falar de pessoas)  
Ganho gigante em qualidade de rascunho de mensagem, planejamento social, lembretes.

\*\*Dependência de memória:\*\* 🟢 Alta  
Fato relacional por definição precisa persistir.

\*\*Risco de erro:\*\* 🔴 Alto  
Errar tom com pessoa é \*\*muito ruim\*\*. "Marcelo formal" registrado como "Marcelo informal" → você manda mensagem casual pro chefe. Confirmação inline é obrigatória aqui.

\*\*Veredicto:\*\* ✅ Caso forte, mas requer maior cuidado no design. Nenhum contato entra na memória sem confirmação explícita.

\---

\#\#\# Caso 5 — Compromissos e calendário

\*\*O que resolve:\*\*  
"Sábado tenho churrasco na casa do João às 15h" → app lembra se você perguntar "o que tenho no sábado".

\*\*Valor real:\*\* 🟡 Médio  
Útil, mas Google Calendar já resolve isso. Vantagem do Kabem Voice: input por voz natural, sem abrir app separado.

\*\*Dependência de memória:\*\* 🟢 Alta  
Compromisso é fato, precisa persistir.

\*\*Risco de erro:\*\* 🟡 Médio  
Errar data/hora de compromisso é ruim, mas é bug óbvio — usuário percebe rápido.

\*\*Veredicto:\*\* 🤔 Duvidoso pra MVP. \*\*Compete com Google Calendar\*\* e não ganha. Só entra se houver integração com calendar nativo do Android (aí é conector, não memória).

\*\*Sugestão honesta:\*\* cortar do escopo. Deixar Google Calendar cuidar disso.

\---

\#\#\# Caso 6 — Responder WhatsApp/mensagens com contexto (o caso original)

\*\*O que resolve:\*\*  
Mensagem chega → app lê → busca contexto sobre remetente e assunto → gera rascunho contextualizado.

\*\*Valor real:\*\* 🟡 Médio a Alto  
Depende de quanto você usa mensageria. Se você recebe muitas mensagens que exigem contexto, muito valioso.

\*\*Dependência de memória:\*\* 🟢 Alta  
Sem memória, o rascunho fica genérico (como conversamos antes).

\*\*Risco de erro:\*\* 🔴 Alto  
Rascunho errado pode ser enviado se confirmação for por engano. Enviar mensagem errada pro chefe é a "categoria João Cabra" de erro.

\*\*Veredicto:\*\* ✅ Caso central, mas depende do Caso 4 (contexto sobre pessoas) estar sólido primeiro.

\---

\#\#\# Caso 7 — Padrões e insights ao longo do tempo

\*\*O que resolve:\*\*  
"Você costuma responder curto nas segundas de manhã" — o app percebe padrão e adapta.  
"Toda vez que fala de trabalho, o tom fica mais tenso" — insight que o próprio app percebe.

\*\*Valor real:\*\* 🟢 Alto teoricamente  
🔴 Médio na prática — pode ser invasivo, "creepy" se mal calibrado

\*\*Dependência de memória:\*\* 🟢 Alta  
Padrão só existe com histórico agregado.

\*\*Risco de erro:\*\* 🔴 Alto  
Insight errado ("você é rude com sua mãe") pode ser ofensivo. Ninguém quer app que julga.

\*\*Veredicto:\*\* ❌ Cortar do MVP. Talvez v3 do app. Terreno psicológico complicado, retorno duvidoso.

\---

\#\#\# Caso 8 — Fatos sobre você mesmo

\*\*O que resolve:\*\*  
"Sou desenvolvedor Android" / "moro em Curitiba" / "minha filha tem 8 anos"

Base para qualquer resposta que dependa de quem você é.

\*\*Valor real:\*\* 🟢 Alto  
É o "sobre mim" que informa quase todas as outras respostas.

\*\*Dependência de memória:\*\* 🟢 Alta  
Fato estável sobre você — por definição persistente.

\*\*Risco de erro:\*\* 🟡 Médio  
Errar é chato mas raramente crítico. Mitigável.

\*\*Veredicto:\*\* ✅ Caso base. Provavelmente o \*\*primeiro tipo de fato\*\* que a memória captura.

\---

\#\#\# Caso 9 — Retomada de tópicos e projetos

\*\*O que resolve:\*\*  
"Tô planejando abrir uma empresa" → semanas depois: "voltando ao tema da empresa, penso em..."  
App lembra o que já foi discutido, evita você reintroduzir do zero.

\*\*Valor real:\*\* 🟢 Alto  
Assistente que "acompanha projetos" é qualitativamente diferente.

\*\*Dependência de memória:\*\* 🟢 Alta  
Projeto multi-sessão é o caso paradigmático de memória.

\*\*Risco de erro:\*\* 🟢 Baixo  
Se lembra errado, você corrige.

\*\*Veredicto:\*\* ✅ Caso forte. Provavelmente virá naturalmente quando os outros casos existirem.

\---

\#\#\# Caso 10 — Rotinas e hábitos

\*\*O que resolve:\*\*  
"Todo dia às 22h me pergunta se registrei o que estudei" — rotina proativa.

\*\*Valor real:\*\* 🟡 Médio  
Cool mas o Android já tem infra pra isso (rotinas Google, alarmes, Bixby, Assistant).

\*\*Dependência de memória:\*\* 🟡 Média  
Rotina é fato estruturado, mas execução depende de background/scheduling — não é memória, é orquestração.

\*\*Risco de erro:\*\* 🟢 Baixo

\*\*Veredicto:\*\* 🤔 Fora do escopo. Território de app de produtividade, não assistente de voz.

\---

\#\# Consolidação — quais casos justificam a Fase 4

Filtrando por \*\*valor alto \+ dependência real de memória \+ risco gerenciável\*\*:

| Caso | Entra? |  
|---|---|  
| 1 — Continuidade entre sessões | 🟡 Parcial (Fase 3 persistência resolve maior parte) |  
| 2 — Progresso em Modo Professor | ✅ Sim |  
| 3 — Preferências pessoais | ✅ Sim |  
| 4 — Contexto sobre pessoas | ✅ Sim |  
| 5 — Compromissos | ❌ Não (Google Calendar) |  
| 6 — Responder mensagens com contexto | ✅ Sim |  
| 7 — Padrões/insights | ❌ Não (v3+, terreno complicado) |  
| 8 — Fatos sobre você mesmo | ✅ Sim |  
| 9 — Retomada de projetos | ✅ Sim |  
| 10 — Rotinas | ❌ Não (fora de escopo) |

\*\*Resultado: 6 casos entram, 4 saem.\*\*

Isso é o suficiente pra dizer com honestidade: \*\*memória persistente é fundacional, sim, justifica ser Fase 4\*\*.

\---

\#\# O que a Fase 4 precisa entregar minimamente

Consolidando os 6 casos que entram, a memória mínima viável precisa de:

\*\*Tipos de fato que persistem:\*\*

1\. \*\*Sobre o usuário\*\* (Caso 8\) — identidade, atributos estáveis  
2\. \*\*Sobre pessoas\*\* (Caso 4, 6\) — contatos com relação e tom  
3\. \*\*Preferências\*\* (Caso 3\) — o que gosta/não gosta em como o app se comporta  
4\. \*\*Projetos ativos\*\* (Caso 9\) — tópicos multi-sessão em andamento  
5\. \*\*Progresso de estudo\*\* (Caso 2\) — o que já foi coberto em cada PDF/tópico

\*\*Não persiste (fora do escopo):\*\*  
\- Compromissos com data (usar calendar nativo)  
\- Padrões inferidos automaticamente sobre você  
\- Rotinas com scheduling  
\- Estado emocional momentâneo  
\- Fatos efêmeros ("comi pizza ontem")

\*\*Cinco categorias durável, tratadas separadamente\*\*. Não é "uma tabela de fatos", são \*\*cinco tipos de fato com tratamento distinto\*\*.

Isso é importante porque \*\*a UX de revisão precisa refletir essa separação\*\*. Ninguém quer ver "132 fatos misturados". Quer ver:  
\- 12 coisas que sei sobre você  
\- 8 pessoas que conheço  
\- 5 preferências suas  
\- 3 projetos ativos  
\- 2 assuntos de estudo em curso

Isso é auditável, humano, gerenciável.

\---

\#\# O tamanho real da Fase 4

Sendo honesto sobre esforço para entregar os 6 casos:

\*\*Infraestrutura mínima:\*\*  
\- Room database com 5 tabelas (uma por categoria)  
\- Extração automática de fatos via LLM (usa Gemma existente, prompt dedicado)  
\- Confirmação inline (Variante 1.5) integrada ao pipeline de resposta  
\- Fila de fatos "candidatos" pra revisão em lote (casos que não couberam inline)  
\- Recuperação contextual (identifica o que buscar em cada turno)  
\- Tela de "Minha memória" com as 5 seções, edição, exclusão  
\- Ajustes no classificador de intenção para detectar quando extrair  
\- Ajustes no prompt principal para injetar fatos relevantes por turno

\*\*Isso é 4-6 sprints honestos de trabalho focado\*\*, com risco alto de escopo crescer. É Fase, não sprint. Você já reconheceu isso, então tá alinhado.

\---

\#\# Sequência mental consolidada

Aqui está o roadmap mental que faz sentido depois dessa conversa:

\- \*\*Fase 1\*\* — Multimodalidade (foto por turno) — já planejado  
\- \*\*Fase 2\*\* — Modo Professor com PDFs — já planejado  
\- \*\*Fase 3\*\* — Persistência de sessão (Room criptografado, retomar sessão) — já planejado  
\- \*\*Fase 4\*\* — Memória persistente estruturada (5 categorias, Variante 1.5) — \*\*essa conversa\*\*  
\- \*\*Fase 5\*\* — Integrações externas (WhatsApp, notificações) usando memória — depende da 4

\*\*Boa notícia:\*\* essa sequência é \*\*lógica e cumulativa\*\*. Cada fase reusa infra da anterior. Não há retrabalho grande previsto.

\---

\#\# O que registrar no ROADMAP agora

Se você concorda com esse mapeamento, sugiro que o Parking Lot do \`ROADMAP.md\` receba \*\*uma entrada consolidada e honesta\*\* sobre a Fase 4 hipotética. Não como promessa, mas como direção mapeada — para quando fizer sentido revisitar.

O texto seria algo do tipo:

\> \*\*Fase 4 potencial — Memória persistente estruturada\*\*  
\> Base fundacional para o app "conhecer o usuário" ao longo do tempo. Cobre 6 casos de uso mapeados: fatos sobre o usuário, contexto sobre pessoas, preferências, projetos ativos, progresso em estudos, e resposta contextualizada a mensagens.  
\>  
\> Abordagem: extração automática \+ confirmação conversacional oportuna (Variante 1.5), com fila residual para revisão em lote.  
\>  
\> Não persiste: compromissos com data, padrões inferidos, rotinas com scheduling.  
\>  
\> Depende de: Fase 3 (persistência básica) implementada.  
\> Habilita: Fase 5 (integrações externas com contexto).  
\>  
\> Tamanho estimado: 4-6 sprints focados. Fase inteira, não incremento.  
\>  
\> Ver conversa de conceito em \[link do commit onde essa conversa foi consolidada, ou anexo\].

Isso preserva a memória do que conversamos sem comprometer com data.

\---

\#\# Onde eu paro

Escrevi muito, então deixa eu resumir onde chegamos:

\- \*\*A ideia de memória persistente sobrevive ao teste de "vários casos"\*\* — 6 casos legítimos justificam  
\- \*\*Variante 1.5\*\* foi validada como abordagem  
\- \*\*Fase 4 hipotética faz sentido\*\* entre Fase 3 (persistência) e Fase 5 (integrações externas)  
\- \*\*Escopo está mapeado\*\* — cinco categorias de fato, seis casos de uso, quatro exclusões conscientes

Duas perguntas finais pra fechar (opcionais):

\*\*1.\*\* Quer que eu escreva o \*\*parágrafo formal\*\* pra colar no \`ROADMAP.md\` seção 4 (Parking Lot)? Fica registrado sem virar promessa.

\*\*2.\*\* Ou prefere que essa conversa vire um \*\*ADR de direção estratégica\*\* (ADR-006: "Memória persistente como direção estratégica futura"), que é mais formal e documenta a decisão de reservar esse espaço?

Ambos são válidos. ADR é mais peso ("decidimos isso"), parking lot é mais leve ("consideramos isso"). Você decide qual tom prefere. 🎯  
