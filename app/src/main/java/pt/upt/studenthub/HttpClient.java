package pt.upt.studenthub;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class HttpClient {
  static final int TIMEOUT_MS = 10_000;

  // Impede a criação de instâncias desta classe.
  private HttpClient() {}

  public static final class HttpStatusException extends IOException {
    public final int code;

    // Guarda o código HTTP recebido.
    public HttpStatusException(int code) {
      super("HTTP " + code);
      this.code = code;
    }
  }

  // Executa o GET e fecha os recursos da ligação.
  public static String get(String url) throws IOException {
    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
    try {
      conn.setRequestMethod("GET");
      conn.setConnectTimeout(TIMEOUT_MS);
      conn.setReadTimeout(TIMEOUT_MS);
      conn.setRequestProperty("Accept", "application/json");
      int code = conn.getResponseCode();
      if (code != HttpURLConnection.HTTP_OK) {
        throw new HttpStatusException(code);
      }
      StringBuilder body = new StringBuilder();
      try (BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedIOException("Request cancelled");
          }
          body.append(line);
        }
      }
      return body.toString();
    } finally {
      conn.disconnect();
    }
  }
}
