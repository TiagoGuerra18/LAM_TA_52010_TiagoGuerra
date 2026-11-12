# UPT Student Hub — Semana 7

Projeto de Tiago Guerra (52010), continuado a partir do StudentHub1 da Semana 6.

## Abrir

1. Extrair o ZIP para uma pasta local.
2. No Android Studio, escolher Open e selecionar a pasta StudentHub que contém settings.gradle.kts.
3. Usar o JDK incorporado do Android Studio ou JDK 21 para o Gradle e aguardar a sincronização.
4. Executar num dispositivo Android API 24 ou superior com Internet.

O projeto mantém o Gradle 9.6.0, o Android Gradle Plugin 9.4.1 e compileSdk/targetSdk 37 da base enviada. O SDK Android 37 deve estar instalado. Os ficheiros .gradle.kts são configuração de build; o código da aplicação é Java.

## Correspondência com o guia

0. Continuação do Módulo 1, mantendo o horário e o respetivo ViewModel.
1. Permissões INTERNET e ACCESS_NETWORK_STATE e declaração de WeatherActivity.
2. strings_weather.xml e mensagem Sobre atualizada.
3. menu_weather.xml com Refresh, demonstração na main thread, erro HTTP e Sobre.
4. Segundo botão na MainActivity e layouts portrait/landscape com os mesmos IDs.
5. Validação partilhada do nome, Intent com extra e StrictMode em debug.
6. WeatherReport, WeatherCodes, WeatherApi e WeatherParser.
7. HttpClient com HttpURLConnection, GET, timeouts de 10 s, UTF-8 e limpeza dos recursos.
8. WeatherViewModel com ExecutorService, Handler e LiveData de leitura.
9. WeatherActivity com observadores, animação, BroadcastReceiver e NetworkCallback registados em onStart e removidos em onStop.
10–11. Plano de testes e mensagens de erro descritos abaixo.

O package e applicationId foram alinhados com pt.upt.studenthub e o minSdk com 24, conforme o enunciado. O tema Theme.StudentHub1 foi preservado. Foram acrescentadas as cores upt_navy e upt_light, ausentes na base.

Os ícones adaptativos foram movidos para mipmap-anydpi-v26 para permitir a instalação em API 24–25, que usam os ícones convencionais existentes. A configuração de daemon que fixava um JDK 25 específico foi retirada para permitir usar o JDK escolhido no Android Studio.

## Validação realizada

- Build Gradle: assembleDebug, lintDebug e testDebugUnitTest concluídos com BUILD SUCCESSFUL usando JDK 21.
- Android Lint: sem erros; existem avisos de manutenção, recursos não utilizados e sugestões sobre o módulo original. O relatório está em VALIDACAO/lint-results-debug.html.
- O teste unitário existente é básico; a sua aprovação não valida os fluxos de rede nem a interface.
- Não havia emulador/dispositivo ligado. Modo de voo, rotação, comportamento visual e integração real com Open-Meteo precisam de confirmação no dispositivo.

## Testes manuais no emulador

- Nome vazio: deve impedir a navegação e mostrar erro no campo.
- Nome preenchido: abre a meteorologia e inicia um pedido automaticamente.
- Durante o pedido: mostra spinner e desativa Refresh e a simulação de erro.
- Sucesso: mostra temperatura, descrição WMO, humidade, vento e hora, com animação.
- Vários toques em Refresh: apenas um pedido de cada vez.
- Simulate server error: mostra o código HTTP e mantém os últimos dados válidos.
- Fetch on main thread: mostra o Toast da exceção e mantém a aplicação aberta.
- Modo de voo ligado: Toast, banner offline e mensagem de erro ao atualizar.
- Modo de voo desligado após erro: atualiza automaticamente se a checkbox estiver marcada.
- Checkbox desmarcada: não atualiza automaticamente quando a rede regressa.
- Rodar durante um pedido: mantém o pedido e usa o layout horizontal; conserva a checkbox.
- Seta Up: regressa ao ecrã inicial com o nome; menu Sobre funciona.
- Sair do ecrã: confirmar ausência de avisos de receiver não removido no Logcat.
- Abrir o horário: confirmar que o Módulo 1 continua a funcionar.
- Network Inspector: confirmar GET, parâmetros do campus, Accept: application/json e resposta JSON.

Não foram acrescentadas as extensões opcionais E1–E3. Os testes manuais acima requerem um emulador ou dispositivo e não devem ser considerados executados apenas por a compilação passar.
