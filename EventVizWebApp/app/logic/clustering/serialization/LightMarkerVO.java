package logic.clustering.serialization;

import com.fasterxml.jackson.annotation.JsonProperty;

import logic.clustering.Marker;

public class LightMarkerVO {

	private final Marker realMarker;

	public LightMarkerVO(Marker marker) {
		this.realMarker = marker;
	}
	
	@JsonProperty(value="_latlng")
	public LightLocationVO getLatLng()
	{
		if(realMarker.getId() == -1){
			System.out.println("null");
		}
		return new LightLocationVO(realMarker);
	}

}
