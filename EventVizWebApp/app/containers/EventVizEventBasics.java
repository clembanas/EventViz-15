package containers;

/**
 * Created by clemens on 07.06.15.
 */
public class EventVizEventBasics {
    private String eventful_id;
    private String name;
    private float latitude;
    private float longitude;

    public EventVizEventBasics(String eventful_id, String name, float latitude, float longitude) {
        this.eventful_id = eventful_id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getEventful_id() {
        return eventful_id;
    }

    public void setEventful_id(String eventful_id) {
        this.eventful_id = eventful_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

}
