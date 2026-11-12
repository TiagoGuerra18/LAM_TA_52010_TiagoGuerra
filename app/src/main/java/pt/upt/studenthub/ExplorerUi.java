package pt.upt.studenthub;

import android.content.Context;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import pt.upt.studenthub.ExplorerModels.Place;
import pt.upt.studenthub.ExplorerModels.Settings;

/** Converts domain values into localized resources; no presentation text belongs in a ViewModel. */
public final class ExplorerUi {
    private ExplorerUi() { }

    /** Localized name of a fixed location. */
    public static String place(Context context, Place place) {
        int id = place == Place.PORTO ? R.string.place_porto
                : place == Place.LISBON ? R.string.place_lisbon : R.string.place_faro;
        return context.getString(id);
    }

    /** Formats the API calendar date without interpreting it in the device's timezone. */
    public static String date(String value) {
        SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        SimpleDateFormat output = new SimpleDateFormat("EEE, dd MMM", new Locale("pt", "PT"));
        input.setTimeZone(TimeZone.getTimeZone("Europe/Lisbon"));
        output.setTimeZone(input.getTimeZone());
        try { return output.format(input.parse(value)); }
        catch (ParseException exception) { return value; }
    }

    /** Temperature with selected unit, or an explicit unavailable marker. */
    public static String temperature(Context context, Double value, Settings settings) {
        return value == null ? context.getString(R.string.ex_unavailable)
                : context.getString(R.string.ex_temperature, value,
                        context.getString(settings.fahrenheit ? R.string.unit_fahrenheit : R.string.unit_celsius));
    }

    /** Percentage, preserving null as unavailable rather than zero. */
    public static String percentage(Context context, Double value) {
        return value == null ? context.getString(R.string.ex_unavailable)
                : context.getString(R.string.ex_percentage, value);
    }

    /** Resource-based status message for any request channel. */
    public static String state(Context context, ExplorerState<?> state) {
        switch (state.status) {
            case LOADING: return context.getString(R.string.status_loading);
            case SUCCESS: return context.getString(R.string.ex_success);
            case EMPTY: return context.getString(R.string.ex_empty);
            case CANCELLED: return context.getString(R.string.ex_cancelled);
            case ERROR:
                switch (state.error) {
                    case HTTP: return context.getString(R.string.err_http, state.httpCode);
                    case TIMEOUT: return context.getString(R.string.err_timeout);
                    case INVALID_DATA: return context.getString(R.string.err_parse);
                    default: return context.getString(R.string.err_no_connection);
                }
            default: return context.getString(R.string.ex_idle);
        }
    }

    /** A vector icon selected by WMO code, including an unknown-data fallback. */
    public static int weatherIcon(int code) {
        if (code == 0 || code == 1) return R.drawable.ic_ex_sun;
        if (code >= 95 && code <= 99) return R.drawable.ic_ex_storm;
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) return R.drawable.ic_ex_snow;
        if (code >= 51 && code <= 82) return R.drawable.ic_ex_rain;
        return R.drawable.ic_ex_cloud;
    }
}
