package pt.upt.studenthub;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import pt.upt.studenthub.ExplorerModels.DayForecast;
import pt.upt.studenthub.ExplorerModels.Place;
import pt.upt.studenthub.ExplorerModels.Settings;

/** ListView adapter with recycled rows and a ViewHolder, as required by RF2. */
public final class ItemAdapter extends BaseAdapter {
    private List<DayForecast> items = Collections.emptyList();
    private Settings settings = Settings.defaults();
    private Place place = Place.PORTO;
    private final Predicate<String> favorite;

    /** Receives a read-only favorite lookup, never the mutable favorite set. */
    public ItemAdapter(Predicate<String> favorite) { this.favorite = favorite; }

    /** Replaces the immutable snapshot displayed by the list. */
    public void submit(List<DayForecast> items, Place place, Settings settings) {
        this.items = items;
        this.place = place;
        this.settings = settings;
        notifyDataSetChanged();
    }

    /** Number of daily forecasts. */
    @Override public int getCount() { return items.size(); }
    /** Forecast associated with a row. */
    @Override public DayForecast getItem(int position) { return items.get(position); }
    /** Stable ID distinguishes dates and locations. */
    @Override public long getItemId(int position) {
        return place.ordinal() * 100_000_000L + Long.parseLong(getItem(position).date.replace("-", ""));
    }
    /** Stable IDs let ListView preserve its selection when favorites change. */
    @Override public boolean hasStableIds() { return true; }

    /** Binds a recycled row; all measurement text comes from formatted string resources. */
    @Override public View getView(int position, View recycled, ViewGroup parent) {
        View row = recycled;
        Holder holder;
        if (row == null) {
            row = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_forecast, parent, false);
            holder = new Holder(row);
            row.setTag(holder);
        } else holder = (Holder) row.getTag();
        DayForecast item = getItem(position);
        android.content.Context context = parent.getContext();
        holder.date.setText(ExplorerUi.date(item.date));
        holder.temperatures.setText(context.getString(R.string.ex_daily_temperature,
                ExplorerUi.temperature(context, item.maximum, settings),
                ExplorerUi.temperature(context, item.minimum, settings)));
        holder.rain.setText(context.getString(R.string.ex_rain, ExplorerUi.percentage(context, item.rain)));
        holder.icon.setImageResource(ExplorerUi.weatherIcon(item.weatherCode));
        holder.icon.setContentDescription(context.getString(WeatherCodes.describe(item.weatherCode)));
        holder.favorite.setVisibility(favorite.test(ExplorerViewModel.favoriteKey(place, item.date))
                ? View.VISIBLE : View.GONE);
        return row;
    }

    private static final class Holder {
        final TextView date;
        final TextView temperatures;
        final TextView rain;
        final ImageView icon;
        final ImageView favorite;
        Holder(View row) {
            date = row.findViewById(R.id.dayDate);
            temperatures = row.findViewById(R.id.dayTemperatures);
            rain = row.findViewById(R.id.dayRain);
            icon = row.findViewById(R.id.dayIcon);
            favorite = row.findViewById(R.id.dayFavorite);
        }
    }
}
