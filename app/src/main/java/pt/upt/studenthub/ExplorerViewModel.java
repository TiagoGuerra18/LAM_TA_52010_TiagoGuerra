package pt.upt.studenthub;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;
import pt.upt.studenthub.ExplorerModels.Comparison;
import pt.upt.studenthub.ExplorerModels.DayForecast;
import pt.upt.studenthub.ExplorerModels.HourForecast;
import pt.upt.studenthub.ExplorerModels.Place;
import pt.upt.studenthub.ExplorerModels.Settings;
import pt.upt.studenthub.ExplorerState.Status;

/** Owns background requests and in-memory state across configuration changes, without a Context. */
public class ExplorerViewModel extends ViewModel {
    private final ExecutorService listExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService detailExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService comparisonPool = Executors.newFixedThreadPool(3);
    private final ExecutorService comparisonCoordinator = Executors.newSingleThreadExecutor();
    private final MutableLiveData<ExplorerState<List<Comparison>>> comparison =
            new MutableLiveData<>(ExplorerState.of(Status.IDLE, Collections.emptyList()));
    private Future<?> comparisonTask;
    private int comparisonGeneration;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final MutableLiveData<ExplorerState<List<DayForecast>>> days =
            new MutableLiveData<>(ExplorerState.of(Status.IDLE, Collections.emptyList()));
    private final MutableLiveData<ExplorerState<List<HourForecast>>> hours =
            new MutableLiveData<>(ExplorerState.of(Status.IDLE, Collections.emptyList()));
    private final MutableLiveData<Integer> favoriteVersion = new MutableLiveData<>(0);
    private final Set<String> favorites = new HashSet<>();
    private Settings settings = Settings.defaults();
    private Place place = Place.PORTO;
    private String selectedDate;
    private Future<?> listTask;
    private Future<?> detailTask;
    private int listGeneration;
    private int detailGeneration;

    /** Observable daily data and request state, updated only on the main thread. */
    public LiveData<ExplorerState<List<DayForecast>>> days() { return days; }
    /** Observable hourly data for the selected date. */
    public LiveData<ExplorerState<List<HourForecast>>> hours() { return hours; }
    /** Combined snapshot that preserves each location's individual result. */
    public LiveData<ExplorerState<List<Comparison>>> comparison() { return comparison; }
    /** Revision signal for adapters; the mutable favorite set is never exposed. */
    public LiveData<Integer> favoriteVersion() { return favoriteVersion; }
    /** Current validated settings. */
    public Settings settings() { return settings; }
    /** Current location. */
    public Place place() { return place; }
    /** Selected date, retained even when switching orientation. */
    public String selectedDate() { return selectedDate; }
    /** True if either request channel is active. */
    public boolean busy() {
        return days.getValue().status == Status.LOADING || hours.getValue().status == Status.LOADING;
    }

    /** Applies new settings and invalidates values expressed in the previous unit. */
    public void configure(Place place, Settings settings) {
        cancel();
        this.place = place;
        this.settings = settings;
        selectedDate = null;
        days.setValue(ExplorerState.of(Status.IDLE, Collections.emptyList()));
        hours.setValue(ExplorerState.of(Status.IDLE, Collections.emptyList()));
    }

    /** Starts a daily request, or publishes an explicit offline error. */
    public void loadDays(boolean online) { loadDays(online, false); }

    /** Debug overload can force an HTTP error using a URL built exclusively by ExplorerApi. */
    public void loadDays(boolean online, boolean debugHttpError) {
        if (days.getValue().status == Status.LOADING) return;
        final List<DayForecast> previous = days.getValue().data;
        final int generation = ++listGeneration;
        if (!online) { days.setValue(ExplorerState.offline(previous)); return; }
        days.setValue(ExplorerState.of(Status.LOADING, previous));
        final Place requestedPlace = place;
        final Settings requestedSettings = settings;
        listTask = listExecutor.submit(() -> {
            ExplorerState<List<DayForecast>> result;
            try {
                String url = debugHttpError ? ExplorerApi.comparison(requestedPlace, requestedSettings, true)
                        : ExplorerApi.daily(requestedPlace, requestedSettings);
                List<DayForecast> parsed = ExplorerParser.daily(HttpClient.get(url));
                result = ExplorerState.of(parsed.isEmpty() ? Status.EMPTY : Status.SUCCESS, parsed);
            } catch (Exception exception) {
                result = ExplorerState.failed(previous, exception);
            }
            final ExplorerState<List<DayForecast>> snapshot = result;
            // A cancelled worker may still finish a socket read: generation prevents stale delivery.
            main.post(() -> { if (generation == listGeneration) days.setValue(snapshot); });
        });
    }

