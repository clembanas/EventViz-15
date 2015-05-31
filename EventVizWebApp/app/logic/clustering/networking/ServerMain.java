package logic.clustering.networking;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import logic.clustering.LocalClusteringWorker;

public class ServerMain {

	public static void main(String[] args) {
		try
		{
			try(ServerSocket server = new ServerSocket(9999))
			{
				System.out.println("ClusteringNodeServer running on " + server.getInetAddress() + ":" + server.getLocalPort());
				while(true)
				{
					final Socket socket = server.accept();
					System.out.println(socket.getInetAddress() + " has connected");
					new Thread(new Runnable()
					{
						
						@Override
						public void run() {
							try {
								new ClusteringNodeServer(socket, new LocalClusteringWorker()).doWork();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						
					}).start();
				}
			}
		}catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}

}
