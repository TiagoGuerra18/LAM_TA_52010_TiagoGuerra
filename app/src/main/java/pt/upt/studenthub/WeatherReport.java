package pt.upt.studenthub;

/** Immutable current-weather measurements for the campus module. */
public final class WeatherReport {
  public final double temperature;
  public final int humidity;
  public final double windSpeed;
  public final int weatherCode;
  public final String time;

  // Guarda os dados imutáveis da observação.
  /** Stores one current-weather observation. */
  public WeatherReport(
      double temperature, int humidity, double windSpeed, int weatherCode, String time) {
    this.temperature = temperature;
    this.humidity = humidity;
    this.windSpeed = windSpeed;
    this.weatherCode = weatherCode;
    this.time = time;
  }

  // Extrai a hora da observação.
  /** Returns the local hour portion of the API timestamp. */
  public String hour() {
    int t = time.indexOf('T');
    return t >= 0 ? time.substring(t + 1) : time;
  }
}
