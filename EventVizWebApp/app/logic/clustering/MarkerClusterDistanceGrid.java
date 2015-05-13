package logic.clustering;

// class just created because Java is stupid and not able to use an array of instances with generic type
public class MarkerClusterDistanceGrid extends DistanceGrid<MarkerCluster> {

	public MarkerClusterDistanceGrid(int cellSize, int sqCellSize) {
		super(cellSize, sqCellSize);
	}

}
