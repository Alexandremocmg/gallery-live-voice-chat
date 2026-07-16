# Kabem Voice - Plano de Implementação do Intelligence Core

**Data:** 16 de julho de 2026  
**Objetivo:** transformar o Kabem Voice em um assistente local mais inteligente, natural e útil, preservando privacidade, desempenho e controle explícito sobre qualquer recurso conectado.

## 1. Princípios de arquitetura

1. **Local e privado por padrão.** O modo conectado nunca é ativado automaticamente.
2. **Uma conversa, uma fonte de contexto.** O histórico não pode existir simultaneamente no runtime e em prompts repetidos por turno.
3. **Capacidades antes do nome do modelo.** A escolha do modelo considera áudio, imagem, ferramentas, thinking, memória disponível e tarefa.
4. **Thinking é exceção.** Conversa cotidiana permanece rápida; raciocínio profundo é reservado para tarefas que realmente ganham com ele.
5. **Skills são especializações controladas.** O app ativa somente Skills instaladas e selecionadas pelo usuário.
6. **Sensores são explícitos.** Câmera e áudio só são usados com indicação visual clara e limites de duração.
7. **Online nunca se disfarça de offline.** MCP e Skills com rede exibem estado conectado e permissões próprias.

## 2. Arquitetura-alvo

```text
Voz / câmera / áudio / PDF
            |
            v
ContextAssembler + ContextBudgetManager
            |
            v
VoiceCapabilityRouter
  conversa | professor | percepção | skill | MCP
            |
            v
CognitivePolicy + ModelPolicy + PermissionPolicy
            |
            v
LiteRT Conversation + ferramentas permitidas
            |
            v
ResponseCoordinator / SkillCoordinator
            |
            v
TTS, resultado visual e retomada
```

### Contratos compartilhados

```kotlin
enum class ConnectivityMode { PRIVATE_OFFLINE, CONNECTED }

enum class VoiceTurnIntent {
    CONVERSATION, TEACHING, PERCEPTION, SKILL, MCP
}

enum class CognitiveMode { FAST, DEEP }

data class RequiredCapabilities(
    val image: Boolean = false,
    val audio: Boolean = false,
    val tools: Boolean = false,
    val thinking: Boolean = false,
    val network: Boolean = false,
)
```

Também serão introduzidos `ContextBudgetSnapshot`, `ModelSelection`, `SpeechPlaybackCheckpoint` e `DeviceCapabilityProfile`.

## 3. Fase 0 - Correções estruturais imediatas

Esta fase corrige falhas que contaminariam todas as demais entregas.

### 3.1 Áudio multimodal

- Passar `supportAudio = model.llmSupportAudio` em toda inicialização e retomada de conversa do Voice.
- Validar `audioClips` antes da inferência e impedir envio a modelos sem suporte.
- Liberar gravações temporárias após inferência, cancelamento ou destruição da tela.
- Criar fallback por transcrição quando áudio direto não estiver disponível.

### 3.2 Retomada correta de sessões

- Criar `VoiceConversationMapper` para converter mensagens persistidas em mensagens aceitas pelo LiteRT.
- Restaurar a conversa por `initialMessages`, incluindo resumo compacto e turnos recentes.
- Preservar perfil pedagógico, idioma, modo cognitivo e estado de reprodução; nunca restaurar áudio bruto.
- Invalidar com segurança estados incompletos de ação ou confirmação após o processo morrer.

### 3.3 Eliminar contexto duplicado

- Remover a reinjeção de `buildSessionContext()` a cada turno quando o runtime já mantém o histórico.
- Manter apenas uma fonte ativa de histórico dentro da conversa LiteRT.
- Usar resumo rolante e turnos recentes somente ao restaurar ou compactar uma sessão.
- Deduplicar por identificador de mensagem durante migração e retomada.

### 3.4 Seleção de modelo por capacidade

Criar `VoiceModelSelector`, que pontua modelos por:

- capacidades obrigatórias da tarefa;
- suporte a Agent Chat e ferramentas;
- janela de contexto;
- RAM disponível e requisitos do modelo;
- resultados locais de benchmark;
- custo de inicialização e estado já carregado;
- pressão térmica e bateria.

Política inicial:

