package pt.upt.studenthub;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import java.util.List;
import pt.upt.studenthub.ExplorerModels.HourForecast;
import pt.upt.studenthub.ExplorerModels.Place;
import pt.upt.studenthub.ExplorerModels.Settings;
import pt.upt.studenthub.ExplorerState.Status;

/** Binds the same detail card in a standalone Activity and in the landscape second panel. */
public final class DetailRenderer {
    private final View root;
    private List<HourForecast> displayed;

    /** Receives callbacks owned by the visible Activity, never by a ViewModel. */
    public DetailRenderer(View root, Runnable retry, Runnable cancel, Runnable favorite) {
        this.root = root;
        root.findViewById(R.id.detailRetry).setOnClickListener(v -> retry.run());
        root.findViewById(R.id.detailCancel).setOnClickListener(v -> cancel.run());
        root.findViewById(R.id.detailFavorite).setOnClickListener(v -> favorite.run());
    }

    /** Displays a request snapshot and highlights hours strictly above the configured threshold. */
    public void render(ExplorerState<List<HourForecast>> state, Place place, String date,
                       Settings settings, boolean favorite) {
        Context context = root.getContext();
        ((TextView) root.findViewById(R.id.detailHeading)).setText(date == null
                ? context.getString(R.string.ex_detail_title)
                : context.getString(R.string.ex_detail_heading, ExplorerUi.place(context, place), ExplorerUi.date(date)));
        ((TextView) root.findViewById(R.id.detailStatus)).setText(date == null
                ? context.getString(R.string.ex_detail_hint) : ExplorerUi.state(context, state));
        boolean loading = state.status == Status.LOADING;
        root.findViewById(R.id.detailProgress).setVisibility(loading ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.detailRetry).setEnabled(date != null && !loading);
        root.findViewById(R.id.detailCancel).setEnabled(loading);
        TextView favoriteButton = root.findViewById(R.id.detailFavorite);
        favoriteButton.setEnabled(date != null);
        favoriteButton.setText(favorite ? R.string.ex_remove_favorite : R.string.ex_add_favorite);
        if (displayed == state.data) return;
        displayed = state.data;
        LinearLayout container = root.findViewById(R.id.hoursContainer);
        container.removeAllViews();
        for (HourForecast hour : state.data) {
            View row = LayoutInflater.from(context).inflate(R.layout.item_hour, container, false);
            ((TextView) row.findViewById(R.id.hourLine)).setText(context.getString(R.string.ex_hour_row,
                    hour.time.substring(11), ExplorerUi.temperature(context, hour.temperature, settings),
                    ExplorerUi.percentage(context, hour.rain)));
            ((TextView) row.findViewById(R.id.hourDescription)).setText(WeatherCodes.describe(hour.weatherCode));
            boolean warning = settings.highlightRain && hour.rain != null && hour.rain > settings.rainThreshold;
            row.findViewById(R.id.hourAlert).setVisibility(warning ? View.VISIBLE : View.GONE);
            if (warning) {
                row.setBackgroundColor(ContextCompat.getColor(context, R.color.ex_warning));
                ((TextView) row.findViewById(R.id.hourLine)).setTextColor(ContextCompat.getColor(context, R.color.ex_warning_text));
                ((TextView) row.findViewById(R.id.hourDescription)).setTextColor(ContextCompat.getColor(context, R.color.ex_warning_text));
            }
            container.addView(row);
        }
        if (state.status == Status.SUCCESS)
            root.startAnimation(AnimationUtils.loadAnimation(context, R.anim.ex_card_enter));
    }
}
