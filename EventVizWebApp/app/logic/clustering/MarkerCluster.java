package logic.clustering;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MarkerCluster implements IMarker, Serializable {
	private static final long serialVersionUID = 6837877666870874516L;
	private final List<MarkerCluster> childClusters = new ArrayList<MarkerCluster>();
	private final List<Marker> markers = new ArrayList<Marker>();
	public List<Marker> getMarkers() {
		return markers;
	}

	private final int _zoom;
	private int _childCount = 0;
	private MarkerCluster parent = null;
	private ILocation _cLatLng;
	private Location _wLatLng;
	private Location _latlng;
	
	// just for serialization
	public MarkerCluster()
	{
		// assign invalid zoom
		this._zoom = -2;
	}
	
	public MarkerCluster(int zoom) {
		this._zoom = zoom;
	}

	public MarkerCluster(int zoom,
			Marker closestMarker, Marker layer) {
		this(zoom);
		
		this.addChild(closestMarker);
		this.addChild(layer);
	}

	public MarkerCluster(int zoom, MarkerCluster lastParent){
		this(zoom);
		this.addChild(lastParent);
	}
	
	public int getZoom()
	{
		return this._zoom;
	}

	public MarkerCluster getParent() {
		return this.parent;
	}

	public void setParent(MarkerCluster parent) {
		this.parent = parent;
	}

	public int getChildCount() {
		return this._childCount;
	}

	public void addChild(MarkerCluster child) {
		this.addChild(child, false);
	}
	
	private void addChild(MarkerCluster child, boolean isNotificationFromChild) {
		this._expandBounds(child);
		
		if (!isNotificationFromChild) {
			childClusters.add(child);
            child.setParent(this);
        }
        this._childCount += child._childCount;
        
        if (this.parent != null) {
            this.parent.addChild(child, true);
        }
	}

	public void addChild(Marker child) {
		this.addChild(child, false);		
	}

	private void addChild(Marker child, boolean isNotificationFromChild) {
		this._expandBounds(child);
		
		if (!isNotificationFromChild) {
			markers.add(child);
        }
        this._childCount++;
        
        if (this.parent != null) {
            this.parent.addChild(child, true);
        }
	}
	
	private void _expandBounds(MarkerCluster child) {
		int addedCount = child._childCount;
		ILocation addedLatLng = child._wLatLng != null ? child._wLatLng : child;
		ILocation cLatLng = child._cLatLng;
		expandBounds(addedCount, addedLatLng, cLatLng);
	}

	private void _expandBounds(Marker child) {
		int addedCount = 1;
		ILocation addedLatLng = child;
		ILocation cLatLng = null;
		expandBounds(addedCount, addedLatLng, cLatLng);
	}

	private void expandBounds(int addedCount, ILocation addedLatLng,
			ILocation cLatLng) {
        if (this._cLatLng == null) {
            // when clustering, take position of the first point as the cluster center
            this._cLatLng = cLatLng != null ? cLatLng : addedLatLng;
        }

        // when showing clusters, take weighted average of all points as cluster center
        int totalCount = this._childCount + addedCount;

        //Calculate weighted latlng for display
        if (this._wLatLng == null) {
            this._latlng = this._wLatLng = new Location(addedLatLng.getId(), addedLatLng.getLatitude(), addedLatLng.getLongitude());
        } else {
            this._wLatLng.setLat((addedLatLng.getLatitude() * addedCount + this._wLatLng.getLatitude() * this._childCount) / totalCount);
            this._wLatLng.setLng((addedLatLng.getLongitude() * addedCount + this._wLatLng.getLongitude() * this._childCount) / totalCount);
        }
    }

	public ILocation getCLatLng() {
		return this._cLatLng;
	}

	@Override
	public double getLatitude() {		
		return this._latlng.getLatitude();
	}

	@Override
	public double getLongitude() {
		return this._latlng.getLongitude();
	}

	public void decrementChildCount() {
		this._childCount--;
	}

	public List<MarkerCluster> getChildClusters() {
		return this.childClusters;
	}

	@Override
	public String getId() {
		return null;
	}

	public void merge(MarkerCluster topCluster) {
		this._latlng = topCluster._latlng; 		// just assign anything in order to reduce problems while serializing
		this.childClusters.addAll(topCluster.childClusters);
		this.markers.addAll(topCluster.markers);
		this._childCount += topCluster._childCount;
	}

}
