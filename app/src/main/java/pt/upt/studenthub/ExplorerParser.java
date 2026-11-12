package pt.upt.studenthub;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import pt.upt.studenthub.ExplorerModels.DayForecast;
import pt.upt.studenthub.ExplorerModels.HourForecast;

/** Parses Open-Meteo arrays and rejects malformed or misaligned payloads. */
public final class ExplorerParser {
    private ExplorerParser() { }

    /** Parses daily forecasts into an immutable list, preserving null measurements. */
    public static List<DayForecast> daily(String json) throws JSONException {
        JSONObject data = section(json, "daily");
        JSONArray times = data.getJSONArray("time");
        JSONArray max = aligned(data, "temperature_2m_max", times.length());
        JSONArray min = aligned(data, "temperature_2m_min", times.length());
        JSONArray rain = aligned(data, "precipitation_probability_max", times.length());
        JSONArray codes = aligned(data, "weather_code", times.length());
        List<DayForecast> result = new ArrayList<>();
        for (int i = 0; i < times.length(); i++) {
            String date = validTime(times, i, "yyyy-MM-dd");
            result.add(new DayForecast(date, number(max, i), number(min, i),
                    probability(rain, i), code(codes, i)));
        }
        return Collections.unmodifiableList(result);
    }

    /** Parses hourly forecasts, validating timestamp syntax and all parallel arrays. */
    public static List<HourForecast> hourly(String json) throws JSONException {
        JSONObject data = section(json, "hourly");
        JSONArray times = data.getJSONArray("time");
        JSONArray temperatures = aligned(data, "temperature_2m", times.length());
        JSONArray rain = aligned(data, "precipitation_probability", times.length());
        JSONArray codes = aligned(data, "weather_code", times.length());
        List<HourForecast> result = new ArrayList<>();
        for (int i = 0; i < times.length(); i++) {
            result.add(new HourForecast(validTime(times, i, "yyyy-MM-dd'T'HH:mm"),
                    number(temperatures, i), probability(rain, i), code(codes, i)));
        }
        return Collections.unmodifiableList(result);
    }

    private static JSONObject section(String json, String key) throws JSONException {
        JSONObject root = new JSONObject(json);
        if (root.optBoolean("error", false)) throw new JSONException("API error response");
        return root.getJSONObject(key);
    }

    private static JSONArray aligned(JSONObject object, String key, int length) throws JSONException {
        JSONArray values = object.getJSONArray(key);
        if (values.length() != length) throw new JSONException("Misaligned array: " + key);
        return values;
    }

    private static Double number(JSONArray array, int index) throws JSONException {
        if (array.isNull(index)) return null;
        Object raw = array.get(index);
        if (!(raw instanceof Number)) throw new JSONException("Non-numeric measurement");
        double value = ((Number) raw).doubleValue();
        if (Double.isNaN(value) || Double.isInfinite(value)) throw new JSONException("Invalid number");
        return value;
    }

    private static Double probability(JSONArray array, int index) throws JSONException {
        Double value = number(array, index);
        if (value != null && (value < 0 || value > 100)) throw new JSONException("Invalid probability");
        return value;
    }

    private static int code(JSONArray array, int index) throws JSONException {
        Double value = number(array, index);
        if (value == null) return -1;
        if (value != Math.rint(value) || value < 0 || value > 999) throw new JSONException("Invalid WMO code");
        return value.intValue();
    }

    private static String validTime(JSONArray times, int index, String pattern) throws JSONException {
        Object raw = times.get(index);
        if (!(raw instanceof String)) throw new JSONException("Missing timestamp");
        String value = (String) raw;
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.ROOT);
        format.setLenient(false);
        ParsePosition position = new ParsePosition(0);
        if (value.length() != (pattern.contains("HH") ? 16 : 10)
                || format.parse(value, position) == null || position.getIndex() != value.length()) {
            throw new JSONException("Invalid timestamp");
        }
        return value;
    }
}
