package pt.upt.studenthub;

import androidx.annotation.StringRes;

/** Maps WMO weather codes to localized string resources. */
public final class WeatherCodes {
  // Impede a criação de instâncias desta classe.
  private WeatherCodes() {}

  @StringRes
  // Converte o código WMO num recurso de texto.
  /** Returns the description resource for a WMO code. */
  public static int describe(int code) {
    if (code == 0) return R.string.wmo_clear;
    if (code == 1) return R.string.wmo_mainly_clear;
    if (code == 2) return R.string.wmo_partly_cloudy;
    if (code == 3) return R.string.wmo_overcast;
    if (code == 45 || code == 48) return R.string.wmo_fog;
    if (code >= 51 && code <= 57) return R.string.wmo_drizzle;
    if (code >= 61 && code <= 67) return R.string.wmo_rain;
    if (code >= 71 && code <= 77) return R.string.wmo_snow;
    if (code >= 80 && code <= 82) return R.string.wmo_showers;
    if (code == 85 || code == 86) return R.string.wmo_snow_showers;
    if (code >= 95 && code <= 99) return R.string.wmo_thunderstorm;
    return R.string.wmo_unknown;
  }
}
