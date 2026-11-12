package pt.upt.studenthub;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.json.JSONException;

public class WeatherViewModel extends ViewModel {
  public enum Status {
    IDLE,
    LOADING,
    SUCCESS,
    ERROR
  }

  public enum ErrorType {
    NO_CONNECTION,
    TIMEOUT,
    HTTP,
    PARSE
  }

  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final Handler mainHandler = new Handler(Looper.getMainLooper());
  private final MutableLiveData<Status> status = new MutableLiveData<>(Status.IDLE);
  private final MutableLiveData<WeatherReport> report = new MutableLiveData<>();
  private final MutableLiveData<ErrorType> error = new MutableLiveData<>();
  private int lastHttpCode;
  private Future<?> currentTask;

  // Expõe o estado do pedido.
  public LiveData<Status> getStatus() {
    return status;
  }

  // Expõe o último relatório recebido.
  public LiveData<WeatherReport> getReport() {
    return report;
  }

  // Expõe o tipo do último erro.
  public LiveData<ErrorType> getError() {
    return error;
  }

  // Devolve o último código de erro HTTP.
  public int getLastHttpCode() {
    return lastHttpCode;
  }

  // Descarrega e interpreta os dados na worker thread.
  public void load(String url) {
    if (status.getValue() == Status.LOADING) {
      return;
    }
    status.setValue(Status.LOADING);
    currentTask =
        executor.submit(
            () -> {
              try {
                String body = HttpClient.get(url);
                WeatherReport result = WeatherParser.parse(body);
                mainHandler.post(
                    () -> {
                      report.setValue(result);
                      status.setValue(Status.SUCCESS);
                    });
              } catch (HttpClient.HttpStatusException e) {
                postError(ErrorType.HTTP, e.code);
              } catch (SocketTimeoutException e) {
                postError(ErrorType.TIMEOUT, 0);
              } catch (IOException e) {
                postError(ErrorType.NO_CONNECTION, 0);
              } catch (JSONException e) {
                postError(ErrorType.PARSE, 0);
              }
            });
  }

  // Publica o erro na thread principal.
  private void postError(ErrorType type, int httpCode) {
    mainHandler.post(
        () -> {
          lastHttpCode = httpCode;
          error.setValue(type);
          status.setValue(Status.ERROR);
        });
  }

  @Override
  // Cancela o trabalho e liberta os recursos.
  protected void onCleared() {
    if (currentTask != null) {
      currentTask.cancel(true);
    }
    executor.shutdownNow();
    mainHandler.removeCallbacksAndMessages(null);
    super.onCleared();
  }
}
