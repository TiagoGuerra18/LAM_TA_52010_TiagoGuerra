package pt.upt.studenthub;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Small GET client shared by the weather and Explorer modules. */
public final class HttpClient {
  static final int TIMEOUT_MS = 10_000;

  // Impede a criação de instâncias desta classe.
  private HttpClient() {}

  /** Carries the HTTP status without exposing server response bodies to the UI. */
  public static final class HttpStatusException extends IOException {
    public final int code;

    /** Creates an HTTP error with the received status code. */
    public HttpStatusException(int code) {
      super("HTTP " + code);
      this.code = code;
    }
  }

  /** Executes a UTF-8 GET with bounded timeouts and cooperative cancellation. */
  public static String get(String url) throws IOException {
    return get(url, TIMEOUT_MS);
  }

  // Package-private timeout override keeps transport tests fast and independent of the Internet.
  static String get(String url, int timeoutMs) throws IOException {
    checkInterrupted();
    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
    try {
      conn.setRequestMethod("GET");
      conn.setConnectTimeout(timeoutMs);
      conn.setReadTimeout(timeoutMs);
      conn.setRequestProperty("Accept", "application/json");
      checkInterrupted();
      int code = conn.getResponseCode();
      checkInterrupted();
      if (code != HttpURLConnection.HTTP_OK) {
        throw new HttpStatusException(code);
      }
      StringBuilder body = new StringBuilder();
      try (BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          checkInterrupted();
          body.append(line);
          if (body.length() > 2_000_000) throw new IOException("Response too large");
        }
      }
      checkInterrupted();
      return body.toString();
    } finally {
      conn.disconnect();
    }
  }

  private static void checkInterrupted() throws InterruptedIOException {
    if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Request cancelled");
  }
}
