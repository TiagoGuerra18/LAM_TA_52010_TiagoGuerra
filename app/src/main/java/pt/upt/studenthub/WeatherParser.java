package pt.upt.studenthub;

import org.json.JSONException;
import org.json.JSONObject;

public final class WeatherParser {
  // Impede a criação de instâncias desta classe.
  private WeatherParser() {}

  // Converte o JSON num relatório meteorológico.
  public static WeatherReport parse(String json) throws JSONException {
    JSONObject root = new JSONObject(json);
    JSONObject current = root.getJSONObject("current");
    return new WeatherReport(
        current.getDouble("temperature_2m"),
        current.getInt("relative_humidity_2m"),
        current.getDouble("wind_speed_10m"),
        current.getInt("weather_code"),
        current.getString("time"));
  }
}
