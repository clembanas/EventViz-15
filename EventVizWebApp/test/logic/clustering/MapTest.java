package logic.clustering;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import scala.Tuple2;

public class MapTest {

	@Test
	public void testProject() {
		Location latLng = new Location("myID", 39.2428725, -94.6588208);
		
		List<Tuple2<Integer, Point>> zoomPointResults = new ArrayList<Tuple2<Integer, Point>>();
		zoomPointResults.add(Tuple2.apply(16, new Point(3977187.2142586307, 6397313.034448437)));
		zoomPointResults.add(Tuple2.apply(15, new Point(1988593.6071293153, 3198656.5172242187)));
		zoomPointResults.add(Tuple2.apply(0, new Point(60.68706076444444, 97.6152501594305)));
		
		for(Tuple2<Integer, Point> zoomPointResult : zoomPointResults)
		{
			Point result = Map.project(latLng, zoomPointResult._1);
			assertEquals(zoomPointResult._2.x, result.x, 0.000001);
			assertEquals(zoomPointResult._2.y, result.y, 0.000001);
		}
		
	
		
	}

}
