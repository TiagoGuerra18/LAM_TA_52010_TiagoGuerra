package pt.upt.studenthub;

import android.net.Uri;
import pt.upt.studenthub.ExplorerModels.Place;
import pt.upt.studenthub.ExplorerModels.Settings;

/** Single URL factory for all Open-Meteo calls; never concatenates query strings. */
public final class ExplorerApi {
    private ExplorerApi() { }

    /** Daily list for the selected location and number of days. */
    public static String daily(Place place, Settings settings) {
        return dailyBuilder(place, settings, false).appendQueryParameter(
                "forecast_days", String.valueOf(settings.days)).build().toString();
    }

    /** Second request: hourly data only for the selected local calendar date. */
    public static String hourly(Place place, Settings settings, String date) {
        return base(place.latitude, place.longitude, false)
                .appendQueryParameter("hourly", "temperature_2m,precipitation_probability,weather_code")
                .appendQueryParameter("temperature_unit", settings.fahrenheit ? "fahrenheit" : "celsius")
                .appendQueryParameter("start_date", date)
                .appendQueryParameter("end_date", date).build().toString();
    }

    /** Today's daily values, optionally using a broken path for a debug failure scenario. */
    public static String comparison(Place place, Settings settings, boolean fail) {
        return dailyBuilder(place, settings, fail)
                .appendQueryParameter("forecast_days", "1").build().toString();
    }

    /** Existing campus module, using the same central URL construction. */
    public static String campusWeather(boolean fail) {
        return base(WeatherApi.UPT_LATITUDE, WeatherApi.UPT_LONGITUDE, fail)
                .appendQueryParameter("current",
                        "temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code")
                .build().toString();
    }

    private static Uri.Builder dailyBuilder(Place place, Settings settings, boolean fail) {
        return base(place.latitude, place.longitude, fail)
                .appendQueryParameter("daily", "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max")
                .appendQueryParameter("temperature_unit", settings.fahrenheit ? "fahrenheit" : "celsius");
    }

    private static Uri.Builder base(double latitude, double longitude, boolean fail) {
        return new Uri.Builder().scheme("https").authority("api.open-meteo.com")
                .appendPath("v1").appendPath(fail ? "forecast-does-not-exist" : "forecast")
                .appendQueryParameter("latitude", String.valueOf(latitude))
                .appendQueryParameter("longitude", String.valueOf(longitude))
                .appendQueryParameter("timezone", "Europe/Lisbon");
    }
}
