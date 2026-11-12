package pt.upt.studenthub;

import java.net.SocketTimeoutException;
import org.json.JSONException;

/** Immutable request snapshot. Previous data can remain visible during load/cancellation. */
public final class ExplorerState<T> {
    /** Visible states required by RF3. */
    public enum Status { IDLE, LOADING, SUCCESS, EMPTY, ERROR, CANCELLED }
    /** Domain errors; the Activity, rather than the ViewModel, chooses human-readable text. */
    public enum Error { NONE, NO_CONNECTION, TIMEOUT, HTTP, INVALID_DATA }
    public final Status status;
    public final T data;
    public final Error error;
    public final int httpCode;

    private ExplorerState(Status status, T data, Error error, int httpCode) {
        this.status = status;
        this.data = data;
        this.error = error;
        this.httpCode = httpCode;
    }

    /** Creates a state without a failure. */
    public static <T> ExplorerState<T> of(Status status, T data) {
        return new ExplorerState<>(status, data, Error.NONE, 0);
    }

    /** Explicit offline result without attempting a connection. */
    public static <T> ExplorerState<T> offline(T data) {
        return new ExplorerState<>(Status.ERROR, data, Error.NO_CONNECTION, 0);
    }

    /** Maps transport and parsing exceptions without retaining exceptions or Context objects. */
    public static <T> ExplorerState<T> failed(T data, Exception exception) {
        Error error = Error.NO_CONNECTION;
        int code = 0;
        if (exception instanceof SocketTimeoutException) error = Error.TIMEOUT;
        else if (exception instanceof HttpClient.HttpStatusException) {
            error = Error.HTTP;
            code = ((HttpClient.HttpStatusException) exception).code;
        } else if (exception instanceof JSONException || exception instanceof IllegalArgumentException) {
            error = Error.INVALID_DATA;
        }
        return new ExplorerState<>(Status.ERROR, data, error, code);
    }
}
