package logic.clustering;

//class just created because Java is stupid and not able to use an array of instances with generic type
public class MarkerDistanceGrid extends DistanceGrid<Marker> {

	public MarkerDistanceGrid(int cellSize, int sqCellSize) {
		super(cellSize, sqCellSize);
	}

}
