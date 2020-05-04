package CS656_WeatherApp.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

import CS656_WeatherApp.R;
import CS656_WeatherApp.models.Weather;
import CS656_WeatherApp.models.WeatherViewHolder;
import CS656_WeatherApp.utils.Convertor;

public class LongtermHourlyAdapter extends RecyclerView.Adapter<WeatherViewHolder> {
    private List<Weather> itemList;
    private Context context;

    public LongtermHourlyAdapter(Context context, List<Weather> itemList) {
        this.itemList = itemList;
        this.context = context;
    }

    @Override
    public WeatherViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_row, null);

        WeatherViewHolder viewHolder = new WeatherViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(WeatherViewHolder customViewHolder, int i) {
        Weather weatherItem = itemList.get(i);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        // Temperature
        float temperature = Convertor.convertTemperature(Float.parseFloat(weatherItem.getTemperature()), sp);

        // Rain
        double rain = Double.parseDouble(weatherItem.getRain());
        String rainString = Convertor.getRainString(rain);

        // Wind
        double wind;
        try {
            wind = Double.parseDouble(weatherItem.getWind());
        } catch (Exception e) {
            e.printStackTrace();
            wind = 0;
        }
        wind = Convertor.convertWind(wind);

        // Pressure
        double pressure = Convertor.convertPressure((float) Double.parseDouble(weatherItem.getPressure()));

        TimeZone tz = TimeZone.getDefault();
        String defaultDateFormat = "E dd/MM/yyyy - HH:mm";
        String dateFormat = sp.getString("dateFormat", defaultDateFormat);
        String dateString;
        try {
            SimpleDateFormat resultFormat = new SimpleDateFormat(dateFormat);
            resultFormat.setTimeZone(tz);
            dateString = resultFormat.format(weatherItem.getDate());
        } catch (IllegalArgumentException e) {
            dateString = "ERROR";
        }


        customViewHolder.itemDate.setText(dateString);
        customViewHolder.itemTemperature.setText(new DecimalFormat("0").format(temperature) + " " + sp.getString("unit", "°F"));
        customViewHolder.itemDescription.setText(weatherItem.getDescription().substring(0, 1).toUpperCase() +
                weatherItem.getDescription().substring(1) + rainString);


        int wID = Integer.parseInt(weatherItem.getId());
        if(wID >= 200 && wID < 300){
            if(wID<=200 || wID >=230){
                customViewHolder.itemIcon.setImageResource(R.drawable.thunderrain);
            }else
                customViewHolder.itemIcon.setImageResource(R.drawable.thunder2xx);

        }else if(wID >= 300 && wID < 400){
                customViewHolder.itemIcon.setImageResource(R.drawable.drizzle);

        }else if(wID >= 500 && wID < 600){
                customViewHolder.itemIcon.setImageResource(R.drawable.rain);

        }else if(wID >= 600 && wID < 700){
            if(wID == 602 || wID ==622){
                customViewHolder.itemIcon.setImageResource(R.drawable.heavysnow);
            }else{
                customViewHolder.itemIcon.setImageResource(R.drawable.snow);
            }
        }else if(wID >= 700 && wID < 800){
                customViewHolder.itemIcon.setImageResource(R.drawable.fog7xx);
        }else if(wID >= 800 && wID < 900){
            if(wID==800){
                customViewHolder.itemIcon.setImageResource(R.drawable.clear);
            }else{
                customViewHolder.itemIcon.setImageResource(R.drawable.cloudy);
            }
        }else
                customViewHolder.itemIcon.setImageResource(R.drawable.unknown);

        customViewHolder.itemyWind.setText(context.getString(R.string.wind) + ": " + new DecimalFormat("0.0").format(wind) + " mph");
        customViewHolder.itemPressure.setText(context.getString(R.string.pressure) + ": " + new DecimalFormat("0.##").format(pressure) + " inHg");
        customViewHolder.itemHumidity.setText(context.getString(R.string.humidity) + ": " + weatherItem.getHumidity() + " %");
    }

    @Override
    public int getItemCount() {
        return (null != itemList ? itemList.size() : 0);
    }
}
