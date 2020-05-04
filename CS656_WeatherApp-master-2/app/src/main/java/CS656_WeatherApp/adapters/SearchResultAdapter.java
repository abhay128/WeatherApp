package CS656_WeatherApp.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import CS656_WeatherApp.R;
import CS656_WeatherApp.models.Weather;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.LocationsViewHolder> {
    private LayoutInflater inflater;
    private ItemClickListener itemClickListener;
    private Context context;
    private ArrayList<Weather> weatherArrayList;

    public SearchResultAdapter(Context context, ArrayList<Weather> weatherArrayList) {
        this.context = context;
        this.weatherArrayList = weatherArrayList;

        inflater = LayoutInflater.from(context);
    }

    @Override
    public LocationsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new LocationsViewHolder(inflater.inflate(R.layout.list_location_row, parent, false));
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onBindViewHolder(LocationsViewHolder holder, int position) {

        Weather weather = weatherArrayList.get(position);

        holder.cityTextView.setText(String.format("%s, %s", weather.getCity(), weather.getCountry()));
        holder.temperatureTextView.setText(weather.getTemperature());
        holder.descriptionTextView.setText(weather.getDescription());
        int wID = Integer.parseInt(weather.getIcon());
        if(wID >= 200 && wID < 300){
            if(wID<=200 || wID >=230){
                holder.iconTextView.setImageResource(R.drawable.thunderrain);
            }else
                holder.iconTextView.setImageResource(R.drawable.thunder2xx);

        }else if(wID >= 300 && wID < 400){
            holder.iconTextView.setImageResource(R.drawable.drizzle);

        }else if(wID >= 500 && wID < 600){
            holder.iconTextView.setImageResource(R.drawable.rain);

        }else if(wID >= 600 && wID < 700){
            if(wID == 602 || wID ==622){
                holder.iconTextView.setImageResource(R.drawable.heavysnow);
            }else{
                holder.iconTextView.setImageResource(R.drawable.snow);
            }
        }else if(wID >= 700 && wID < 800){
            holder.iconTextView.setImageResource(R.drawable.fog7xx);
        }else if(wID >= 800 && wID < 900){
            if(wID==800){
                holder.iconTextView.setImageResource(R.drawable.clear);
            }else{
                holder.iconTextView.setImageResource(R.drawable.cloudy);
            }
        }else
            holder.iconTextView.setImageResource(R.drawable.unknown);

        holder.webView.getSettings().setJavaScriptEnabled(true);
        holder.webView.loadUrl("file:///android_asset/map.html?lat=" + weather.getLat()+ "&lon=" + weather.getLon() + "&zoom=" + 10 + "&appid=notneeded&displayPin=true");

    }

    @Override
    public int getItemCount() {
        return weatherArrayList.size();
    }

    class LocationsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView cityTextView, temperatureTextView, descriptionTextView;
        private ImageView iconTextView;
        private WebView webView;
        private CardView cardView;

        LocationsViewHolder(View itemView) {
            super(itemView);

            cityTextView = itemView.findViewById(R.id.rowCityTextView);
            temperatureTextView = itemView.findViewById(R.id.rowTemperatureTextView);
            descriptionTextView = itemView.findViewById(R.id.rowDescriptionTextView);
            iconTextView = itemView.findViewById(R.id.rowIconTextView);
            webView = itemView.findViewById(R.id.webView2);
            cardView = itemView.findViewById(R.id.rowCardView);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (itemClickListener != null) {
                itemClickListener.onItemClickListener(view, getAdapterPosition());
            }
        }
    }

    public Weather getItem(int position) {
        return weatherArrayList.get(position);
    }

    public void setClickListener(ItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public interface ItemClickListener {
        void onItemClickListener(View view, int position);
    }

}
