package pt.upt.studenthub;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Owns cancellable timetable synchronization across rotation. */
public class TimetableViewModel extends ViewModel {

  // Estados possíveis da sincronização.

  /** Timetable synchronization states. */
  public enum State {
    IDLE,
    RUNNING,
    DONE,
    CANCELLED
  }

  // Tempo de carregamento simulado de cada aula.

  private static final long SIMULATED_DELAY_MS = 800;

  // Uma única thread executa as tarefas de segundo plano.

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  // Permite enviar atualizações para a thread principal.

  private final Handler mainHandler = new Handler(Looper.getMainLooper());

  // Dados mutáveis privados: só o ViewModel os pode alterar.

  private final MutableLiveData<State> state = new MutableLiveData<>(State.IDLE);

  private final MutableLiveData<Integer> progress = new MutableLiveData<>(0);

  private final MutableLiveData<List<String>> sessions =
      new MutableLiveData<>(Collections.emptyList());

  // Referência à tarefa atual para permitir o cancelamento.

  private Future<?> currentTask;

  // Expõe os dados à Activity apenas para observação.

  /** Read-only synchronization state. */
  public LiveData<State> getState() {

    return state;
  }

  /** Read-only progress percentage. */
  public LiveData<Integer> getProgress() {

    return progress;
  }

  /** Read-only immutable session snapshot. */
  public LiveData<List<String>> getSessions() {

    return sessions;
  }

  // Chamado na thread principal para iniciar a sincronização.

  /** Starts background synchronization unless one is already running. */
  public void startSync(String[] source) {

    // Impede iniciar uma segunda sincronização enquanto decorre a primeira.

    if (state.getValue() == State.RUNNING) {

      return;
    }

    // Limpa os resultados anteriores e inicia um novo carregamento.

    sessions.setValue(Collections.emptyList());

    progress.setValue(0);

    state.setValue(State.RUNNING);

    // O código dentro do submit corre na thread de trabalho.

    currentTask =
        executor.submit(
            () -> {

              // Esta lista é utilizada apenas pela thread de trabalho.

              List<String> loaded = new ArrayList<>();

              try {

                for (int i = 0; i < source.length; i++) {

                  // Verifica se foi pedida a interrupção da tarefa.

                  if (Thread.currentThread().isInterrupted()) {

                    throw new InterruptedException();
                  }

                  // Simula a demora do servidor sem bloquear a interface.

                  Thread.sleep(SIMULATED_DELAY_MS);

                  // Guarda a aula que acabou de ser carregada.

                  loaded.add(source[i]);

                  // Cria uma cópia imutável para enviar à thread principal.

                  final List<String> snapshot =
                      Collections.unmodifiableList(new ArrayList<>(loaded));

                  // Calcula a percentagem concluída.

                  final int pct = (i + 1) * 100 / source.length;

                  // Atualiza os LiveData na thread principal.

                  mainHandler.post(
                      () -> {
                        sessions.setValue(snapshot);

                        progress.setValue(pct);
                      });
                }

                // Comunica que todas as aulas foram carregadas.

                mainHandler.post(() -> state.setValue(State.DONE));

              } catch (InterruptedException e) {

                // Restaura o sinal de interrupção.

                Thread.currentThread().interrupt();

                // Comunica o cancelamento à interface.

                mainHandler.post(() -> state.setValue(State.CANCELLED));
              }
            });
  }

  // Pede a interrupção da tarefa em execução.

  /** Requests interruption of the current synchronization. */
  public void cancelSync() {

    if (currentTask != null) {

      currentTask.cancel(true);
    }
  }

  // Limpa os dados apenas quando não existe sincronização em execução.

  /** Clears completed data when no synchronization is running. */
  public void clear() {

    if (state.getValue() == State.RUNNING) {

      return;
    }

    sessions.setValue(Collections.emptyList());

    progress.setValue(0);

    state.setValue(State.IDLE);
  }

  // Liberta os recursos quando o ViewModel termina.

  // Uma simples rotação não provoca esta limpeza.

  @Override
  protected void onCleared() {

    cancelSync();

    executor.shutdownNow();

    mainHandler.removeCallbacksAndMessages(null);

    super.onCleared();
  }
}
