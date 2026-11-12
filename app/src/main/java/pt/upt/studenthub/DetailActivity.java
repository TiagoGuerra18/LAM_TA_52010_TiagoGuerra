package pt.upt.studenthub;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import pt.upt.studenthub.ExplorerState.Status;

/** Performs request B, displays hourly forecasts and returns a favorite result to Explorer. */
public class DetailActivity extends ExplorerScreen {
    private ExplorerViewModel model;
    private DetailRenderer renderer;
    private String date;
    private boolean favorite;

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.activity_detail);
        date = getIntent().getStringExtra(ExplorerContract.DATE);
        if (date == null || !date.matches("\\d{4}-\\d{2}-\\d{2}")) { finish(); return; }
        favorite = saved == null ? getIntent().getBooleanExtra(ExplorerContract.FAVORITE, false)
                : saved.getBoolean(ExplorerContract.FAVORITE);
        model = new ViewModelProvider(this).get(ExplorerViewModel.class);
        if (model.hours().getValue().status == Status.IDLE)
            model.configure(ExplorerContract.place(getIntent().getExtras()), ExplorerContract.settings(getIntent().getExtras()));
        renderer = new DetailRenderer(findViewById(R.id.detailPanel),
                () -> model.loadHours(date, isOnline(), true), () -> model.cancel(), this::toggleFavorite);
        model.hours().observe(this, state -> render());
        model.loadHours(date, isOnline(), false);
    }

    private void render() {
        renderer.render(model.hours().getValue(), model.place(), date, model.settings(), favorite);
    }

    private boolean isOnline() {
        ConnectivityManager manager = ContextCompat.getSystemService(this, ConnectivityManager.class);
        if (manager == null) return false;
        NetworkCapabilities caps = manager.getNetworkCapabilities(manager.getActiveNetwork());
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void toggleFavorite() {
        if (favorite) new AlertDialog.Builder(this).setTitle(R.string.ex_remove_title)
                .setMessage(R.string.ex_remove_message).setNegativeButton(R.string.ex_keep, null)
                .setPositiveButton(R.string.ex_remove, (dialog, which) -> returnFavorite(false)).show();
        else returnFavorite(true);
    }

    private void returnFavorite(boolean value) {
        favorite = value;
        Intent result = new Intent().putExtra(ExplorerContract.FAVORITE, value)
                .putExtra(ExplorerContract.FAVORITE_KEY, ExplorerViewModel.favoriteKey(model.place(), date));
        setResult(RESULT_OK, result);
        Toast.makeText(this, value ? R.string.ex_favorite_added : R.string.ex_favorite_removed, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override protected void onSaveInstanceState(@NonNull Bundle state) {
        state.putBoolean(ExplorerContract.FAVORITE, favorite);
        super.onSaveInstanceState(state);
    }
}
