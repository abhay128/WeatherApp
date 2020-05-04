package CS656_WeatherApp.viewmodels;

import android.arch.lifecycle.ViewModel;
import android.content.SharedPreferences;

import CS656_WeatherApp.DefaultCity;


public class MapViewModel extends ViewModel {
    public SharedPreferences sharedPreferences;
    public String apiKey;
    public double mapLat = Double.valueOf(DefaultCity.DEFAULT_LAT);
    public double mapLon = Double.valueOf(DefaultCity.DEFAULT_LON);
    public int mapZoom = DefaultCity.DEFAULT_ZOOM_LEVEL;
    public int tabPosition = 0;
}
