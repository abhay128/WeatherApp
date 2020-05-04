package CS656_WeatherApp.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

import CS656_WeatherApp.R;

public class Convertor {
    public static float convertTemperature(float temperature, SharedPreferences sp) {
        if (sp.getString("unit", "°C").equals("°C")) {
            return temperature - 273.15f;
        } else {
            return ((9 * (temperature-273.15f)) /5) +32;
        }
    }

    public static float convertRain(float rain) {
        return rain / 25.4f;
    }

    public static String getRainString(double rain) {
        rain = rain / 25.4;
        if (rain < 0.01) {
            return " (<0.01 in)";
        } else {
            return String.format(Locale.ENGLISH, " (%.2f %s)", rain, "in");
        }

    }

    public static float convertPressure(float pressure) {
        return (float) (pressure * 0.0295299830714);
    }

    public static double convertWind(double wind) {

        return wind * 2.23693629205;
    }

    public static String convertUvIndexToRiskLevel(double value, Context context) {
        /* based on: https://en.wikipedia.org/wiki/Ultraviolet_index */
        if (value < 0) {
            return context.getString(R.string.uvi_no_info);
        } else if (value >= 0.0 && value < 3.0) {
            return context.getString(R.string.uvi_low);
        } else if (value >= 3.0 && value < 6.0) {
            return context.getString(R.string.uvi_moderate);
        } else if (value >= 6.0 && value < 8.0) {
            return context.getString(R.string.uvi_high);
        } else if (value >= 8.0 && value < 11.0) {
            return context.getString(R.string.uvi_very_high);
        } else {
            return context.getString(R.string.uvi_extreme);
        }
    }

}