- Gemma 4 E2B como modelo inteligente preferencial em aparelhos compatíveis a partir de 8 GB.
- Gemma 4 E4B somente em aparelhos com cerca de 12 GB ou mais e bom resultado de benchmark.
- Modelo textual menor para conversa sem mídia em aparelhos limitados.
- Não trocar de modelo no meio de um turno. Trocas por nova capacidade acontecem antes do envio e são comunicadas quando causarem espera perceptível.

### Critérios de aceite

- Exercício com áudio chega ao Gemma em modelo compatível.
- Uma sessão retomada não repete nem perde turnos recentes.
- Vinte turnos não provocam crescimento duplicado do prompt.
- Modelo sem imagem ou áudio nunca é escolhido para uma tarefa que exige essa modalidade.

## 4. Fase 1 - Smart Core e orçamento de contexto

### 4.1 Roteador local de capacidades

Criar `VoiceCapabilityRouter`, sem segunda chamada ao modelo, combinando:

- perfil de resposta e estado do professor adaptativo;
- anexos presentes;
- frases de ativação de Skills;
- Skill explicitamente selecionada;
- modo privado ou conectado;
- capacidades do modelo ativo.

O resultado do roteamento informa intenção, capacidades necessárias, risco, modo cognitivo sugerido e política de resposta.

### 4.2 Orçamento de tokens

Criar `ContextBudgetManager` com uma interface `TokenEstimator`.

Orçamento por turno:

```text
janela do modelo
- prompt do sistema
- catálogo de ferramentas/Skills
- saída reservada
= orçamento disponível para conversa, memória e anexos
```

Política inicial de ocupação:

- abaixo de 65%: contexto normal;
- 65% a 75%: remover metadados redundantes;
- 75% a 85%: compactar turnos antigos e descartar anexos sem relevância atual;
- 85% a 92%: manter resumo rolante, turnos recentes e trechos recuperados;
- acima de 92%: reiniciar a conversa LiteRT com contexto compactado em `initialMessages`.

A primeira versão pode usar estimativa heurística calibrada pelos benchmarks locais. Um tokenizador exato substitui a heurística quando a API do runtime permitir, sem alterar os consumidores.

### 4.3 Compactação sem latência constante

- Não chamar um segundo modelo para resumir todo turno.
- Criar resumos determinísticos curtos por turno.
- Gerar resumo semântico pelo modelo apenas em compactação explícita ou período ocioso.
- Nunca resumir de forma destrutiva objetivos pedagógicos, correções de pronúncia, códigos ou datas relevantes.

### Critérios de aceite

- Conversa longa com imagem e PDF continua dentro do limite do modelo.
- O mesmo conteúdo não aparece duas vezes no contexto enviado.
- Compactação preserva nome, objetivo atual, decisões e pontos não resolvidos.
- Métricas locais registram tamanho estimado, tempo até primeira resposta, início do TTS e velocidade de geração sem guardar conteúdo pessoal.

## 5. Fase 2 - Cognição adaptativa

Criar `CognitivePolicy` para decidir entre `FAST` e `DEEP`.

### FAST

Usado em:

- FLASH e conversa cotidiana;
- saudações e perguntas factuais simples;
- explicações curtas;
- comandos de ação;
- confirmações e cancelamentos.

### DEEP

Usado em:

- STRATEGIC;
- comparação com vários critérios e consequências;
- planejamento e análise causal difícil;
- síntese de várias páginas ou fontes;
- etapa `DEEPEN` do professor adaptativo;
- pedido explícito para pensar ou analisar profundamente.

### Regras técnicas

- Ativar `enable_thinking=true` apenas se o modelo declarar `LLM_THINKING` e a política escolher `DEEP`.
- Não ativar thinking automaticamente em todo `STEP_BY_STEP`.
- Nunca enviar o conteúdo de thinking ao TTS, à interface ou à memória.
- Aplicar limite de tempo e fallback para `FAST` quando o aparelho estiver sob pressão térmica ou quando a resposta profunda exceder o orçamento.
- Decodificação especulativa permanece uma configuração de sessão/modelo, não uma alternância por turno.

### Critérios de aceite

- Matriz de testes classifica conversas simples como rápidas e planos difíceis como profundos.
- Thinking não aparece na resposta falada ou persistida.
- FLASH mantém latência semelhante à versão atual.
- Modo profundo degrada com elegância em modelos sem thinking.

## 6. Fase 3 - Percepção inteligente

### 6.1 Câmera como olhos do Kabem

