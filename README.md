# UPT Student Hub - Tiago Guerra (52010)

Projeto de Laboratório de Aplicações Móveis, Universidade Portucalense.
Android nativo, Java, variante **V1 - Previsão semanal**.

O projeto continua a base pessoal da Semana 7 e mantém o horário (Módulo 1)
e a meteorologia do campus (Módulo 2). O Módulo 3 acrescenta o Explorer.
A entrega é realizada pelo GitHub, conforme a indicação atualizada do professor.

## Abrir e executar

1. Clonar este repositório e abrir a pasta raiz no Android Studio.
2. Usar o **JDK 25** incluído no Android Studio instalado, ou outro JDK 25.
3. Instalar o **Android SDK 37** e deixar o Gradle sincronizar as dependências.
4. Executar `app` num emulador **API 34** ou dispositivo compatível.

Configuração preservada da base: Gradle 9.6.0, Android Gradle Plugin 9.4.1,
compileSdk/targetSdk 37 e minSdk 24. O applicationId é
`pt.upt.studenthub.a52010`; o namespace Java continua `pt.upt.studenthub`.
O Android Studio configura o seu caminho local do SDK; `local.properties`
não é partilhado no Git.

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat lintDebug testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
```

O último comando precisa de um emulador/dispositivo ligado.
Em macOS/Linux, usar `./gradlew` em vez de `.\gradlew.bat`.
O APK de debug é gerado em `app/build/outputs/apk/debug/app-debug.apk`.

Os ficheiros `.gradle.kts` são configuração de compilação, não código Kotlin da
aplicação. A rede de produção usa exclusivamente `HttpURLConnection`.
Robolectric e MockWebServer são dependências exclusivas dos testes; o runner
AndroidJUnit executa o teste instrumentado no dispositivo.

## Utilização

- Introduzir o nome no ecrã inicial e abrir **Explorer**.
- Consultar máxima, mínima, probabilidade de chuva e ícone WMO de cada dia.
- Tocar num dia para consultar o pedido horário desse dia e marcar favorito.
- Em paisagem, o detalhe aparece ao lado da lista, sem Fragments.
- No menu, escolher Porto, Lisboa ou Faro, atualizar, comparar ou configurar.
- As definições permitem Celsius/Fahrenheit, 3/5/7 dias, limiar de chuva de
  0 a 100%, atualização entre 15 e 3600 segundos e destaque das horas chuvosas.
- Cancelar conserva os dados anteriores. A rotação conserva os pedidos,
  preferências, favoritos e posição da lista.
- A faixa offline acompanha a rede; o regresso da ligação repete uma lista
  que tenha falhado. A bateria fraca suspende a atualização automática.
- Em debug, existem opções para provocar HTTP 404 e falha apenas em Faro.

Os dados da Open-Meteo não exigem chave de API e usam `Europe/Lisbon`.
As medições nulas são apresentadas como indisponíveis, nunca como zero.
As preferências e os favoritos não têm persistência entre sessões: vivem no
ViewModel e no estado temporário das activities, conforme o âmbito do trabalho.
Não são usados SQLite, SharedPreferences, mapas nem GPS.

As antigas demonstrações de bloquear a interface e tentar rede na thread
principal foram retiradas. O horário continua a sincronizar em segundo plano,
e a meteorologia do campus mantém atualização e tratamento de erros.

## Organização

| Componente | Responsabilidade |
|---|---|
| `ExplorerActivity` | Lista, menus, resultados, rede, bateria, temporizador e dois painéis |
| `DetailActivity` / `DetailRenderer` | Pedido B, horas, cartão e resultado do favorito |
| `SettingsActivity` | Validação e devolução das escolhas |
| `CompareActivity` | Três cartões com resultados independentes |
| `ExplorerViewModel` | LiveData, executores, Futures, gerações e estado em memória |
| `ExplorerApi` | Todos os URLs construídos com Uri.Builder |
| `HttpClient` | GET, timeouts, UTF-8, interrupção e fecho dos recursos |
| `ExplorerParser` / `ExplorerModels` | JSON validado, arrays alinhados e objetos imutáveis |
| `ItemAdapter` | ListView personalizada e reciclagem por ViewHolder |
| `ExplorerState` / `ExplorerUi` | Estado de domínio e tradução para recursos visíveis |

O pedido A e o pedido B têm executores independentes. A comparação usa um
coordenador fora da main thread e `Executors.newFixedThreadPool(3)`; `invokeAll`
agrega três resultados, incluindo falhas individuais. Os resultados chegam à
main thread por Handler. Identificadores de geração impedem que uma resposta
cancelada substitua uma resposta mais recente. `onCleared()` cancela Futures,
executa `shutdownNow()` e remove os callbacks.

A Activity agenda a atualização com Handler apenas entre `onStart` e `onStop`.
Callbacks de rede passam por `runOnUiThread`; receivers e NetworkCallback são
registados e removidos nesses mesmos limites. O ViewModel não guarda Context,
Activity ou View. O StrictMode em debug impede rede na thread principal.

## Mapa dos requisitos funcionais

Os ficheiros Java encontram-se em `app/src/main/java/pt/upt/studenthub/`.

| RF | Implementação principal |
|---|---|
| RF1 | `MainActivity.openWith`, `ExplorerActivity.onCreate` |
| RF2 | `ItemAdapter.getView`, `item_forecast.xml` |
| RF3 | `ExplorerState`, `ExplorerActivity.renderDays`, `DetailRenderer.render` |
| RF4 | `ExplorerViewModel.cancel`, `HttpClient.checkInterrupted` |
| RF5 | `ExplorerActivity.openDay`, `DetailActivity`, `loadHours` |
| RF6 | `DetailActivity.returnFavorite`, launcher `detailResult`, `setFavorite` |
| RF7 | `toggleFavorite`, `toggleInlineFavorite`, AlertDialog e Toast |
| RF8 | `SettingsActivity.save`, launcher `settingsResult` |
| RF9 | `ExplorerViewModel.loadComparison`, `CompareActivity.render` |
| RF10 | `layout-land/activity_explorer.xml`, `openDay`, `DetailRenderer` |
| RF11 | `menu_explorer.xml`, métodos do menu e `ExplorerScreen.onSupportNavigateUp` |
| RF12 | `res/anim/ex_list.xml`, `ex_card_enter.xml` e transições `ex_screen_*` |
| RF13 | `ExplorerActivity.networkCallback`, `updateNetwork` |
| RF14 | `refreshTick`, `scheduleRefresh`, `onStart` e `onStop` |
| RF15 | `batteryReceiver`, `setBatteryLow` |
| RF16 | `ExplorerState.failed`, `ExplorerUi.state`, estados EMPTY |
| RF17 | `ExplorerViewModel`, `ExplorerActivity.onSaveInstanceState`, ListView |
| RF18 | `ExplorerScreen.log`, tag `ExplorerLifecycle` |
| RF19 | `TimetableActivity`, `WeatherActivity`, `AboutDialog`, `about_message` |

## Mapa dos requisitos técnicos

| RT | Implementação / verificação |
|---|---|
| RT1 | Fontes `.java`, minSdk 24 e targetSdk 37 |
| RT2 | Dependências em `app/build.gradle.kts`; bibliotecas de testes fora de produção |
| RT3 | `HttpClient.get` e testes com MockWebServer |
| RT4 | `ExplorerApi` e `ExplorerApiTest` |
| RT5 | `ExplorerParser`, `ExplorerModels`, `ExplorerParserTest` |
| RT6 | `ExplorerViewModel`, `onCleared`, StrictMode em `ExplorerScreen` e `MainActivity` |
| RT7 | ViewModel sem referências a Context, Activity ou View |
| RT8 | `ExplorerActivity.onStart/onStop`, RECEIVER_NOT_EXPORTED, runOnUiThread |
| RT9 | `res/values`, ícones vetoriais, Lint sem HardcodedText |
| RT10 | Javadoc nas classes e métodos públicos; comentários nas regras de concorrência |
| RT11 | Histórico Git com commits descritivos por etapa |
| RT12 | Compilação Gradle e instalação/testes em emulador API 34 |

## Testes automáticos

A suíte JVM tem **30 testes**, incluindo o teste inicial da base:

- `ExplorerParserTest`: dados válidos, nulos, vazio, arrays desalinhados,
  datas inválidas, tipos incorretos, probabilidades e imutabilidade.
- `HttpClientTest`: UTF-8, HTTP 503, timeout e interrupção antes de ligar.
- `ExplorerViewModelTest`: offline sem pedido, pedido duplicado, cancelamento,
  retenção dos dados, resposta tardia cancelada, favoritos e paralelismo com falha parcial.
- `ExplorerRotationTest`: recriação da Activity durante LOADING com transporte controlado;
  mantém o mesmo ViewModel, uma chamada e entrega o resultado ao novo ecrã.
- `ExplorerApiTest`: local, unidade, dias, timezone e data escolhida.
- `SettingsActivityTest`: erro no campo, resultado válido e formulário após recriação.

O teste instrumentado confirma o applicationId no emulador. As requisições dos
testes JVM usam dados de exemplo ou MockWebServer e não dependem da API pública.
As tags `ExplorerLifecycle` e `ExplorerRequests` permitem seguir recriações e
contar pedidos no Logcat.

## Plano de testes no emulador - Anexo B

| ID | Procedimento e resultado esperado |
|---|---|
| T1 | Abrir com rede: LOADING seguido da lista com animação |
| T2 | Abrir em modo de avião: faixa offline e erro sem ligação |
| T3 | Restabelecer a rede depois de T2: atualização automática |
| T4 | Rede lenta e Cancelar: CANCELLED, conservando dados anteriores |
| T5 | Rodar durante LOADING: uma chamada, resultado na nova orientação |
| T6 | Em paisagem, tocar numa linha: painel atualizado na mesma Activity |
| T7 | Marcar favorito no detalhe e voltar: estrela na linha correspondente |
| T8 | Remover favorito: confirmação, possibilidade de manter e Toast final |
| T9 | Guardar valores inválidos: campos com erro, sem resultado |
| T10 | Guardar valores válidos: lista recarregada com novas definições |
| T11 | Menu debug de falha em Faro: dois cartões com dados e Faro com HTTP 404 |
| T12 | Bateria fraca: aviso e suspensão periódica; bateria normal retoma |
| T13 | Simular HTTP 404 no menu debug: código visível, dados anteriores conservados |
| T14 | Abrir e usar o horário e a meteorologia do campus |

A recriação durante o pedido é verificada de forma controlada por
`ExplorerRotationTest`. No dispositivo, os eventos `daily start`, ciclo de vida
e `daily finish` permitem acompanhar a ordem e contar os pedidos.
Para testar rede lenta, usar os Extended Controls do emulador. Para bateria,
usar os respetivos controlos e repor os valores normais no final.

## Verificação realizada

- Compilação de debug concluída e APK instalado na API 34.
- Lint sem erros e sem avisos HardcodedText.
- 30 testes JVM concluídos sem falhas.
- 1 teste instrumentado executado e aprovado na API 34.
- No emulador: dados reais, detalhe, favoritos, confirmação de remoção,
  definições inválidas/válidas, dois painéis, posição da lista após rotação,
  cancelamento, abertura offline, recuperação automática, HTTP 404, falha
  parcial na comparação, bateria e paragem/retoma do temporizador.
- Horário e meteorologia do campus verificados após a limpeza das demonstrações.
- O cenário de recriação com pedido em curso é coberto pelo teste controlado
  ExplorerRotationTest; a rotação e os layouts também foram verificados no emulador.
