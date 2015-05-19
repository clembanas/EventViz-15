package logic.clustering.serialization;

import java.util.ArrayList;
import java.util.List;

import logic.clustering.Marker;
import logic.clustering.MarkerCluster;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
public class LightMarkerClusterVO {
	private final MarkerCluster realMarkerCluster;

	public LightMarkerClusterVO(MarkerCluster realMarkerCluster)
	{
		if(realMarkerCluster == null)
		{
			String test = "";
			test+="";
		}
		
		this.realMarkerCluster = realMarkerCluster;
	}
	
	@JsonProperty(value="_zoom")
	public int getZoom()
	{
		return realMarkerCluster.getZoom();
	}
	
	@JsonProperty(value="_latlng")
	public LightLocationVO getLatLng()
	{		
		return new LightLocationVO(realMarkerCluster);
	}
	
	@JsonProperty(value="_childClusters")
	public List<LightMarkerClusterVO> getChildClusters()
	{
		List<LightMarkerClusterVO> markerClusters = new ArrayList<LightMarkerClusterVO>();
		for(MarkerCluster markerCluster : realMarkerCluster.getChildClusters())
		{
			markerClusters.add(new LightMarkerClusterVO(markerCluster));
		}
		return markerClusters;
	}
	
	@JsonProperty(value="_markers")
	public List<LightMarkerVO> getMarkers()
	{
		List<LightMarkerVO> markers = new ArrayList<LightMarkerVO>();
		
		for(Marker marker : realMarkerCluster.getMarkers())
		{
			markers.add(new LightMarkerVO(marker));
		}
		
		return markers;
	}
	
	@JsonProperty(value="_childCount")
	public int getChildCount()
	{
		return realMarkerCluster.getChildCount();
	}

}
