package pt.upt.studenthub;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import java.util.List;
import pt.upt.studenthub.ExplorerModels.DayForecast;
import pt.upt.studenthub.ExplorerModels.Place;
import pt.upt.studenthub.ExplorerState.Status;

/** Daily forecast entry point, retaining request state and scroll position across rotation. */
public class ExplorerActivity extends ExplorerScreen {
    protected ExplorerViewModel model;
    private ItemAdapter adapter;
    private ListView list;
    private Parcelable scrollState;
    private List<DayForecast> displayed;
    private final ActivityResultLauncher<Intent> detailResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String key = result.getData().getStringExtra(ExplorerContract.FAVORITE_KEY);
                    if (key != null) model.setFavorite(key,
                            result.getData().getBooleanExtra(ExplorerContract.FAVORITE, false));
                }
            });

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.activity_explorer);
        model = new ViewModelProvider(this).get(ExplorerViewModel.class);
        if (saved != null && model.days().getValue().status == Status.IDLE) {
            model.configure(ExplorerContract.place(saved), ExplorerContract.settings(saved));
            model.restoreFavorites(saved.getStringArrayList("favorites"));
        }
        if (saved != null) scrollState = saved.getParcelable("scroll");
        String name = getIntent().getStringExtra(TimetableActivity.EXTRA_STUDENT_NAME);
        ((TextView) findViewById(R.id.exGreeting)).setText(getString(R.string.ex_greeting, name == null ? "" : name));
        list = findViewById(R.id.forecastList);
        adapter = new ItemAdapter(model::isFavorite);
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, row, position, id) -> openDay(adapter.getItem(position)));
        findViewById(R.id.exRefresh).setOnClickListener(v -> model.loadDays(isOnline()));
        findViewById(R.id.exCancel).setOnClickListener(v -> model.cancel());
        model.days().observe(this, this::renderDays);
        model.favoriteVersion().observe(this, revision -> adapter.notifyDataSetChanged());
        if (model.days().getValue().status == Status.IDLE) model.loadDays(isOnline());
    }

    protected boolean isOnline() {
        ConnectivityManager manager = ContextCompat.getSystemService(this, ConnectivityManager.class);
        if (manager == null || manager.getActiveNetwork() == null) return false;
        NetworkCapabilities caps = manager.getNetworkCapabilities(manager.getActiveNetwork());
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void openDay(DayForecast day) {
        Bundle extras = new Bundle();
        ExplorerContract.write(extras, model.place(), model.settings());
        extras.putString(ExplorerContract.DATE, day.date);
        extras.putBoolean(ExplorerContract.FAVORITE,
                model.isFavorite(ExplorerViewModel.favoriteKey(model.place(), day.date)));
        detailResult.launch(new Intent(this, DetailActivity.class).putExtras(extras));
    }

    private void renderDays(ExplorerState<List<DayForecast>> state) {
        ((TextView) findViewById(R.id.exLocation)).setText(getString(R.string.ex_location,
                ExplorerUi.place(this, model.place()), model.settings().days));
        ((TextView) findViewById(R.id.exStatus)).setText(ExplorerUi.state(this, state));
        boolean loading = model.busy();
        findViewById(R.id.exProgress).setVisibility(loading ? View.VISIBLE : View.GONE);
        findViewById(R.id.exRefresh).setEnabled(!loading);
        findViewById(R.id.exCancel).setEnabled(loading);
        list.setEnabled(!loading);
        if (displayed != state.data) {
            displayed = state.data;
            adapter.submit(state.data, model.place(), model.settings());
            if (scrollState != null && !state.data.isEmpty()) {
                list.onRestoreInstanceState(scrollState);
                scrollState = null;
            } else if (state.status == Status.SUCCESS) list.scheduleLayoutAnimation();
        }
        invalidateOptionsMenu();
    }

    @Override protected void onSaveInstanceState(@NonNull Bundle saved) {
        saved.putParcelable("scroll", list.onSaveInstanceState());
        saved.putStringArrayList("favorites", model.favorites());
        ExplorerContract.write(saved, model.place(), model.settings());
        super.onSaveInstanceState(saved);
    }

    /** Creates location selection, refresh and About options. */
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_explorer, menu);
        return true;
    }

    /** Commands that initiate requests are disabled during loading. */
    @Override public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.exMenuRefresh).setEnabled(!model.busy());
        menu.findItem(R.id.exMenuLocations).setEnabled(!model.busy());
        int checked = model.place() == Place.PORTO ? R.id.exPorto
                : model.place() == Place.LISBON ? R.id.exLisbon : R.id.exFaro;
        menu.findItem(checked).setChecked(true);
        return super.onPrepareOptionsMenu(menu);
    }

    /** Executes the requested action without performing network work on the main thread. */
    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.exMenuRefresh) { model.loadDays(isOnline()); return true; }
        if (id == R.id.exMenuAbout) { AboutDialog.show(this); return true; }
        if (id == R.id.exPorto || id == R.id.exLisbon || id == R.id.exFaro) {
            Place place = id == R.id.exPorto ? Place.PORTO : id == R.id.exLisbon ? Place.LISBON : Place.FARO;
            model.configure(place, model.settings());
            model.loadDays(isOnline());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
