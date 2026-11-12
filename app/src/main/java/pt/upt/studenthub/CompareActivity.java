package pt.upt.studenthub;

import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import pt.upt.studenthub.ExplorerModels.Comparison;
import pt.upt.studenthub.ExplorerModels.DayForecast;
import pt.upt.studenthub.ExplorerModels.Place;
import pt.upt.studenthub.ExplorerState.Status;

/** Displays Porto, Lisbon and Faro side by side, including partial failures. */
public class CompareActivity extends ExplorerScreen {
    public static final String DEBUG_FAIL_FARO = "explorer.debug_fail_faro";
    private ExplorerViewModel model;
    private final Map<Place, View> cards = new EnumMap<>(Place.class);
    private boolean failFaro;

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.activity_compare);
        failFaro = (getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
                && getIntent().getBooleanExtra(DEBUG_FAIL_FARO, false);
        findViewById(R.id.compareDebugWarning).setVisibility(failFaro ? View.VISIBLE : View.GONE);
        model = new ViewModelProvider(this).get(ExplorerViewModel.class);
        if (model.comparison().getValue().status == Status.IDLE)
            model.configure(ExplorerContract.place(getIntent().getExtras()), ExplorerContract.settings(getIntent().getExtras()));
        LinearLayout container = findViewById(R.id.comparisonColumns);
        for (Place place : Place.values()) {
            View card = LayoutInflater.from(this).inflate(R.layout.item_comparison, container, false);
            ((TextView) card.findViewById(R.id.comparePlace)).setText(ExplorerUi.place(this, place));
            container.addView(card);
            cards.put(place, card);
        }
        findViewById(R.id.compareRefresh).setOnClickListener(v -> model.loadComparison(isOnline(), failFaro));
        findViewById(R.id.compareCancel).setOnClickListener(v -> model.cancelComparison());
        model.comparison().observe(this, this::render);
        if (model.comparison().getValue().status == Status.IDLE) model.loadComparison(isOnline(), failFaro);
    }

    private boolean isOnline() {
        ConnectivityManager manager = ContextCompat.getSystemService(this, ConnectivityManager.class);
        if (manager == null) return false;
        NetworkCapabilities caps = manager.getNetworkCapabilities(manager.getActiveNetwork());
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void render(ExplorerState<List<Comparison>> state) {
        boolean loading = state.status == Status.LOADING;
        findViewById(R.id.compareProgress).setVisibility(loading ? View.VISIBLE : View.GONE);
        findViewById(R.id.compareRefresh).setEnabled(!loading);
        findViewById(R.id.compareCancel).setEnabled(loading);
        ((TextView) findViewById(R.id.compareStatus)).setText(ExplorerUi.state(this, state));
        for (Place place : Place.values()) {
            View card = cards.get(place);
            ((TextView) card.findViewById(R.id.compareMessage)).setText(ExplorerUi.state(this, state));
            ((TextView) card.findViewById(R.id.compareValues)).setText("");
        }
        for (Comparison item : state.data) {
            View card = cards.get(item.place);
            ((TextView) card.findViewById(R.id.compareMessage)).setText(ExplorerUi.state(this, item.result));
            DayForecast day = item.result.data;
            if (day != null) ((TextView) card.findViewById(R.id.compareValues)).setText(getString(
                    R.string.ex_compare_values, ExplorerUi.temperature(this, day.maximum, model.settings()),
                    ExplorerUi.temperature(this, day.minimum, model.settings()), ExplorerUi.percentage(this, day.rain)));
        }
    }
}
