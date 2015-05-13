package logic.clustering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public class DistanceGrid<T> {
	private final int cellSize;
	private final int sqCellSize;
	private final Table<Integer, Integer, List<T>> _grid;
	private final HashMap<T, Point> _objectPoint;

	public DistanceGrid(int cellSize, int sqCellSize) {
		this.cellSize = cellSize;
		this.sqCellSize = sqCellSize;
		this._grid = HashBasedTable.create();
		this._objectPoint = new HashMap<T, Point>();
	}

	public void addObject(T location, Point point) {
		int x = this._getCoord(point.x);
		int y = this._getCoord(point.y);
		// int stamp = L.Util.stamp(obj);
		
		if(point == null)
		{
			String test = "";
			test+="";
		}

		this._objectPoint.put(location, point);

		List<T> cellLocations = _grid.get(x, y);
		if (cellLocations == null) {
			cellLocations = new ArrayList<T>();
			_grid.put(x, y, cellLocations);
		}

		cellLocations.add(location);
	};

	// Returns true if the object was found
	public boolean removeObject(T location, Point point) {
		int x = this._getCoord(point.x);
		int y = this._getCoord(point.y);

		this._objectPoint.remove(location);

		List<T> cellLocations = _grid.get(x, y);
		if (cellLocations != null) {
			return cellLocations.remove(location);
		}

		return false;
	}

	public T getNearObject(Point point) {		
		int x = this._getCoord(point.x);
		int y = this._getCoord(point.y);
		double dist;
		double closestDistSq = this.sqCellSize;
		T closest = null;

		for (int i = y - 1; i <= y + 1; i++) {
			for (int j = x - 1; j <= x + 1; j++) {
				List<T> cell = this._grid.get(j, i);
				if (cell != null) {
					for (T obj : cell) {
						Point p = _objectPoint.get(obj);
						if(p == null){
							String test = "";
							test+="";
						}
						dist = this._sqDist(p, point);
						if (dist < closestDistSq) {
							closestDistSq = dist;
							closest = obj;
						}
					}
				}
			}
		}
		return closest;
	}

	private double _sqDist(Point p, Point p2) {
		double dx = p2.x - p.x, dy = p2.y - p.y;
		return dx * dx + dy * dy;
	}

	private int _getCoord(double x) {
		return (int) Math.floor(x / this.cellSize);
	}

}
