package logic.clustering;

import java.util.List;

public class MarkerClusterGroup {
	// constants that are configurable
	private static final int MAXZOOM = 16;
	// predefined leaflet constants
	private static final int RADIUS = 80;
	private static final int SQRADIUS = RADIUS * RADIUS;
	
	// TODO check
	private int zoom = 2;
	
	private final MarkerClusterDistanceGrid[] _gridClusters = new MarkerClusterDistanceGrid[MAXZOOM + 1];
	private final MarkerDistanceGrid[] _gridUnclustered = new MarkerDistanceGrid[MAXZOOM + 1];
	private final MarkerCluster topClusterLevel;

	public MarkerClusterGroup() {
		this.topClusterLevel = new MarkerCluster(-1);
		
		// *********************************************************************
		// taken from javascript-function: _generateInitialClusters
		//Set up DistanceGrids for each zoom
        for (int zoom = MAXZOOM; zoom >= 0; zoom--) {
            this._gridClusters[zoom] = new MarkerClusterDistanceGrid(RADIUS, SQRADIUS);
            this._gridUnclustered[zoom] = new MarkerDistanceGrid(RADIUS, SQRADIUS);
        }
        
	}
	
/*	public MarkerCluster calculateTopClusterLevel(Iterable<ILocation> locations) {
		// *********************************************************************
        // taken from javascript-function: addLayers 
		
		
        for(ILocation location : locations)
        {
        	Marker m = new Marker(location);
            this.addLayer(m, MAXZOOM);

            //TODO check if this is necessary
//            //If we just made a cluster of size 2 then we need to remove the other marker from the map (if it is) or we never will
//            ClusteredLocation parent = m.getParent();
//            if (parent != null) {
//                if (parent.getChildCount() == 2) {
//                    var markers = parent.getAllChildMarkers(),
//                    		otherMarker = markers[0] === m ? markers[1] : markers[0];
//                    fg.removeLayer(otherMarker);
//                }
//            }
        }


        //this.topClusterLevel._recursivelyAddChildrenToMap(null, this.zoom, this._currentShownBounds);
		
		return topClusterLevel;
	}*/

	private void addLayer(Marker layer, int zoom) {
		for (; zoom >= 0; zoom--) {
			Point markerPoint = Map.project(layer, zoom);
			
			//Try find a cluster close by
			MarkerCluster closestCluster = _gridClusters[zoom].getNearObject(markerPoint);
            if (closestCluster != null) {
            	closestCluster.addChild(layer);
                layer.setParent(closestCluster);
                return;
            }
            
          //Try find a marker close by to form a new cluster with
            Marker closestMarker = this._gridUnclustered[zoom].getNearObject(markerPoint);
            if (closestMarker != null) {
            	MarkerCluster parent = closestMarker.getParent();
                if (parent != null) {
                    this._removeLayer(closestMarker, false);
                }

                //Create new cluster with these 2 in it

                MarkerCluster newCluster = new MarkerCluster(zoom, closestMarker, layer);
                this._gridClusters[zoom].addObject(newCluster, Map.project(newCluster.getCLatLng(), zoom));
                closestMarker.setParent(newCluster);
                layer.setParent(newCluster);

                //First create any new intermediate parent clusters that don't exist
                MarkerCluster lastParent = newCluster;
                for (int z = zoom - 1; z > parent.getZoom(); z--) {
                    lastParent = new MarkerCluster(z, lastParent);
                    _gridClusters[z].addObject(lastParent, Map.project(closestMarker, z));
                }
                parent.addChild(lastParent);

                //Remove closest from this zoom level and any above that it is in, replace with newCluster
                for (int z = zoom; z >= 0; z--) {
                    if (!_gridUnclustered[z].removeObject(closestMarker, Map.project(closestMarker, z))) {
                        break;
                    }
                }

                return;
            }

            //Didn't manage to cluster in at this zoom, record us as a marker here and continue upwards
            _gridUnclustered[zoom].addObject(layer, markerPoint);
		}
		
		//Didn't get in anything, add us to the top
        this.topClusterLevel.addChild(layer);
        layer.setParent(this.topClusterLevel);
	}

	private void _removeLayer(Marker marker, boolean removeFromDistanceGrid) {
		// fg = this._featureGroup,

            //Remove the marker from distance clusters it might be in
            if (removeFromDistanceGrid) {
                for (int z = MarkerClusterGroup.MAXZOOM; z >= 0; z--) {
                    if (!_gridUnclustered[z].removeObject(marker, Map.project(marker, z))) {
                        break;
                    }
                }
            }

            //Work our way up the clusters removing them as we go if required
            MarkerCluster cluster = marker.getParent();
            List<Marker> markers = cluster.getMarkers();
            Marker otherMarker = null;

            //Remove the marker from the immediate parents marker list
            markers.remove(marker);

            while (cluster != null) {
                cluster.decrementChildCount();
                markers = cluster.getMarkers();
                zoom = cluster.getZoom();

                if (cluster.getZoom() < 0) {
                    //Top level, do nothing
                    break;
                } else if (removeFromDistanceGrid && cluster.getChildCount() <= 1) { //Cluster no longer required
                    //We need to push the other marker up to the parent
                	
                    otherMarker = markers.get(0) == marker ? markers.get(1) : markers.get(0);

                    //Update distance grid
                    _gridClusters[zoom].removeObject(cluster, Map.project(cluster.getCLatLng(), zoom));
                    _gridUnclustered[zoom].addObject(otherMarker, Map.project(otherMarker, zoom));

                    //Move otherMarker up to parent
                    cluster.getParent().getChildClusters().remove(cluster);
                    cluster.getParent().getMarkers().add(otherMarker);
                    otherMarker.setParent(cluster.getParent());

                    /*if (cluster._icon) {
                        //Cluster is currently on the map, need to put the marker on the map instead
                        fg.removeLayer(cluster);
                        if (!dontUpdateMap) {
                            fg.addLayer(otherMarker);
                        }
                    }*/
                } else {
                    /*cluster._recalculateBounds();
                    if (!dontUpdateMap || !cluster._icon) {
                        cluster._updateIcon();
                    }*/
                }

                cluster = cluster.getParent();
            }

            marker.setParent(null);
	}

	public void addLocation(ILocation location) {
		Marker m = new Marker(location);
        this.addLayer(m, MAXZOOM);
	}

	public MarkerCluster getTopLevelCluster() {
		return this.topClusterLevel;
	}

}
