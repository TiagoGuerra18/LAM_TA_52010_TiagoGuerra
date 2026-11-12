package pt.upt.studenthub;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import android.os.Looper;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import pt.upt.studenthub.ExplorerModels.Place;
import pt.upt.studenthub.ExplorerModels.Settings;
import pt.upt.studenthub.ExplorerState.Status;

/** Verifies request ownership, cancellation races and independence of parallel results. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, manifest = Config.NONE)
public class ExplorerViewModelTest {
    private ExplorerViewModel model;
    @After public void clear() { if (model != null) model.onCleared(); }

    @Test public void offlineNeverStartsTransport() {
        AtomicInteger calls = new AtomicInteger();
        model = new ExplorerViewModel(url -> { calls.incrementAndGet(); return ExplorerParserTest.DAILY; });
        model.loadDays(false);
        assertEquals(Status.ERROR, model.days().getValue().status);
        assertEquals(ExplorerState.Error.NO_CONNECTION, model.days().getValue().error);
        assertEquals(0, calls.get());
    }
    @Test public void duplicateLoadIsIgnoredAndCancellationIsImmediate() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        model = new ExplorerViewModel(url -> {
            calls.incrementAndGet(); started.countDown(); release.await(2, TimeUnit.SECONDS);
            return ExplorerParserTest.DAILY;
        });
        model.loadDays(true);
        assertTrue(started.await(2, TimeUnit.SECONDS));
        model.loadDays(true);
        model.cancel();
        release.countDown();
        assertEquals(Status.CANCELLED, model.days().getValue().status);
        assertEquals(1, calls.get());
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(Status.CANCELLED, model.days().getValue().status);
    }
    @Test public void cancellationPreservesPreviouslyLoadedData() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        model = new ExplorerViewModel(url -> {
            if (calls.incrementAndGet() > 1) Thread.sleep(1000);
            return ExplorerParserTest.DAILY;
        });
        model.loadDays(true);
        await(() -> model.days().getValue().status == Status.SUCCESS);
        model.loadDays(true);
        model.cancel();
        assertEquals(Status.CANCELLED, model.days().getValue().status);
        assertEquals(1, model.days().getValue().data.size());
    }
    @Test public void lateCancelledResultNeverOverwritesANewerRequest() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        java.util.List<String> successfulDates = new java.util.ArrayList<>();
        model = new ExplorerViewModel(url -> {
            if (calls.getAndIncrement() == 0) {
                started.countDown();
                boolean released = false;
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                while (!released && System.nanoTime() < deadline) {
                    try { released = release.await(2, TimeUnit.SECONDS); }
                    catch (InterruptedException ignored) { /* Simulates a socket finishing after cancellation. */ }
                }
                return ExplorerParserTest.DAILY;
            }
            return ExplorerParserTest.DAILY.replace("2026-11-12", "2026-11-13");
        });
        model.days().observeForever(state -> {
            if (state.status == Status.SUCCESS) successfulDates.add(state.data.get(0).date);
        });
        model.loadDays(true);
        assertTrue(started.await(2, TimeUnit.SECONDS));
        model.cancel();
        model.loadDays(true);
        release.countDown();
        await(() -> model.days().getValue().status == Status.SUCCESS);
        assertEquals(java.util.Collections.singletonList("2026-11-13"), successfulDates);
    }
    @Test public void favoritesDistinguishLocationAndSurviveConfigurationChange() {
        model = new ExplorerViewModel(url -> ExplorerParserTest.DAILY);
        String porto = ExplorerViewModel.favoriteKey(Place.PORTO, "2026-11-12");
        String faro = ExplorerViewModel.favoriteKey(Place.FARO, "2026-11-12");
        model.setFavorite(porto, true);
        model.configure(Place.FARO, Settings.defaults());
        assertTrue(model.isFavorite(porto));
        assertFalse(model.isFavorite(faro));
        model.favorites().clear();
        assertTrue(model.isFavorite(porto));
    }
    @Test public void comparisonRunsConcurrentlyAndPreservesPartialFailure() throws Exception {
        CountDownLatch threeStarted = new CountDownLatch(3);
        model = new ExplorerViewModel(url -> {
            threeStarted.countDown();
            if (!threeStarted.await(2, TimeUnit.SECONDS)) throw new AssertionError("Not parallel");
            if (url.contains("does-not-exist")) throw new HttpClient.HttpStatusException(404);
            return ExplorerParserTest.DAILY;
        });
        model.loadComparison(true, true);
        await(() -> model.comparison().getValue().status == Status.SUCCESS);
        assertEquals(3, model.comparison().getValue().data.size());
        assertEquals(Status.SUCCESS, model.comparison().getValue().data.get(0).result.status);
        assertEquals(Status.SUCCESS, model.comparison().getValue().data.get(1).result.status);
        assertEquals(404, model.comparison().getValue().data.get(2).result.httpCode);
    }

    private void await(java.util.function.BooleanSupplier done) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(4);
        while (!done.getAsBoolean() && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(10);
        }
        assertTrue("Background result did not arrive", done.getAsBoolean());
    }
}
