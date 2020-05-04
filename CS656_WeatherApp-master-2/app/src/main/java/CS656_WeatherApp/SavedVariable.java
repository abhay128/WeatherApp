package CS656_WeatherApp;

public class SavedVariable {
    String check_default_city ;
    private static final SavedVariable ourInstance = new SavedVariable();
    public static SavedVariable getInstance() {
        return ourInstance;
    }
    private SavedVariable() {
    }
    public void setData(String check_default_city) {
        this.check_default_city = check_default_city;
    }
    public String getData() {
        return check_default_city;
    }
}
