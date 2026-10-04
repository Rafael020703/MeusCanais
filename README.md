<div align="center">

# SQUI TV 📺
### Sua experiência de entretenimento na Android TV

[![Android](https://img.shields.io/badge/Platform-Android%20TV-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/tv)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.x-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square&logo=jetpack-compose&logoColor=white)](https://developer.android.com/compose)
[![Version](https://img.shields.io/badge/Version-1.7%20(Code%208)-0284C7?style=flat-square)](https://github.com/rafasqui/MeusCanais)

</div>

---

## ✨ Sobre o Squi TV

**Squi TV** é um aplicativo moderno desenvolvido exclusivamente para **Android TV**, oferecendo uma experiência de streaming imersiva, limpa e de alta performance. Projetado desde a raiz para navegação por controle remoto (D-pad), o Squi TV combina o poder do Jetpack Compose para TV com arquitetura moderna baseada em Clean Architecture / MVVM.

---

## 🚀 Novidades da Versão 1.7

* **Correção Definitiva de Foco e Teclado (Android TV):** A tela de login foi aprimorada para abrir pronta para navegação via D-pad sem disparar o teclado virtual (IME) automaticamente. O teclado só é aberto mediante escolha explícita do usuário.
* **Padronização da Porta 1234 no Pairing:** O servidor HTTP local para pareamento por rede Wi-Fi/LAN agora utiliza de forma determinística a porta **1234**, facilitando a digitação manual de endereços (`http://<IP>:1234`).
* **Polimento Visual do Site de Pairing (Dark Premium):** A interface web servida pela TV para configuração de credenciais recebeu um redesign completo com identidade visual *Squi TV Dark Premium*, design responsivo e botão de cópia rápida de URL.
* **Resiliência e Concorrência:** Eliminação de *race conditions* na inicialização do servidor local e tratamento robusto de conflitos de porta.

---

## 🎬 Principais Recursos

* 📺 **Otimizado para Android TV:** Navegação nativa e fluida com D-pad e controle remoto.
* 🔐 **Login Xtream Codes / IPTV:** Conexão segura com servidores de streaming compatíveis.
* 🌐 **Pareamento por Rede Local (TV Pairing):** Configure seu login na TV acessando o navegador do celular ou computador na mesma rede Wi-Fi (`http://<IP>:1234`) com token seguro de uso único e expiração de 5 minutos.
* 🏠 **Dashboard Imersivo:** Acesso rápido a conteúdos recentes, favoritos e categorias organizadas.
* 📡 **TV ao Vivo (Live TV):** Grade de canais ao vivo com suporte completo a EPG (Guia Eletrônico de Programação).
* 🍿 **VOD & Séries:** Catálogos completos de filmes e séries organizados por temporadas e episódios com progresso de reprodução.
* 🎥 **Player Media3 / ExoPlayer:** Reprodução de alta performance com aceleração de hardware e suporte a streams adaptativos.
* 🔄 **Atualizações via GitHub Releases:** Verificação e instalação automática de atualizações diretamente pelo app.

---

## 🔗 Pareamento por Rede Local (TV Pairing)

1. Abra a tela de **Login** na sua Android TV e selecione **"Parear por rede local"**.
2. A TV exibirá o endereço de acesso na porta padrão `1234` (ex: `http://192.168.1.50:1234`) e um código de 8 dígitos.
3. No seu celular ou computador conectado à mesma rede Wi-Fi, abra o navegador e acesse o endereço informado.
4. Insira suas credenciais (Usuário e Senha). O aplicativo autenticará e configurará a TV instantaneamente.

---

## 🛠️ Stack Tecnológica

* **Linguagem:** Kotlin (Coroutines & Flows)
* **Interface:** Jetpack Compose & Material 3
* **Mídia:** Jetpack Media3 (ExoPlayer)
* **Injeção de Dependências:** Dagger Hilt
* **Persistência:** Room Database & DataStore Preferences
* **Rede:** Retrofit, OkHttp & Kotlinx Serialization
* **Background Workers:** WorkManager

---

## 📋 Changelog v1.7

* Implementação de servidor HTTP local suspensivo com ligação determinística na porta **1234**.
* Correção de foco inicial na tela de Login para evitar abertura indesejada do IME no Android TV.
* Redesign da página HTML de pairing com tema *Squi TV Dark Premium*.
* Aprimoramento dos testes unitários de rede e sessões de pareamento.

---

## 📄 Licença

Desenvolvido por **rafasqui**. Todos os direitos reservados.
