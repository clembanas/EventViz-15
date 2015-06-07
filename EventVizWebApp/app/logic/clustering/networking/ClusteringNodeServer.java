package logic.clustering.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import org.springframework.util.SerializationUtils;

import logic.clustering.ClusteringWorker;
import logic.clustering.ILocation;
import logic.clustering.Location;
import logic.clustering.MarkerCluster;

public class ClusteringNodeServer {
	private final DataOutputStream outputStream;
	private final DataInputStream inputStream;
	private ClusteringWorker worker;
	
	public ClusteringNodeServer(Socket serverSocket, ClusteringWorker worker) throws IOException
	{
		// Try to open input and output streams
        outputStream = new DataOutputStream(serverSocket.getOutputStream());
        inputStream = new DataInputStream(serverSocket.getInputStream());
        this.worker = worker;
	}
	
	public void doWork() {
		try
		{
			ClusteringMessageType cMessageType;
			do
			{
				byte messageType = inputStream.readByte();
				cMessageType = ClusteringMessageType.fromByte(messageType);
				
				switch(cMessageType)
				{
					case AddLocation:
						ILocation location = receiveLocation();
						worker.addLocation(location);
						break;
					case WaitForResult:
						MarkerCluster result = worker.waitForResult();
						writeResult(result);
						break;
					default:
						throw new RuntimeException("Not implemented yet");
				}
			}while(cMessageType != ClusteringMessageType.WaitForResult);
		
		}catch(Exception ex)
		{
			ex.printStackTrace();
			tryWriteException(ex);
		}
	}

	private void tryWriteException(Exception e) {
		try
		{
			outputStream.writeByte(ClusteringMessageType.Exception.getVal());
			outputStream.writeUTF(e.getMessage());
		}catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}

	private void writeResult(MarkerCluster result) throws IOException {
		byte[] bytes = SerializationUtils.serialize(result);
		outputStream.writeByte(ClusteringMessageType.WaitForResultResponse.getVal());
		outputStream.write(bytes);
	}

	private ILocation receiveLocation() throws IOException {	
		long id = inputStream.readLong();
		double lat = inputStream.readDouble();
		double lng = inputStream.readDouble();
		String name = inputStream.readUTF();
		
		return new Location(id, lat, lng, name);
	}
}
