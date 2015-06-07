package logic.clustering.serialization;

import com.fasterxml.jackson.annotation.JsonProperty;

import logic.clustering.Marker;

public class LightMarkerVO {

	private final Marker realMarker;

	public LightMarkerVO(Marker marker) {
		this.realMarker = marker;
	}
	
	@JsonProperty(value="_latlng")
	public LightMarkerLocationVO getLatLng()
	{
		return new LightMarkerLocationVO(realMarker);
	}

}
