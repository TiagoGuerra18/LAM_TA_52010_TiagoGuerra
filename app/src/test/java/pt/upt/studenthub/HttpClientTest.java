package pt.upt.studenthub;

import static org.junit.Assert.*;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Uses a loopback HTTP server, never the public Open-Meteo service. */
public class HttpClientTest {
    private MockWebServer server;
    private String base;

    @Before public void start() throws Exception {
        server = new MockWebServer();
        server.start();
        base = server.url("/").toString();
    }
    @After public void stop() throws Exception { Thread.interrupted(); server.shutdown(); }
    @Test public void readsUtf8() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"cidade\":\"São João\"}"));
        assertTrue(HttpClient.get(base + "/ok").contains("São João"));
    }
    @Test public void reportsActualHttpStatus() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(503));
        try { HttpClient.get(base + "/failure"); fail("Expected HTTP failure"); }
        catch (HttpClient.HttpStatusException exception) { assertEquals(503, exception.code); }
    }
    @Test(expected = SocketTimeoutException.class) public void boundsReadTime() throws Exception {
        server.enqueue(new MockResponse().setBody("slow").setBodyDelay(300, TimeUnit.MILLISECONDS));
        HttpClient.get(base + "/slow", 50);
    }
    @Test(expected = InterruptedIOException.class) public void cancelledThreadDoesNotConnect() throws Exception {
        Thread.currentThread().interrupt();
        HttpClient.get(base + "/ok");
    }
}
