package CS656_WeatherApp.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import CS656_WeatherApp.DefaultCity;
import CS656_WeatherApp.R;
import CS656_WeatherApp.adapters.ViewPagerAdapter;
import CS656_WeatherApp.adapters.LongtermHourlyAdapter;
import CS656_WeatherApp.fragments.AboutFragment;
import CS656_WeatherApp.fragments.SearchResultFragment;
import CS656_WeatherApp.fragments.HourlyFragment;
import CS656_WeatherApp.models.Weather;
import CS656_WeatherApp.tasks.APIRequest;
import CS656_WeatherApp.tasks.ParseResult;
import CS656_WeatherApp.tasks.TaskOutput;
import CS656_WeatherApp.utils.Convertor;
import CS656_WeatherApp.SavedVariable;

public class MainActivity extends AppCompatActivity implements LocationListener {
    protected static final int MY_PERMISSIONS_ACCESS_FINE_LOCATION = 1;
    private static final int NO_UPDATE_REQUIRED_THRESHOLD = 900000; // in ms = 15 minutes
    private Weather currentWeather = new Weather();
    private TextView currentTempView, currentDescriptionView, currentWindView, currentPressureView, currentHumidityView,
            todaySunriseView, todaySunsetView, currentUvIndexView, lastUpdateView;
    private ImageView currentIconView;
    private ViewPager pageView;
    private TabLayout tabLayout;
    private SwipeRefreshLayout swipeRefreshLayout;
    protected int theme;
    private View appView;
    private LocationManager locationManager;
    private ProgressDialog progressDialog;
    private boolean destroyed = false;
    private boolean firstRun;

    private List<Weather> laterWeather = new ArrayList<>();
    private List<Weather> todayWeather = new ArrayList<>();
    private List<Weather> tomorrowWeather = new ArrayList<>();

    public String recentCityId = "";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    SavedVariable SavedVariable = CS656_WeatherApp.SavedVariable.getInstance();
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        PreferenceManager.setDefaultValues(this, R.xml.prefs, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        editor = prefs.edit();
        firstRun = prefs.getBoolean("firstRun", true);

        setTheme(theme = R.style.AppTheme);

        // Initiate activity
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);
        appView = findViewById(R.id.viewApp);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        progressDialog = new ProgressDialog(MainActivity.this);

        // Load toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize textboxes
        currentTempView = (TextView) findViewById(R.id.currentTemperature);
        currentDescriptionView = (TextView) findViewById(R.id.currentDescription);
        currentWindView = (TextView) findViewById(R.id.currentWind);
        currentPressureView = (TextView) findViewById(R.id.currentPressure);
        currentHumidityView = (TextView) findViewById(R.id.currentHumidity);
        todaySunriseView = (TextView) findViewById(R.id.todaySunrise);
        todaySunsetView = (TextView) findViewById(R.id.todaySunset);
        currentUvIndexView = (TextView) findViewById(R.id.currentUvIndex);
        lastUpdateView = (TextView) findViewById(R.id.lastUpdate);
        currentIconView = (ImageView) findViewById(R.id.currentIcon);


        // Initialize viewPager
        pageView = (ViewPager) findViewById(R.id.viewPager);
        tabLayout = (TabLayout) findViewById(R.id.tabs);

        destroyed = false;

