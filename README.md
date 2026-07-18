# Kabem Voice ✨

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Branch](https://img.shields.io/badge/branch-feat%2Flive--voice--chat-coral)](https://github.com/Alexandremocmg/gallery-live-voice-chat/tree/feat/live-voice-chat)

**Assistente de voz pessoal com IA local, offline e privado.**

Kabem Voice é um fork do Google AI Edge Gallery, redesenhado com identidade visual própria e foco central em conversação por voz em tempo real. Rode modelos LLM poderosos diretamente no seu celular — sem internet, sem servidores, sem nenhum dado saindo do aparelho ("Privado por padrão").

**Identidade Visual:**
- Paleta de cores exclusiva baseada no novo logo (coral, verde, azul suave e fundo marfim).
- Ícone do aplicativo, splash screen, launcher e interface inicial (Home) totalmente refeitos para refletir a marca Kabem Voice.
- Interface limpa com destaque para o painel de conversação e o botão "Falar agora".

Baseado no Google AI Edge Gallery com suporte oficial ao Gemma 4. Herda toda a infraestrutura de modelos (LiteRT, AICore) e adiciona o módulo Kabem Voice como funcionalidade principal.


| **Install the app today from Google Play** | **Install the app today from App Store** | **Download for macOS** |
| :--- | :--- | :--- |
| <a href='https://play.google.com/store/apps/details?id=com.google.ai.edge.gallery'><img alt='Get it on Google Play' height="120" src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png'/></a> | <a href="https://apps.apple.com/us/app/google-ai-edge-gallery/id6749645337?itscg=30200&itsct=apps_box_badge&mttnsubad=6749645337" style="display: inline-block;"> <img src="https://toolbox.marketingtools.apple.com/api/v2/badges/download-on-the-app-store/black/en-us?releaseDate=1771977600" alt="Download on the App Store" style="width: 244px; height: 88px; vertical-align: middle; object-fit: contain;" /></a> | <a href="https://dl.google.com/google-ai-edge-gallery/macos/dmg/GoogleAIEdgeGallery-0.1.0.dmg"><img alt='Download for macOS' width="257" height="97" src="https://github.com/user-attachments/assets/29c70795-93b3-4e8b-8752-0cad4e413182" /></a> |

For users without Google Play access, install the apk from the [**latest release**](https://github.com/google-ai-edge/gallery/releases/latest/)


## App Preview

<img width="480" alt="01" src="https://github.com/user-attachments/assets/a809ad78-aef4-4169-91ee-de7213cbb3bd" />
<img width="480" alt="02" src="https://github.com/user-attachments/assets/1effd10d-f45a-4f7b-9435-f50f1bdd36b6" />
<img width="480" alt="03" src="https://github.com/user-attachments/assets/e5089e41-2c18-4fbe-9011-ebe9e5a02044" />
<img width="480" alt="04" src="https://github.com/user-attachments/assets/0f39d3ed-7403-4606-a7c6-b2c7e51ba6c1" />
<img width="480" alt="05" src="https://github.com/user-attachments/assets/8c229e96-b598-4735-9f60-e96907e1d5d5" />
<img width="480" alt="06" src="https://github.com/user-attachments/assets/ac9fb77b-81de-4197-9ed3-f6fe58290b3e" />
<img width="480" alt="07" src="https://github.com/user-attachments/assets/bc86ba07-2eaf-49b1-980f-8a87a85c596f" />
<img width="480" alt="08" src="https://github.com/user-attachments/assets/061564ed-030f-4630-810b-13a7863fce4c" />

## ✨ Core Features

* **Agent Skills**: Transform your LLM from a conversationalist into a proactive assistant. Use the Agent Skills tile to augment model capabilities with tools like Wikipedia for fact-grounding, interactive maps, and rich visual summary cards. You can even load modular skills from a URL or browse community contributions on GitHub Discussions.

* **AI Chat with Thinking Mode**: Engage in fluid, multi-turn conversations and toggle the new Thinking Mode to peek "under the hood." This feature allows you to see the model’s step-by-step reasoning process, which is perfect for understanding complex problem-solving. Note: Thinking Mode currently works with supported models, starting with the Gemma 4 family.

* **Ask Image**: Use multimodal power to identify objects, solve visual puzzles, or get detailed descriptions using your device’s camera or photo gallery.

* **Audio Scribe**: Transcribe and translate voice recordings into text in real-time using high-efficiency on-device language models.

*   **Live Voice Chat (Kabem Voice)**: Engage in real-time, hands-free spoken conversations with local LLMs. Voice and keyboard share a persistent timeline, and normal speech is sent automatically when silence ends the turn. Transcript review is reserved for low-confidence recognition, while Markdown responses, contextual copy/share/edit/regenerate actions and response replay remain available. Multimodal turns support camera/gallery, audio and PDF study; unsafe edit/regenerate actions are conservatively disabled when their media payload cannot be reconstructed. Sessions, preferences and **Personal Memory** remain local to the device.

*   **Conversa bilíngue local**: português e inglês compartilham o mesmo roteador de idioma, histórico e fluxo de voz. O Kabem detecta o idioma do turno, mantém continuações curtas no contexto correto e alterna reconhecimento e TTS sem usar uma segunda inferência. No Android 14+, a troca automática é habilitada somente quando os dois pacotes de reconhecimento estão instalados; em qualquer versão, uma voz local de outro idioma nunca é usada como substituta.

*   **Prompt Lab**: A dedicated workspace to test different prompts and single-turn use cases with granular control over model parameters like temperature and top-k.

*   **Mobile Actions**: Unlock offline device controls and automated tasks powered entirely by a finetune of FunctionGemma 270m.

*   **Tiny Garden**: A fun, experimental mini-game that uses natural language to plant and harvest a virtual garden using a finetune of FunctionGemma 270m.

* **Model Management & Benchmark**: Gallery is a flexible sandbox for a wide variety of open-source models. Easily download models from the list or load your own custom models. Manage your model library effortlessly and run benchmark tests to understand exactly how each model performs on your specific hardware.

* **100% On-Device Privacy**: All model inferences happen directly on your device hardware. No internet is required, ensuring total privacy for your prompts, images, and sensitive data.

## 🏁 Get Started in Minutes!

1. **Check OS Requirement**: Android 12 and up, and iOS 17 and up.
2.  **Download the App:**
    - Install the app from [Google Play](https://play.google.com/store/apps/details?id=com.google.ai.edge.gallery) or [App Store](https://apps.apple.com/us/app/google-ai-edge-gallery/id6749645337).
    - For users without Google Play access: install the apk from the [**latest release**](https://github.com/google-ai-edge/gallery/releases/latest/)
3.  **Install & Explore:** For detailed installation instructions (including for corporate devices) and a full user guide, head over to our [**Project Wiki**](https://github.com/google-ai-edge/gallery/wiki)!

## 🎨 Design System

*   **Identidade:** paleta coral/verde/azul suave, tipografia responsiva e acessível.
*   **Material Design 3:** tema dinâmico com tokens de cor próprios para modo claro e escuro.
*   **Componentes:** TopBar, PromoScreen e TemsOfUse reestilizados com a marca Kabem Voice.
*   **Live Voice:** controles de galeria/câmera espaçados, animação de waveform e chip de perfil de resposta.

## 💬 Conversa híbrida do Kabem Voice

*   **Voz e teclado no mesmo histórico:** alterne o modo de entrada sem criar uma conversa paralela.
*   **Conversa contínua:** a fala é enviada automaticamente após o silêncio indicar o fim do turno, sem tela intermediária.
*   **Correção sem interromper a conversa:** toda fala segue direto para o modelo; se a transcrição estiver errada, use **Editar e reenviar** no menu da mensagem.
*   **Retorno automático:** depois que o Kabem termina de falar, ele volta a ouvir para manter o ritmo de uma conversa natural.
*   **Ações contextuais:** toque longo ou menu de overflow para copiar, compartilhar, editar, regenerar ou reproduzir respostas, conforme a policy e as capacidades disponíveis.
*   **Histórico resiliente:** mensagens possuem IDs persistentes, migração de sessões legadas, revisão monotônica e rollback de edições que falhem.
*   **Segurança multimodal:** turnos com imagem, áudio ou PDF não oferecem edição/regeneração quando o contexto original não pode ser restaurado integralmente.

Detalhes de implementação, limitações e matriz de validação estão no [plano de paridade de conversa](docs/superpowers/plans/2026-07-16-smart-conversation-parity-implementation.md).

## 🌐 Português e inglês

*   **Idioma por turno:** comandos explícitos, reconhecimento do Android, análise textual e contexto anterior alimentam uma única decisão de idioma.
*   **Continuidade natural:** respostas curtas como “sim”, “continua” e “okay” preservam o idioma estabelecido em vez de alternar sem motivo.
*   **Professor de Inglês:** explicações usam português e exemplos, repetição e conversa livre usam `EN-US` ou `EN-GB` conforme a atividade.
*   **Streaming seguro:** cada trecho é validado antes do TTS; marcadores internos não aparecem na tela nem são falados.
*   **Privacidade offline:** reconhecimento e vozes locais são verificados separadamente. O app informa quando falta pacote, voz ou suporte à troca automática.
*   **Sessões persistentes:** o idioma estabelecido é restaurado com a conversa; uma nova sessão começa em português.

O chip de idioma mostra `PT`, `EN-US`, `EN-GB` ou `PT/EN automático`. A indicação de prontidão bilíngue offline só aparece quando os pacotes de reconhecimento e as vozes locais de ambos os idiomas foram confirmados.

## 🧠 Professor Adaptativo (Adaptive Teacher)

*   **Detecção de Intenção:** Identifica quando o usuário deseja aprender e ativa o modo pedagógico automaticamente.
*   **Ensino em Blocos:** Explicações curtas seguidas de verificações de entendimento.
*   **Sinais de Feedback:** Reconhece respostas como "entendi", "não entendi", "dê um exemplo" e ajusta a estratégia de ensino (Socratic scaffolding).
*   **Avaliação Formativa:** Avalia tentativas do aluno com correções construtivas, sem rotular o usuário.

## 🛠️ Technology Highlights

*   **Google AI Edge:** Core APIs e ferramentas para ML on-device.
*   **LiteRT:** runtime leve para execução otimizada de modelos.
*   **Android TTS / SpeechRecognizer:** síntese e reconhecimento de voz nativos, sem dependência de nuvem.
*   **PDFBox Android:** leitura e indexação de PDFs para estudo por voz.
*   **DataStore + Protobuf:** persistência local de sessões, memórias e preferências.
*   **Jetpack Compose + Material 3:** UI declarativa com tema personalizado Kabem Voice.

## ⌨️ Development

Check out the [development notes](DEVELOPMENT.md) for instructions about how to build the app locally.

## 🤝 Feedback

This is an **experimental Beta release**, and your input is crucial!

*   🐞 **Found a bug?** [Report it here!](https://github.com/google-ai-edge/gallery/issues/new?assignees=&labels=bug&template=bug_report.md&title=%5BBUG%5D)
*   💡 **Have an idea?** [Suggest a feature!](https://github.com/google-ai-edge/gallery/issues/new?assignees=&labels=enhancement&template=feature_request.md&title=%5BFEATURE%5D)

## 📄 License

Licensed under the Apache License, Version 2.0. See the [LICENSE](LICENSE) file for details.

## 🔗 Useful Links

*   [**Project Wiki (Detailed Guides)**](https://github.com/google-ai-edge/gallery/wiki)
*   [Hugging Face LiteRT Community](https://huggingface.co/litert-community)
*   [LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM)
*   [Google AI Edge Documentation](https://ai.google.dev/edge)
