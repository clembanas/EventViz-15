package logic.clustering.networking;

import java.io.DataInputStream;

import logic.clustering.Location;
import logic.clustering.LocationProxy;
import logic.clustering.Marker;
import logic.clustering.MarkerCluster;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class MySerializationHelper {
	private static Kryo kryo = new Kryo();
	static 
	{
		kryo.register(Location.class, kryo.getNextRegistrationId());
		kryo.register(LocationProxy.class, kryo.getNextRegistrationId());
		kryo.register(Marker.class, kryo.getNextRegistrationId());
		kryo.register(MarkerCluster.class, kryo.getNextRegistrationId());
	}
	
	
	private static final int BUFFER_SIZE = 104857600; // 100 MB

	public static MarkerCluster deserialize(DataInputStream inputStream) {
		
		//return (MarkerCluster)SerializationUtils.deserialize(inputStream);
		
		try(Input input = new Input(inputStream, BUFFER_SIZE))
		{
			return kryo.readObject(input, MarkerCluster.class);
		}		
	}

	public static byte[] serialize(MarkerCluster result) {
		//return SerializationUtils.serialize(result);
		Output output = new Output(BUFFER_SIZE);
		kryo.writeObject(output, result);
		output.close();
		return output.toBytes();
	}

}
