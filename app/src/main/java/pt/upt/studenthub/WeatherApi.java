package pt.upt.studenthub;

/** Compatibility facade for the campus module; ExplorerApi owns URL construction. */
public final class WeatherApi {
  public static final double UPT_LATITUDE = 41.18;
  public static final double UPT_LONGITUDE = -8.61;

  // Impede a criação de instâncias desta classe.
  private WeatherApi() {}

  // Devolve o endereço da meteorologia atual.
  public static String currentWeatherUrl() {
    return ExplorerApi.campusWeather(false);
  }

  // Devolve um endereço inválido para testar erros HTTP.
  public static String brokenUrl() {
    return ExplorerApi.campusWeather(true);
  }

}
