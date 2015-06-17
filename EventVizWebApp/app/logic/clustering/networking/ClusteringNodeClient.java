package logic.clustering.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import net.sf.ehcache.terracotta.SerializationHelper;

import org.apache.commons.lang3.SerializationUtils;

import logic.clustering.ClusteringWorker;
import logic.clustering.ILocation;
import logic.clustering.MarkerCluster;

public class ClusteringNodeClient implements ClusteringWorker {
	private final Socket clientSocket;
	private final DataOutputStream outputStream;
	private final DataInputStream inputStream;
	private Exception exception;
	
	
	public ClusteringNodeClient(Socket clientSocket) throws IOException
	{
		this.clientSocket = clientSocket;
		// Try to open input and output streams
        outputStream = new DataOutputStream(clientSocket.getOutputStream());
        inputStream = new DataInputStream(clientSocket.getInputStream());
	}
	

	public ClusteringNodeClient(String hostname, int port) throws UnknownHostException, IOException {
		this(new Socket(hostname, port));		
	}


	@Override
	public void addLocation(ILocation location) {
		synchronized(this)
		{
			try {
				outputStream.writeByte(ClusteringMessageType.AddLocation.getVal());
				outputStream.writeLong(location.getId());
				outputStream.writeDouble(location.getLatitude());
				outputStream.writeDouble(location.getLongitude());
				outputStream.writeUTF(location.getName());
			}catch(Exception ex)
			{
				this.exception = ex;
			}
		}
	}

	@Override
	public synchronized MarkerCluster waitForResult() throws Exception {
		if(exception != null)
		{
			throw exception;
		}
		
		outputStream.writeByte(ClusteringMessageType.WaitForResult.getVal());
		
		byte messageType = inputStream.readByte();
		ClusteringMessageType cMessageType = ClusteringMessageType.fromByte(messageType);
		switch(cMessageType)
		{
			case Exception:
				String message = inputStream.readUTF();
				throw new Exception(message);
			case WaitForResultResponse:				
				return (MarkerCluster)MySerializationHelper.deserialize(inputStream);
			default:
				throw new RuntimeException("Not implemented yet: " + cMessageType);
		}
	}


	@Override
	public void close() throws Exception {
		if(this.clientSocket != null)
		{
			this.clientSocket.close();
		}
	}

}