        // Preload data from cache
        preloadWeather();
        preloadUVIndex();
        updateLastUpdateTime();


        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshWeather();
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                // Only allow pull to refresh when scrolled to top
                swipeRefreshLayout.setEnabled(verticalOffset == 0);
            }
        });

        Bundle bundle = getIntent().getExtras();
        if (bundle != null && bundle.getBoolean("shouldRefresh")) {
            refreshWeather();
        }

        if (  SavedVariable.getData() != "0")
        {
            getCityByLocation();
        }
    }



    @Override
    public void onStart() {
        super.onStart();
        updateCurrentWeatherUI();
        updateLongTermWeatherUI();
        updateUVIndexUI();
    }




    private void preloadUVIndex() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

        String lastUVIToday = sp.getString("lastToday", "");
        if (!lastUVIToday.isEmpty()) {
            double latitude = currentWeather.getLat();
            double longitude = currentWeather.getLon();
            if (latitude == 0 && longitude == 0) {
                return;
            }
            new TodayUVITask(this, this, progressDialog).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "coords", Double.toString(latitude), Double.toString(longitude));
        }
    }


    private void preloadWeather() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

        String lastToday = sp.getString("lastToday", "");
        if (!lastToday.isEmpty()) {
            new currentWeatherTask(this, this, progressDialog).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "cachedResponse", lastToday);
        }
        String lastLongterm = sp.getString("lastLongterm", "");
        if (!lastLongterm.isEmpty()) {
            new LongTermWeatherTask(this, this, progressDialog).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "cachedResponse", lastLongterm);
        }
    }

    private void getTodayUVIndex() {
        double latitude = currentWeather.getLat();
        double longitude = currentWeather.getLon();
        new TodayUVITask(this, this, progressDialog).execute("coords", Double.toString(latitude), Double.toString(longitude));
    }

    private void getTodayWeather() {
        new currentWeatherTask(this, this, progressDialog).execute();
    }

    private void getLongTermWeather() {
        new LongTermWeatherTask(this, this, progressDialog).execute();
    }

    public LongtermHourlyAdapter getAdapter(int id) {
        LongtermHourlyAdapter weatherRecyclerAdapter;
        if (id == 0) {
            weatherRecyclerAdapter = new LongtermHourlyAdapter(this, todayWeather);
        } else if (id == 1) {
            weatherRecyclerAdapter = new LongtermHourlyAdapter(this, tomorrowWeather);
        } else {
            weatherRecyclerAdapter = new LongtermHourlyAdapter(this, laterWeather);
        }
        return weatherRecyclerAdapter;
    }

    private void searchCities() {
        SavedVariable.setData("0");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setMaxLines(1);
        input.setSingleLine(true);

        TextInputLayout inputLayout = new TextInputLayout(this);
        inputLayout.setPadding(32, 0, 32, 0);
        inputLayout.addView(input);

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(this.getString(R.string.search_title));
        alert.setView(inputLayout);

        alert.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String result = input.getText().toString().trim();
                if (!result.isEmpty()) {
                    new FindCitiesByNameTask(getApplicationContext(),
                            MainActivity.this, progressDialog).execute("city", result);
                }
            }
        });
        alert.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Cancelled
            }
        });
        alert.show();
    }

    private void saveLocation(String result) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        recentCityId = preferences.getString("cityId", DefaultCity.DEFAULT_CITY_ID);

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("cityId", result);

        editor.commit();
    }


    private void aboutDialog() {
        new AboutFragment().show(getSupportFragmentManager(), null);
    }


    public static String getRainString(JSONObject rainObj) {
        String rain = "0";
        if (rainObj != null) {
            rain = rainObj.optString("3h", "fail");
            if ("fail".equals(rain)) {
                rain = rainObj.optString("1h", "0");
            }
        }
        return rain;
    }


    private ParseResult parseCurrentJson(String result) {
        try {
            JSONObject reader = new JSONObject(result);

            final String code = reader.optString("cod");
            if ("404".equals(code)) {
                return ParseResult.CITY_NOT_FOUND;
            }

            String city = reader.getString("name");
            String country = "";
            JSONObject countryObj = reader.optJSONObject("sys");
            if (countryObj != null) {
                country = countryObj.getString("country");
                currentWeather.setSunrise(countryObj.getString("sunrise"));
                currentWeather.setSunset(countryObj.getString("sunset"));
            }
            currentWeather.setCity(city);
            currentWeather.setCountry(country);

            JSONObject coordinates = reader.getJSONObject("coord");
            if (coordinates != null) {
                currentWeather.setLat(coordinates.getDouble("lat"));
                currentWeather.setLon(coordinates.getDouble("lon"));
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
                sp.edit().putFloat("latitude", (float) currentWeather.getLat()).putFloat("longitude", (float) currentWeather.getLon()).commit();
            }

            JSONObject main = reader.getJSONObject("main");

            currentWeather.setTemperature(main.getString("temp"));
            currentWeather.setDescription(reader.getJSONArray("weather").getJSONObject(0).getString("description"));
            JSONObject windObj = reader.getJSONObject("wind");
            currentWeather.setWind(windObj.getString("speed"));
            currentWeather.setPressure(main.getString("pressure"));
            currentWeather.setHumidity(main.getString("humidity"));

            JSONObject rainObj = reader.optJSONObject("rain");
            String rain;
            if (rainObj != null) {
                rain = getRainString(rainObj);
            } else {
                JSONObject snowObj = reader.optJSONObject("snow");
                if (snowObj != null) {
                    rain = getRainString(snowObj);
                } else {
                    rain = "0";
                }
            }
            currentWeather.setRain(rain);
            currentWeather.setId(reader.getJSONArray("weather").getJSONObject(0).getString("id"));

            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
            editor.putString("lastToday", result);
            editor.commit();

        } catch (JSONException e) {
            Log.e("JSONException Data", result);
            e.printStackTrace();
            return ParseResult.JSON_EXCEPTION;
        }

        return ParseResult.OK;
    }

    private ParseResult parseCurrentUVIJson(String result) {
        try {
            JSONObject reader = new JSONObject(result);

            final String code = reader.optString("cod");
            if ("404".equals(code)) {
                currentWeather.setUvIndex(-1);
                return ParseResult.CITY_NOT_FOUND;
            }

            double value = reader.getDouble("value");
            currentWeather.setUvIndex(value);
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
            editor.putString("lastUVIToday", result);
            editor.commit();
        } catch (JSONException e) {
            Log.e("JSONException Data", result);
            e.printStackTrace();
            return ParseResult.JSON_EXCEPTION;
        }

        return ParseResult.OK;
    }

    private void updateCurrentWeatherUI() {
        try {
            if (currentWeather.getCountry().isEmpty()) {
                preloadWeather();
                return;
            }
        } catch (Exception e) {
            preloadWeather();
            return;
        }
        String city = currentWeather.getCity();
        String country = currentWeather.getCountry();
        DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(getApplicationContext());
        getSupportActionBar().setTitle(city + (country.isEmpty() ? "" : ", " + country));

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

        // Temperature
        float temperature = Convertor.convertTemperature(Float.parseFloat(currentWeather.getTemperature()), sp);

        // Rain
        double rain = Double.parseDouble(currentWeather.getRain());
        String rainString = Convertor.getRainString(rain);

        // Wind
        double wind;
        try {
            wind = Double.parseDouble(currentWeather.getWind());
        } catch (Exception e) {
            e.printStackTrace();
            wind = 0;
        }
        wind = Convertor.convertWind(wind);

        // Pressure
        double pressure = Convertor.convertPressure((float) Double.parseDouble(currentWeather.getPressure()));

        currentTempView.setText(new DecimalFormat("0").format(temperature) + " " + sp.getString("unit", "°F"));
        currentDescriptionView.setText(currentWeather.getDescription().substring(0, 1).toUpperCase() +
                currentWeather.getDescription().substring(1) + rainString);
        currentWindView.setText(getString(R.string.wind) + ": " + new DecimalFormat("0.0").format(wind) + " mph");
        currentPressureView.setText(getString(R.string.pressure) + ": " + new DecimalFormat("0.##").format(pressure) + " inHg");
        currentHumidityView.setText(getString(R.string.humidity) + ": " + currentWeather.getHumidity() + " %");
        todaySunriseView.setText(getString(R.string.sunrise) + ": " + timeFormat.format(currentWeather.getSunrise()));
        todaySunsetView.setText(getString(R.string.sunset) + ": " + timeFormat.format(currentWeather.getSunset()));


        //show weather icon based on weather condition code
        int wID = Integer.parseInt(currentWeather.getId());
        if(wID >= 200 && wID < 300){
            if(wID<=200 || wID >=230){
                currentIconView.setImageResource(R.drawable.thunderrain);
            }else
                currentIconView.setImageResource(R.drawable.thunder2xx);

        }else if(wID >= 300 && wID < 400){
            currentIconView.setImageResource(R.drawable.drizzle);

        }else if(wID >= 500 && wID < 600){
            currentIconView.setImageResource(R.drawable.rain);

        }else if(wID >= 600 && wID < 700){
            if(wID == 602 || wID ==622){
                currentIconView.setImageResource(R.drawable.heavysnow);
            }else{
                currentIconView.setImageResource(R.drawable.snow);
            }
        }else if(wID >= 700 && wID < 800){
            currentIconView.setImageResource(R.drawable.fog7xx);
        }else if(wID >= 800 && wID < 900){
            if(wID==800){
                currentIconView.setImageResource(R.drawable.clear);
            }else{
                currentIconView.setImageResource(R.drawable.cloudy);
            }
        }else
            currentIconView.setImageResource(R.drawable.unknown);


    }

    private void updateUVIndexUI() {
        try {
            if (currentWeather.getCountry().isEmpty()) {
                return;
            }
        } catch (Exception e) {
            preloadUVIndex();
            return;
        }

        // UV Index
        double uvIndex = currentWeather.getUvIndex();
        currentUvIndexView.setText(getString(R.string.uvindex) + ": " + Convertor.convertUvIndexToRiskLevel(uvIndex, this));
    }

    public ParseResult parseLongTermJson(String result) {
        int i;
        try {
            JSONObject reader = new JSONObject(result);

            final String code = reader.optString("cod");
            if ("404".equals(code)) {
                if (laterWeather == null) {
                    laterWeather = new ArrayList<>();
                    todayWeather = new ArrayList<>();
                    tomorrowWeather = new ArrayList<>();
                }
                return ParseResult.CITY_NOT_FOUND;
            }

            laterWeather = new ArrayList<>();
            todayWeather = new ArrayList<>();
            tomorrowWeather = new ArrayList<>();

            JSONArray list = reader.getJSONArray("list");
            for (i = 0; i < list.length(); i++) {
                Weather weather = new Weather();

                JSONObject listItem = list.getJSONObject(i);
                JSONObject main = listItem.getJSONObject("main");

                weather.setDate(listItem.getString("dt"));
                weather.setTemperature(main.getString("temp"));
                weather.setDescription(listItem.optJSONArray("weather").getJSONObject(0).getString("description"));
                JSONObject windObj = listItem.optJSONObject("wind");
                if (windObj != null) {
                    weather.setWind(windObj.getString("speed"));
                }
                weather.setPressure(main.getString("pressure"));
                weather.setHumidity(main.getString("humidity"));

                JSONObject rainObj = listItem.optJSONObject("rain");
                String rain = "";
                if (rainObj != null) {
                    rain = getRainString(rainObj);
                } else {
                    JSONObject snowObj = listItem.optJSONObject("snow");
                    if (snowObj != null) {
                        rain = getRainString(snowObj);
                    } else {
                        rain = "0";
                    }
                }
                weather.setRain(rain);

                final String idString = listItem.optJSONArray("weather").getJSONObject(0).getString("id");
                weather.setId(idString);

                final String dateMsString = listItem.getString("dt") + "000";
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(Long.parseLong(dateMsString));


                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MILLISECOND, 0);

                Calendar tomorrow = (Calendar) today.clone();
                tomorrow.add(Calendar.DAY_OF_YEAR, 1);

                Calendar later = (Calendar) today.clone();
                later.add(Calendar.DAY_OF_YEAR, 2);

                if (cal.before(tomorrow)) {
                    todayWeather.add(weather);
                } else if (cal.before(later)) {
                    tomorrowWeather.add(weather);
                } else {
                    laterWeather.add(weather);
                }
            }
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
            editor.putString("lastLongterm", result);
            editor.commit();
        } catch (JSONException e) {
            Log.e("JSONException Data", result);
            e.printStackTrace();
            return ParseResult.JSON_EXCEPTION;
        }

        return ParseResult.OK;
    }

    private void updateLongTermWeatherUI() {
        if (destroyed) {
            return;
        }

        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());

        Bundle bundleToday = new Bundle();
        bundleToday.putInt("day", 0);
        HourlyFragment recyclerViewFragmentToday = new HourlyFragment();
        recyclerViewFragmentToday.setArguments(bundleToday);
        viewPagerAdapter.addFragment(recyclerViewFragmentToday, getString(R.string.today));

        Bundle bundleTomorrow = new Bundle();
        bundleTomorrow.putInt("day", 1);
        HourlyFragment recyclerViewFragmentTomorrow = new HourlyFragment();
        recyclerViewFragmentTomorrow.setArguments(bundleTomorrow);
        viewPagerAdapter.addFragment(recyclerViewFragmentTomorrow, getString(R.string.tomorrow));

        Bundle bundle = new Bundle();
        bundle.putInt("day", 2);
        HourlyFragment recyclerViewFragment = new HourlyFragment();
        recyclerViewFragment.setArguments(bundle);
        viewPagerAdapter.addFragment(recyclerViewFragment, "Later");
        int currentPage = pageView.getCurrentItem();

        viewPagerAdapter.notifyDataSetChanged();
        pageView.setAdapter(viewPagerAdapter);
        tabLayout.setupWithViewPager(pageView);

        if (currentPage == 0 && todayWeather.isEmpty()) {
            currentPage = 1;
        }
        pageView.setCurrentItem(currentPage, false);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private boolean shouldUpdate() {
        long lastUpdate = PreferenceManager.getDefaultSharedPreferences(this).getLong("lastUpdate", -1);
        boolean cityChanged = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("cityChanged", false);
        // Update if never checked or last update is longer ago than specified threshold
        return cityChanged || lastUpdate < 0 || (Calendar.getInstance().getTimeInMillis() - lastUpdate) > NO_UPDATE_REQUIRED_THRESHOLD;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            refreshWeather();
            return true;
        }


        if (id == R.id.action_search) {
            searchCities();
            return true;
        }
        if (id == R.id.action_location) {
            SavedVariable.setData("1");
            getCityByLocation();
            return true;
        }
        if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        }
        if (id == R.id.action_about) {
            aboutDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void refreshWeather() {
        if (isNetworkAvailable()) {
            getTodayWeather();
            getLongTermWeather();
            getTodayUVIndex();
        } else {
            Snackbar.make(appView, getString(R.string.msg_connection_not_available), Snackbar.LENGTH_LONG).show();
        }
    }



    void getCityByLocation() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                showLocationSettingsDialog();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_ACCESS_FINE_LOCATION);
            }

        } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Retrieving your location...");
            progressDialog.setCancelable(false);
            progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    try {
                        locationManager.removeUpdates(MainActivity.this);
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                }
            });
            progressDialog.show();
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
            }
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            }
        } else {
            showLocationSettingsDialog();
        }
    }

    private void showLocationSettingsDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle(R.string.location_settings);
        alertDialog.setMessage(R.string.location_settings_message);
        alertDialog.setPositiveButton(R.string.location_settings_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });
        alertDialog.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alertDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getCityByLocation();
                }
                return;
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        progressDialog.hide();
        try {
            locationManager.removeUpdates(this);
        } catch (SecurityException e) {
            Log.e("LocationManager", "Error while trying to stop listening for location updates. This is probably a permissions issue", e);
        }
        Log.i("LOCATION (" + location.getProvider().toUpperCase() + ")", location.getLatitude() + ", " + location.getLongitude());
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        new ProvideCityNameTask(this, this, progressDialog).execute("coords", Double.toString(latitude), Double.toString(longitude));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }


    class currentWeatherTask extends APIRequest {
        public currentWeatherTask(Context context, MainActivity activity, ProgressDialog progressDialog) {
            super(context, activity, progressDialog);
        }

        @Override
        protected void onPreExecute() {
            loading = 0;
            super.onPreExecute();
        }

        @Override
        protected ParseResult parseResponse(String response) {
            return parseCurrentJson(response);
        }

        @Override
        protected String getAPIName() {
            return "weather";
        }

        @Override
        protected void updateMainUI() {
            updateCurrentWeatherUI();
            updateLastUpdateTime();
            updateUVIndexUI();
        }
    }

    class LongTermWeatherTask extends APIRequest {
        public LongTermWeatherTask(Context context, MainActivity activity, ProgressDialog progressDialog) {
            super(context, activity, progressDialog);
        }

        @Override
        protected ParseResult parseResponse(String response) {
            return parseLongTermJson(response);
        }

        @Override
        protected String getAPIName() {
            return "forecast";
        }

        @Override
        protected void updateMainUI() {
            updateLongTermWeatherUI();
        }
    }

    class FindCitiesByNameTask extends APIRequest {

        public FindCitiesByNameTask(Context context, MainActivity activity, ProgressDialog progressDialog) {
            super(context, activity, progressDialog);
        }

        @Override
        protected void onPreExecute() { /*Nothing*/ }

        @Override
        protected ParseResult parseResponse(String response) {
            try {
                JSONObject reader = new JSONObject(response);

                final String code = reader.optString("cod");
                if ("404".equals(code)) {
                    Log.e("Geolocation", "No city found");
                    return ParseResult.CITY_NOT_FOUND;
                }

//                saveLocation(reader.getString("id"));
                final JSONArray cityList = reader.getJSONArray("list");

                if (cityList.length() > 1) {
                    launchLocationPickerDialog(cityList);
                } else {
                    saveLocation(cityList.getJSONObject(0).getString("id"));
                }

            } catch (JSONException e) {
                Log.e("JSONException Data", response);
                e.printStackTrace();
                return ParseResult.JSON_EXCEPTION;
            }

            return ParseResult.OK;
        }

        @Override
        protected String getAPIName() {
            return "find";
        }

        @Override
        protected void onPostExecute(TaskOutput output) {
            /* Handle possible errors only */
            handleTaskOutput(output);
            refreshWeather();
        }
    }

    //search results
    private void launchLocationPickerDialog(JSONArray cityList) {
        SearchResultFragment fragment = new SearchResultFragment();
        Bundle bundle = new Bundle();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

        bundle.putString("cityList", cityList.toString());
        fragment.setArguments(bundle);

        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        fragmentTransaction.add(android.R.id.content, fragment)
                .addToBackStack(null).commit();
    }

    class ProvideCityNameTask extends APIRequest {

        public ProvideCityNameTask(Context context, MainActivity activity, ProgressDialog progressDialog) {
            super(context, activity, progressDialog);
        }

        @Override
        protected void onPreExecute() { /*Nothing*/ }

        @Override
        protected String getAPIName() {
            return "weather";
        }

        @Override
        protected ParseResult parseResponse(String response) {
            Log.i("RESULT", response.toString());
            try {
                JSONObject reader = new JSONObject(response);

                final String code = reader.optString("cod");
                if ("404".equals(code)) {
                    Log.e("Geolocation", "No city found");
                    return ParseResult.CITY_NOT_FOUND;
                }

                saveLocation(reader.getString("id"));

            } catch (JSONException e) {
                Log.e("JSONException Data", response);
                e.printStackTrace();
                return ParseResult.JSON_EXCEPTION;
            }

            return ParseResult.OK;
        }

        @Override
        protected void onPostExecute(TaskOutput output) {
            /* Handle possible errors only */
            handleTaskOutput(output);

            refreshWeather();
        }
    }

    class TodayUVITask extends APIRequest {
        public TodayUVITask(Context context, MainActivity activity, ProgressDialog progressDialog) {
            super(context, activity, progressDialog);
        }

        @Override
        protected void onPreExecute() {
            loading = 0;
            super.onPreExecute();
        }

        @Override
        protected ParseResult parseResponse(String response) {
            return parseCurrentUVIJson(response);
        }

        @Override
        protected String getAPIName() {
            return "uvi";
        }

        @Override
        protected void updateMainUI() {
            updateUVIndexUI();
        }
    }

    public static long saveLastUpdateTime(SharedPreferences sp) {
        Calendar now = Calendar.getInstance();
        sp.edit().putLong("lastUpdate", now.getTimeInMillis()).commit();
        return now.getTimeInMillis();
    }

    private void updateLastUpdateTime() {
        updateLastUpdateTime(
                PreferenceManager.getDefaultSharedPreferences(this).getLong("lastUpdate", -1)
        );
    }

    private void updateLastUpdateTime(long timeInMillis) {
        if (timeInMillis < 0) {
            // No time
            lastUpdateView.setText("");
        } else {
            lastUpdateView.setText(getString(R.string.last_update, formatTimeWithDayIfNotToday(this, timeInMillis)));
        }
    }

    public static String formatTimeWithDayIfNotToday(Context context, long timeInMillis) {
        Calendar now = Calendar.getInstance();
        Calendar lastCheckedCal = new GregorianCalendar();
        lastCheckedCal.setTimeInMillis(timeInMillis);
        Date lastCheckedDate = new Date(timeInMillis);
        String timeFormat = android.text.format.DateFormat.getTimeFormat(context).format(lastCheckedDate);
        if (now.get(Calendar.YEAR) == lastCheckedCal.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == lastCheckedCal.get(Calendar.DAY_OF_YEAR)) {
            // Same day, only show time
            return timeFormat;
        } else {
            return android.text.format.DateFormat.getDateFormat(context).format(lastCheckedDate) + " " + timeFormat;
        }
    }
}
