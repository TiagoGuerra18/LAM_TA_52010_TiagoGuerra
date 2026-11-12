package pt.upt.studenthub;

import android.net.Uri;

public final class WeatherApi {
  public static final double UPT_LATITUDE = 41.18;
  public static final double UPT_LONGITUDE = -8.61;

  // Impede a criação de instâncias desta classe.
  private WeatherApi() {}

  // Devolve o endereço da meteorologia atual.
  public static String currentWeatherUrl() {
    return build("forecast");
  }

  // Devolve um endereço inválido para testar erros HTTP.
  public static String brokenUrl() {
    return build("forecast-does-not-exist");
  }

  // Constrói o URL HTTPS com os parâmetros do campus.
  private static String build(String path) {
    return new Uri.Builder()
        .scheme("https")
        .authority("api.open-meteo.com")
        .appendPath("v1")
        .appendPath(path)
        .appendQueryParameter("latitude", String.valueOf(UPT_LATITUDE))
        .appendQueryParameter("longitude", String.valueOf(UPT_LONGITUDE))
        .appendQueryParameter(
            "current", "temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code")
        .appendQueryParameter("timezone", "Europe/Lisbon")
        .build()
        .toString();
  }
}
