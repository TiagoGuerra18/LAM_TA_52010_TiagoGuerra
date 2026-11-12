package pt.upt.studenthub;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import android.os.Looper;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import pt.upt.studenthub.ExplorerState.Status;

/** Recreates the actual Explorer screen while its transport is deliberately still in flight. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ExplorerRotationTest {
    private static ExplorerViewModel controlled;
    private static AtomicInteger factoryCalls;

    @Test public void recreationDuringLoadingKeepsTheSameRequestAndViewModel() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger requests = new AtomicInteger();
        factoryCalls = new AtomicInteger();
        controlled = new ExplorerViewModel(url -> {
            requests.incrementAndGet();
            entered.countDown();
            if (!release.await(8, TimeUnit.SECONDS)) throw new IllegalStateException("Test did not release transport");
            return ExplorerParserTest.DAILY;
        });
        try (ActivityController<ControlledExplorerActivity> controller =
                     Robolectric.buildActivity(ControlledExplorerActivity.class).setup()) {
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            assertEquals(Status.LOADING, controlled.days().getValue().status);
            ExplorerViewModel original = controller.get().model;
            controller.recreate();
            assertSame(original, controller.get().model);
            assertEquals(1, factoryCalls.get());
            assertEquals(1, requests.get());
            assertEquals(Status.LOADING, controlled.days().getValue().status);
            release.countDown();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(4);
            while (controlled.days().getValue().status == Status.LOADING && System.nanoTime() < deadline) {
                shadowOf(Looper.getMainLooper()).idle();
                Thread.sleep(10);
            }
            assertEquals(Status.SUCCESS, controlled.days().getValue().status);
            assertEquals(1, ((ListView) controller.get().findViewById(R.id.forecastList)).getCount());
            assertEquals(1, requests.get());
        } finally {
            release.countDown();
            controlled.onCleared();
            controlled = null;
        }
    }

    /** Test-only factory and connectivity override; production uses the normal ViewModelProvider. */
    public static class ControlledExplorerActivity extends ExplorerActivity {
        @Override protected boolean isOnline() { return true; }

        /** Counts factory calls to distinguish actual retention from creating the same fake twice. */
        @NonNull @Override public ViewModelProvider.Factory getDefaultViewModelProviderFactory() {
            return new ViewModelProvider.Factory() {
                @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                    factoryCalls.incrementAndGet();
                    return modelClass.cast(controlled);
                }
            };
        }
    }
}
