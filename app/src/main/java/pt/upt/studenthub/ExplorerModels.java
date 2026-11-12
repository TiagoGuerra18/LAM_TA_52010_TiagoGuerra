package pt.upt.studenthub;

/** Immutable values shared by the Explorer screens, parser and background tasks. */
public final class ExplorerModels {
    private ExplorerModels() { }

    /** Fixed locations required by variant V1. */
    public enum Place {
        PORTO(41.15, -8.61), LISBON(38.72, -9.14), FARO(37.02, -7.93);
        public final double latitude;
        public final double longitude;

        Place(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        /** Reads an untrusted Intent value without allowing an invalid enum index. */
        public static Place fromIndex(int index) {
            return index >= 0 && index < values().length ? values()[index] : PORTO;
        }
    }

    /** Validated in-memory preferences; temperatures use the API unit selected here. */
    public static final class Settings {
        public final boolean fahrenheit;
        public final int days;
        public final int rainThreshold;
        public final int refreshSeconds;
        public final boolean highlightRain;

        /** Rejects values outside the controls' documented ranges. */
        public Settings(boolean fahrenheit, int days, int rainThreshold,
                        int refreshSeconds, boolean highlightRain) {
            if ((days != 3 && days != 5 && days != 7) || rainThreshold < 0
                    || rainThreshold > 100 || refreshSeconds < 15 || refreshSeconds > 3600) {
                throw new IllegalArgumentException("Invalid Explorer settings");
            }
            this.fahrenheit = fahrenheit;
            this.days = days;
            this.rainThreshold = rainThreshold;
            this.refreshSeconds = refreshSeconds;
            this.highlightRain = highlightRain;
        }

        /** Default: seven days in Celsius, rain above 50%, refresh every minute. */
        public static Settings defaults() {
            return new Settings(false, 7, 50, 60, true);
        }
    }

    /** One daily entry. A null measurement means unavailable, never zero. */
    public static final class DayForecast {
        public final String date;
        public final Double maximum;
        public final Double minimum;
        public final Double rain;
        public final int weatherCode;

        /** Stores the values aligned with one position in daily.time. */
        public DayForecast(String date, Double maximum, Double minimum, Double rain, int weatherCode) {
            this.date = date;
            this.maximum = maximum;
            this.minimum = minimum;
            this.rain = rain;
            this.weatherCode = weatherCode;
        }
    }

    /** One hourly entry; time is local to Europe/Lisbon. */
    public static final class HourForecast {
        public final String time;
        public final Double temperature;
        public final Double rain;
        public final int weatherCode;

        /** Stores one position from the parallel hourly arrays. */
        public HourForecast(String time, Double temperature, Double rain, int weatherCode) {
            this.time = time;
            this.temperature = temperature;
            this.rain = rain;
            this.weatherCode = weatherCode;
        }
    }
}