- Usar CameraX com prévia e indicador de privacidade visível.
- A câmera é iniciada e encerrada explicitamente pelo usuário.
- Capturar uma imagem ou poucos quadros-chave, reduzir ao tamanho adequado e liberar recursos após o turno.
- Rotear frases como “o que você está vendo?” e “leia isto” para `PERCEPTION`.
- Evitar vídeo contínuo e limitar cada análise a 1 a 3 quadros.

### 6.2 Áudio como contexto geral

- Extrair um `VoiceActivityDetector` reutilizável do gravador de pronúncia.
- Definir `AudioCaptureMode`: `SPEECH_ASR`, `PRONUNCIATION` e `GENERAL_ANALYSIS`.
- Exigir início explícito para análise geral de áudio, com duração máxima e indicador visual.
- Manter amostras somente em memória e apagá-las após uso.
- Quando o modelo não aceitar áudio, explicar a limitação e oferecer transcrição como fallback.

### 6.3 PDFs híbridos e escaneados

Substituir o fluxo único atual por `PdfIngestionPipeline`:

1. Classificar páginas como textuais, híbridas ou escaneadas.
2. Extrair texto selecionável quando existir.
3. Renderizar páginas escaneadas em bitmap com `PdfRenderer` ou PDFBox.
4. Executar OCR totalmente local para criar índice pesquisável.
5. Dividir por página e seção e usar busca por relevância no estilo BM25.
6. Enviar ao Gemma Vision apenas as páginas mais relevantes, junto do OCR e do número da página.

Limites iniciais: duas páginas em alta qualidade por pergunta, miniaturas em cache e limpeza junto com a sessão.

### Critérios de aceite

- PDF escaneado de dez páginas responde corretamente e informa a página em modo avião.
- Texto híbrido não é duplicado entre extração e OCR.
- Câmera e microfone são liberados ao sair da tela.
- Uso prolongado permanece dentro do orçamento de memória definido para o aparelho.

## 7. Fase 4 - Skills do Kabem por voz

Skills serão módulos especializados do próprio aplicativo. Cada Skill combina instruções, capacidades do modelo, memória permitida, estado próprio e uma experiência de voz adequada à atividade.

### Contrato de uma Skill

Criar um manifesto tipado `KabemSkillManifest` com:

- identificador e versão;
- nome, descrição e frases de ativação;
- capacidades necessárias: texto, áudio, imagem, PDF ou thinking;
- idiomas suportados;
- política de memória e dados que pode ler ou gravar;
- modo de conectividade: offline, opcional ou conectado;
- módulo de instruções e formato de resposta;
- tela ou controles específicos, quando existirem.

Criar `SkillExecutionEngine` fora da UI para:

- listar apenas Skills instaladas e selecionadas pelo usuário;
- detectar ativação explícita por voz ou pela interface;
- carregar o módulo completo somente quando a Skill for usada;
- manter o estado da Skill separado da conversa geral;
- devolver o controle ao modo de conversa sem perder o contexto útil;
- impedir que uma Skill solicite capacidades não declaradas.

### Primeira Skill - Professor de Inglês

O Professor de Inglês será a implementação de referência e aproveitará recursos já existentes:

- professor adaptativo em blocos curtos;
- inferência do nível do aluno e confirmação somente quando houver dúvida;
- alternância automática entre PT-BR e inglês;
- áudio direto no Gemma para exercícios de repetição quando o modelo permitir;
- fallback por transcrição local;
- avaliação cuidadosa de pronúncia, sem prometer precisão fonética que o modelo não consiga medir;
- exercícios de conversação, vocabulário, gramática, compreensão e repetição;
- memória local de progresso, erros recorrentes, palavras estudadas e objetivo atual;
- retomada da aula a partir do último bloco concluído;
- TTS com voz inglesa para exemplos e voz PT-BR para explicações.

Frases como “quero estudar inglês”, “vamos praticar conversação” ou “corrija minha pronúncia” ativam a Skill. A Skill permanece ativa até a aula terminar, o usuário mudar de assunto ou pedir para sair.

### Próximas Skills internas

- **Professor de PDF:** cria explicações, perguntas e revisões a partir do documento aberto.
- **Conversação em Inglês:** conversa livre com correções discretas e relatório no fim.
- **Leitor Visual:** explica objetos, textos e cenas capturados pela câmera.
- **Revisor de Estudos:** usa memórias autorizadas para revisar conteúdos e dificuldades anteriores.

Essas Skills reutilizam o Smart Core; não carregam um segundo modelo e não duplicam histórico.