    /** Selects a day. Rotation observes the existing request instead of starting another one. */
    public void loadHours(String date, boolean online, boolean force) {
        if (date == null || !date.matches("\\d{4}-\\d{2}-\\d{2}")) return;
        if (!force && date.equals(selectedDate) && hours.getValue().status != Status.IDLE) return;
        boolean sameDate = date.equals(selectedDate);
        selectedDate = date;
        if (detailTask != null) detailTask.cancel(true);
        int generation = ++detailGeneration;
        final List<HourForecast> previous = sameDate ? hours.getValue().data : Collections.emptyList();
        if (!online) { hours.setValue(ExplorerState.offline(previous)); return; }
        hours.setValue(ExplorerState.of(Status.LOADING, previous));
        final Place requestedPlace = place;
        final Settings requestedSettings = settings;
        detailTask = detailExecutor.submit(() -> {
            ExplorerState<List<HourForecast>> result;
            try {
                List<HourForecast> parsed = ExplorerParser.hourly(HttpClient.get(
                        ExplorerApi.hourly(requestedPlace, requestedSettings, date)));
                result = ExplorerState.of(parsed.isEmpty() ? Status.EMPTY : Status.SUCCESS, parsed);
            } catch (Exception exception) {
                result = ExplorerState.failed(previous, exception);
            }
            final ExplorerState<List<HourForecast>> snapshot = result;
            main.post(() -> { if (generation == detailGeneration) hours.setValue(snapshot); });
        });
    }

    /** Stable favorite identity includes both location and local date. */
    public static String favoriteKey(Place place, String date) { return place.name() + ":" + date; }
    /** Reads a favorite without exposing the set. */
    public boolean isFavorite(String key) { return favorites.contains(key); }
    /** Updates a favorite and notifies observers. */
    public void setFavorite(String key, boolean favorite) {
        if (favorite) favorites.add(key); else favorites.remove(key);
        favoriteVersion.setValue(favoriteVersion.getValue() + 1);
    }
    /** Defensive snapshot for onSaveInstanceState. */
    public ArrayList<String> favorites() { return new ArrayList<>(favorites); }
    /** Restores only in-memory favorites after Activity recreation. */
    public void restoreFavorites(List<String> saved) {
        if (saved != null) favorites.addAll(saved);
    }

    /** Executes three requests concurrently; invokeAll also cancels children if interrupted. */
    public void loadComparison(boolean online, boolean failFaro) {
        if (comparison.getValue().status == Status.LOADING) return;
        final int generation = ++comparisonGeneration;
        final List<Comparison> previous = comparison.getValue().data;
        if (!online) {
            List<Comparison> offline = new ArrayList<>();
            for (Place location : Place.values()) offline.add(new Comparison(location, ExplorerState.offline(null)));
            comparison.setValue(ExplorerState.of(Status.ERROR, Collections.unmodifiableList(offline)));
            return;
        }
        comparison.setValue(ExplorerState.of(Status.LOADING, previous));
        final Settings requestedSettings = settings;
        comparisonTask = comparisonCoordinator.submit(() -> {
            List<Callable<Comparison>> jobs = new ArrayList<>();
            for (Place location : Place.values()) jobs.add(() -> {
                try {
                    List<DayForecast> values = ExplorerParser.daily(HttpClient.get(
                            ExplorerApi.comparison(location, requestedSettings, failFaro && location == Place.FARO)));
                    return new Comparison(location, ExplorerState.of(values.isEmpty() ? Status.EMPTY : Status.SUCCESS,
                            values.isEmpty() ? null : values.get(0)));
                } catch (Exception exception) {
                    return new Comparison(location, ExplorerState.failed(null, exception));
                }
            });
            try {
                List<Future<Comparison>> futures = comparisonPool.invokeAll(jobs);
                List<Comparison> results = new ArrayList<>();
                for (Future<Comparison> future : futures) results.add(future.get());
                List<Comparison> snapshot = Collections.unmodifiableList(results);
                main.post(() -> {
                    if (generation == comparisonGeneration)
                        comparison.setValue(ExplorerState.of(Status.SUCCESS, snapshot));
                });
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (Exception exception) {
                main.post(() -> {
                    if (generation == comparisonGeneration)
                        comparison.setValue(ExplorerState.failed(previous, exception));
                });
            }
        });
    }

    /** Cancels the comparison coordinator and all still-running invokeAll children. */
    public void cancelComparison() {
        ++comparisonGeneration;
        if (comparisonTask != null) comparisonTask.cancel(true);
        if (comparison.getValue().status == Status.LOADING)
            comparison.setValue(ExplorerState.of(Status.CANCELLED, comparison.getValue().data));
    }

    /** Cancels Futures and immediately publishes CANCELLED while keeping previous data. */
    public void cancel() {
        ++listGeneration;
        ++detailGeneration;
        if (listTask != null) listTask.cancel(true);
        if (detailTask != null) detailTask.cancel(true);
        if (days.getValue().status == Status.LOADING)
            days.setValue(ExplorerState.of(Status.CANCELLED, days.getValue().data));
        if (hours.getValue().status == Status.LOADING)
            hours.setValue(ExplorerState.of(Status.CANCELLED, hours.getValue().data));
    }

    @Override
    protected void onCleared() {
        cancel();
        cancelComparison();
        listExecutor.shutdownNow();
        detailExecutor.shutdownNow();
        comparisonPool.shutdownNow();
        comparisonCoordinator.shutdownNow();
        main.removeCallbacksAndMessages(null);
        super.onCleared();
    }
}