### Skills JavaScript

Adicionar metadado obrigatório:

```kotlin
enum class NetworkAccess { NONE, OPTIONAL, REQUIRED }
```

- No modo privado, bloquear rede no WebView e nos clientes usados pela Skill, independentemente do que o metadado declara.
- No modo conectado, mostrar indicador persistente e pedir permissão na primeira execução.
- Aplicar timeout, limite de saída, tamanho máximo, tratamento do resultado como conteúdo não confiável e defesa contra instruções que tentem escapar das permissões.
- Falar apenas um resumo do resultado; mídia e páginas continuam disponíveis visualmente.

Skills JavaScript não fazem parte da primeira entrega. Elas entram somente após as Skills internas estarem estáveis e permanecem separadas da promessa de funcionamento offline.

### Critérios de aceite

- Skill não selecionada não pode ser executada por voz.
- Professor de Inglês inicia, mantém e encerra uma aula sem contaminar a conversa geral.
- Progresso de inglês é retomado após fechar e abrir o aplicativo.
- Exemplos em inglês usam reconhecimento e síntese no idioma correto.
- Skill offline funciona em modo avião.
- Skill que exige rede é bloqueada no modo privado.
- Uma Skill não consegue acessar capacidades ou memórias fora do próprio manifesto.

## 8. Fase 5 - Conversação realmente natural

Criar uma única `VoiceConversationStateMachine`:

```text
IDLE -> LISTENING -> THINKING -> SPEAKING
                                    |
                                    v
                               INTERRUPTED
                                    |
                       LISTENING ou SPEAKING
```

Estados adicionais: `SKILL_ACTIVE`, `SUCCEEDED` e `FAILED`.

### Barge-in e retomada

1. **Primeira entrega:** interrupção por toque durante o TTS.
2. **Segunda entrega:** interrupção por voz com VAD adaptativo, pré-buffer e cancelamento de eco quando disponível.

Usar `UtteranceProgressListener.onRangeStart` para guardar `SpeechPlaybackCheckpoint` com trecho, posição, idioma, voz e velocidade.

Política após interrupção:

- “continua” retoma do trecho restante;
- nova pergunta descarta o restante;
- “repete” reproduz o último bloco completo;
- durante um exercício de repetição, a fala do aluno é encaminhada à Skill ativa em vez de ser tratada como nova pergunta.

O VAD deve usar piso de ruído adaptativo, histerese, duração mínima de fala e limite máximo de gravação para reduzir falsos disparos pelo próprio TTS.

### Critérios de aceite

- Interrupção funciona no começo, meio e fim da resposta.
- Retomada usa a voz e o idioma corretos em respostas bilíngues.
- O áudio do próprio TTS não dispara barge-in em uma bateria de testes físicos.
- Cancelamento libera microfone, fila de TTS e gravação temporária.

## 9. Fase 6 - Modo conectado opcional

Criar uma preferência de alto nível:

- `PRIVATE_OFFLINE`: padrão, sem MCP ou Skills com rede.
- `CONNECTED`: ativado conscientemente pelo usuário, com indicador persistente.

### Garantias do modo privado

- ASR local; se indisponível, informar em vez de enviar áudio para a nuvem.
- MCP desligado e clientes encerrados.
- Rede de Skills JavaScript bloqueada.
- Analytics e telemetria remota desativados.
- Nenhum fallback automático para serviço online.

### Proteções do modo conectado

- Exibir servidor MCP e ferramenta que serão usados.
- Permissão por ferramenta, com opção “permitir sempre” revogável.
- Timeout, limite de saída, redução de dados enviados e histórico local de acessos.
- Ao voltar ao privado, fechar conexões e limpar resultados remotos transitórios.

### Critérios de aceite

- Instrumentação de rede confirma zero tráfego de saída no modo privado.
- MCP não executa sem modo conectado e permissão da ferramenta.
- A interface nunca apresenta uma execução conectada como totalmente local.
- Alternar para privado interrompe chamadas pendentes de forma segura.

## 10. Trabalho transversal

### Desempenho e saúde do aparelho

Criar `DeviceCapabilityProfile` a partir de RAM, benchmark, bateria e estado térmico. Sob pressão:

- desativar thinking;
- reduzir páginas e imagens simultâneas;
- preferir E2B ou modelo textual menor;
- diminuir saída máxima;
- evitar manter dois modelos carregados.

### Privacidade e armazenamento

- Evoluir memórias sensíveis para armazenamento local criptografado antes de distribuição pública.
- Não persistir áudio bruto, quadros de câmera ou cadeia de thinking.
- Manter migrações aditivas do Proto DataStore para não perder sessões existentes.
- Oferecer limpeza separada de conversa, progresso de Skills, memória e documentos.

### Observabilidade local

Registrar apenas métricas técnicas sem conteúdo:

- intenção e capacidade escolhidas;
- modelo selecionado e motivo;
- ocupação estimada do contexto;
- latência de primeira resposta e TTS;
- falhas de ferramenta e permissões;
- taxa de falso barge-in durante testes.

## 11. Estratégia de testes

### Testes JVM

- roteamento e desempate de intenções;
- orçamento e compactação;
- seleção de modelo por capacidade;
- política FAST/DEEP;
- ativação, isolamento e retomada de Skills;
- regras de modo privado/conectado;
- retomada e deduplicação de mensagens.

### Testes instrumentados

- CameraX e ciclo de vida;
- PDFs textuais, híbridos e escaneados;
- TTS, checkpoints e fila de retomada;
- VAD com arquivos de ruído e fala;
- Skills falsas para validar capacidades, isolamento e timeout;
- bloqueio de rede no WebView.

### Testes em aparelhos físicos

- aparelho de 8 GB e aparelho de 12 GB;
- modo avião e rede disponível;
- ambiente silencioso e ambiente ruidoso;
- conversa longa com mudança PT-BR/inglês;
- pressão térmica após uso prolongado;
- encerramento e retomada do processo durante fala e uma aula.

## 12. Ordem recomendada de entrega

1. **Fase 0:** corrigir áudio, retomada, duplicação e seleção de modelo.
2. **Fase 1:** roteador, orçamento de contexto e compactação.
3. **Fase 2:** thinking seletivo e política de desempenho.
4. **Fase 5A:** máquina de estados e barge-in por toque.
5. **Fase 3:** câmera, áudio geral e PDF escaneado.
6. **Fase 4A:** infraestrutura de Skills e Professor de Inglês.
7. **Fase 4B:** demais Skills internas do Kabem.
8. **Fase 5B:** barge-in por voz e VAD adaptativo.
9. **Fase 6:** MCP e modo conectado separado.

Cada fase deve ficar atrás de feature flag até cumprir seus critérios de aceite.

## 13. Marcos de produto

### Marco A - Núcleo confiável

Fases 0, 1 e 2. O Kabem conversa por mais tempo, escolhe melhor o modelo e aprofunda somente quando necessário.

### Marco B - Assistente perceptivo

Fases 3 e 5A. O Kabem entende câmera, áudio e PDFs escaneados e pode ser interrompido sem perder a conversa.

### Marco C - Assistente especializado

Fase 4. O Kabem oferece Skills internas especializadas, começando pelo Professor de Inglês, sem perder a conversa natural.

### Marco D - Ecossistema conectado opcional

Fases 5B e 6. Conversação por voz mais fluida e MCP claramente separado da experiência privada.

## 14. Decisões que não devem ser adotadas

- Ativar thinking em toda resposta.
- Manter dois modelos grandes carregados permanentemente.
- Enviar áudio, câmera ou documentos à rede como fallback silencioso.
- Confiar apenas no metadado de uma Skill para bloquear rede.
- Permitir que uma Skill leia toda a memória do usuário por padrão.
- Transformar a Skill de inglês em apenas um prompt longo sem estado pedagógico próprio.
- Persistir áudio bruto ou raciocínio interno.
- Misturar histórico do LiteRT com histórico reinjetado manualmente.
- Lançar todas as integrações MCP antes de estabilizar o núcleo local.

## 15. Definição de pronto do programa

O Intelligence Core estará pronto quando o Kabem conseguir, em um aparelho compatível:

- sustentar conversa longa sem repetição de contexto;
- alternar entre resposta rápida e análise profunda de modo previsível;
- entender imagem, áudio e PDF escaneado localmente;
- interromper e retomar a fala naturalmente;
- executar Skills selecionadas respeitando capacidades, memória e conectividade declaradas;
- conduzir e retomar uma aula de inglês adaptativa totalmente offline;
- demonstrar, por teste de rede, que o modo privado não transmite dados;
- explicar claramente quando uma capacidade exige o modo conectado.
