/**
 * @author Bernhard Weber
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.commons.lang3.ObjectUtils.Null;


/**
 * Manages remote connections and provides remote object control facilities.
 */
public class RemoteObjectManager {

	public static int REMOTE_OBJECT_MGR_PORT = 9125;
	
	/**
	 * Available debug flags
	 */
	public static enum DebugFlag implements DebugUtils.DebugFlagBase {
		CONNECTION(1),
		REMOTE_OBJECT(2);
		
		public final int value;
		
		DebugFlag(int value) 
		{
			this.value = value;
		}

		public int toInt() 
		{
			return value;
		}
	}

	/**
	 * Internally used object accessor interface 
	 */
	private static interface ObjectAccessor {
		
		public void close();
		
		public Object getObject();
		
		public Class<?> getRemoteObjectInterface();
	}
	
	/**
	 * Utility class to access a local object. 
	 */
	private static class LocalObjectAccessor implements ObjectAccessor {
		
		private Object obj;
		
		public LocalObjectAccessor(Object obj)
		{
			this.obj = obj;
		}
		
		public synchronized Object invoke(String mthdName, Object args[]) throws Exception
		{
			if (obj == null)
				throw new RemoteObjectException("Access to remote object closed!");
			
			Class<?>[] argTypes = new Class<?>[args.length];
			
			for (int i = 0; i < args.length; ++i) {
				if (args[i] == null)
					argTypes[i] = Null.class;
				else
					argTypes[i] = args[i].getClass();
			}
			return obj.getClass().getMethod(mthdName, argTypes).invoke(obj, args);	
		}
		
		public synchronized void close() 
		{
			obj = null;
		}
		
		public Object getObject()
		{
			return obj;
		}

		public Class<?> getRemoteObjectInterface() 
		{
			return obj.getClass();
		}
	}
	
	/**
	 * Utility class to access a remote object. 
	 */
	private static class RemoteObjectAccessor implements ObjectAccessor, InvocationHandler {

		private Class<?> remObjInterface;
		private Object proxy; 
		private RemoteConnection remConnection;
		
		private void performHandshake() throws Exception
		{
			DebugUtils.printDebugInfo("Performing handshake for remote object '" + 
				remObjInterface.getName() +	"' with server '" + 
				remConnection.getSocket().getInetAddress().getHostAddress() + 
				"' ...", RemoteObjectManager.class, null, getClass(), DebugFlag.REMOTE_OBJECT);
			
			remConnection.writeString(remObjInterface.getName());
			String resp = remConnection.readString();
			
			if (!resp.equalsIgnoreCase("READY")) {
				RemoteObjectException e = new RemoteObjectException(resp);
				remConnection.close();
				ExceptionHandler.handle("Failed to perform handshake for remote object '" + 
					remObjInterface.getName() + "' with server '" + 
					remConnection.getSocket().getInetAddress().getHostAddress() + "'!", e, 
					RemoteObjectManager.class, null, getClass());
				throw e;
			}
			DebugUtils.printDebugInfo("Performing handshake for remote object '" + 
				remObjInterface.getName() +	"' with server '" + 
				remConnection.getSocket().getInetAddress().getHostAddress() + 
				"' ... DONE", RemoteObjectManager.class, null, getClass(), DebugFlag.REMOTE_OBJECT);
		}
		
		public RemoteObjectAccessor(Class<?> remObjInterface, InetAddress remAddr) throws Exception 
		{
			this.remObjInterface = remObjInterface;
			this.proxy = Proxy.newProxyInstance(remObjInterface.getClassLoader(), 
						     new Class[]{remObjInterface}, this);
			remConnection = new RemoteConnection(remAddr);
			performHandshake();
		}
		
		public synchronized Object invoke(Object obj, Method mthd, Object[] args) throws Exception 
		{
			DebugUtils.printDebugInfo("Invoking method '" + mthd.getName() + 
				"' of remote object '" + remObjInterface.getName() + "' on server '" + 
				remConnection.getSocket().getInetAddress().getHostAddress() + 
				"' ...", RemoteObjectManager.class, null, getClass(), DebugFlag.REMOTE_OBJECT);
			
			remConnection.writeString("FUNC: " + mthd.getName());
			remConnection.writeObjects(args);
			
			String resp = remConnection.readString();
			if (!resp.toLowerCase().startsWith("RESULT: ")) {
				RemoteObjectException e = new RemoteObjectException(resp);
				ExceptionHandler.handle("Failed to invoke method '" + mthd.getName() + 
					"' of remote object '" + remObjInterface.getName() + 
					"' on server '" + remConnection.getSocket().getInetAddress().getHostAddress() + 
					"'!", e, RemoteObjectManager.class, null, getClass());
				throw e;
			}
			Object res = remConnection.readObject();
			
			DebugUtils.printDebugInfo("Invoking method '" + mthd.getName() + 
				"' of remote object '" + remObjInterface.getName() + "' on server '" + 
				remConnection.getSocket().getInetAddress().getHostAddress() + 
				"' ... DONE", RemoteObjectManager.class, null, getClass(), DebugFlag.REMOTE_OBJECT);
			return res;
		}
		
		public synchronized void close()
		{
			DebugUtils.printDebugInfo("Closing access to remote object '" + 
				remObjInterface.getName() + "' on server '" + 
				remConnection.getSocket().getInetAddress().getHostAddress() + 
				"' ...", RemoteObjectManager.class, null, getClass(), DebugFlag.REMOTE_OBJECT);
			try {
				remConnection.writeString("CLOSE");
				
				String resp = remConnection.readString();
				if (!resp.equalsIgnoreCase("CLOSED"))
					throw new RemoteObjectException(resp);
				DebugUtils.printDebugInfo("Closing access to remote object '" + 
					remObjInterface.getName() + "' on server '" + 
					remConnection.getSocket().getInetAddress().getHostAddress() + 
					"' ... DONE", RemoteObjectManager.class, null, getClass(), 
					DebugFlag.REMOTE_OBJECT);
			}
			catch (Exception e) {
				ExceptionHandler.handle("Failed to close access to remote object!", e, 
					RemoteObjectManager.class, null, getClass());
			}
			remConnection.close();
			proxy = null;
		}

		public Object getObject() 
		{
			return proxy;
		}
		
		public Class<?> getRemoteObjectInterface() 
		{
			return remObjInterface.getClass();
		}
	}

	/**
	 * Utility class which encapsulates a TCP connection to a remote host. 
	 */
	private static class RemoteConnection
	{
		private Socket sock;
		private DataInputStream sockIn;
		private DataOutputStream sockOut;
		
		RemoteConnection(Socket sock) throws Exception
		{
			this.sock = sock;
			sockIn = new DataInputStream(sock.getInputStream());
			sockOut = new DataOutputStream(sock.getOutputStream());
		}
		
		RemoteConnection(InetAddress remAddr) throws Exception
		{
			this.sock = new Socket(remAddr, REMOTE_OBJECT_MGR_PORT);
			sockIn = new DataInputStream(sock.getInputStream());
			sockOut = new DataOutputStream(sock.getOutputStream());
		}
		
		public String readString() throws Exception
		{
			int dataLen = sockIn.readInt();
			byte[] data = new byte[dataLen];
			
			if (dataLen == 0)
				return "";
			if (sockIn.read(data) != dataLen)
				throw new RemoteObjectException("Incomplete data transmission!");
			return new String(data);
		}
		
		public Object readObject() throws Exception
		{
			int dataLen = sockIn.readInt(); 
			byte[] data = new byte[dataLen];
			
			dataLen = sockIn.readInt();
			if (dataLen == 0)
				return null;
			if (sockIn.read(data) != dataLen)
				throw new RemoteObjectException("Incomplete data transmission!");
			return (new ObjectInputStream(new ByteArrayInputStream(data))).readObject();
		}
		
		public Object[] readObjects() throws Exception
		{
			int objCnt = sockIn.readInt();
			Object[] objs = new Object[objCnt];
			int dataLen;
			byte[] data = null;
			
			for (int i = 0; i < objCnt; ++i) {
				dataLen = sockIn.readInt();
				if (dataLen == 0)
					continue;
				if (data == null || data.length < dataLen)
					data = new byte[dataLen];
				if (sockIn.read(data) != dataLen)
					throw new RemoteObjectException("Incomplete data transmission!");
				objs[i] = (new ObjectInputStream(new ByteArrayInputStream(data))).readObject();
			}
			return new Object[]{};
		}
		
		public void writeString(String strData) throws Exception
		{
			sockOut.writeInt(strData.getBytes().length);
			sockOut.write(strData.getBytes());
		}
		
		public void writeObject(Object obj) throws Exception
		{
			ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
			ObjectOutputStream objOut = new ObjectOutputStream(byteOut);
			
			if (obj == null) 
				sockOut.writeInt(0);
			else {
				objOut.writeObject(obj);
				sockOut.writeInt(byteOut.size());
				sockOut.write(byteOut.toByteArray());
			}
		}
		
		public void writeObjects(Object ... objs) throws Exception
		{
			ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
			ObjectOutputStream objOut = new ObjectOutputStream(byteOut);
			
			sockOut.writeInt(objs.length);
			for (Object obj: objs) {
				if (obj == null) 
					sockOut.writeInt(0);
				else {
					byteOut.reset();
					objOut.reset();
					objOut.writeObject(obj);
					sockOut.writeInt(byteOut.size());
					sockOut.write(byteOut.toByteArray());
				}
			}
		}
		
		public void close()
		{
			try {
				sockIn.close();
				sockOut.close();
			}
			catch (Exception e) {}
			try {
				sock.close();
			}
			catch (Exception e) {}
		}
		
		public Socket getSocket()
		{
			return sock;
		}
	}
	
	/**
	 * Socket handler thread for incoming/ accepted client connections 
	 */
	private static class ClientSocketHandler implements Runnable {

		private RemoteConnection remConnection;
		private LocalObjectAccessor localObjAccessor;
		
		private void performHandshake() throws Exception
		{
			String className = remConnection.readString();
			
			DebugUtils.printDebugInfo("Performing handshake for remote object '" + className + 
				"' with client '" + remConnection.getSocket().getInetAddress().getHostAddress() + 
				"' ...", RemoteObjectManager.class, null, getClass(), DebugFlag.REMOTE_OBJECT);
			try {
				localObjAccessor = getLocalObjectAccessor(Class.forName(className));
			}
			catch (Exception e) {
				remConnection.writeString("ERROR: " + e.getMessage());
				ExceptionHandler.handle("Failed to perform handshake for remote object '" + 
					className + "' with client '" + 
					remConnection.getSocket().getInetAddress().getHostAddress() + "'!", e, 
					RemoteObjectManager.class, null, getClass());
				throw e;
			}
			remConnection.writeString("READY");
			DebugUtils.printDebugInfo("Performing handshake for remote object '" + className + 
				"' with client '" + remConnection.getSocket().getInetAddress().getHostAddress() + 
				"' ... DONE", RemoteObjectManager.class, null, getClass(), DebugFlag.REMOTE_OBJECT);
		}
		
		private void processMethodInvocation(String mthdName) throws Exception
		{
			Object args[] = remConnection.readObjects();
			
			DebugUtils.printDebugInfo("Invoking method '" + mthdName + "' of remote object '" + 
				localObjAccessor.getRemoteObjectInterface().getName() + "' for client '" + 
				remConnection.getSocket().getInetAddress().getHostAddress() + 
				"' ...", RemoteObjectManager.class, null, getClass(), DebugFlag.REMOTE_OBJECT);
			try {
				Object res = localObjAccessor.invoke(mthdName, args);
				remConnection.writeString("RESULT: ");
				remConnection.writeObject(res);
				DebugUtils.printDebugInfo("Invoking method '" + mthdName + "' of remote object '" + 
					localObjAccessor.getRemoteObjectInterface().getName() + "' for client '" + 
					remConnection.getSocket().getInetAddress().getHostAddress() + 
					"' ... DONE", RemoteObjectManager.class, null, getClass(), 
					DebugFlag.REMOTE_OBJECT);
			}
			catch (Exception e) {
				remConnection.writeString("ERROR: " + e.getMessage());
				ExceptionHandler.handle("Failed to invoke method '" + mthdName + 
					"' of remote object '" + localObjAccessor.getRemoteObjectInterface().getName() + 
					"' for client '" + remConnection.getSocket().getInetAddress().getHostAddress() + 
					"'!", e, RemoteObjectManager.class, null, getClass());
			}
		}
		
		public ClientSocketHandler(Socket clientSock) throws Exception 
		{
			remConnection = new RemoteConnection(clientSock);
		}

		public void run() 
		{
			String cmd;
			
			try {
				performHandshake();
				while (true) {
					cmd = remConnection.readString();
					if (cmd.equalsIgnoreCase("close")) {
						DebugUtils.printDebugInfo("Received close request from client '" + 
							remConnection.getSocket().getInetAddress().getHostAddress() + 
							"'", RemoteObjectManager.class, null, getClass(), 
							DebugFlag.CONNECTION);
						remConnection.writeString("CLOSED");
						return;
					}
					else if (cmd.toLowerCase().startsWith("func: ")) 
						processMethodInvocation(cmd.substring(6));
				}
			}
			catch (Exception e) {
				ExceptionHandler.handle("Failed to handle connection to client '" + 
					remConnection.getSocket().getInetAddress().getHostAddress() + "'!", e, 
					RemoteObjectManager.class, null, getClass());
			}
			finally {
				remConnection.close();
				DebugUtils.printDebugInfo("Connection to client '" + 
					remConnection.getSocket().getInetAddress().getHostAddress() + 
					"' closed", RemoteObjectManager.class, null, getClass(), 
					DebugFlag.CONNECTION);
			}
		}
	}
	
	/**
	 * Socket handler thread for server connections 
	 */
	private static class ServerSocketHandler extends Thread {
		
		private ServerSocket sock;
		private ExecutorService thdPool;
		private List<Utils.Pair<Socket, Future<?>>> pendingClientHandlers = 
			new ArrayList<Utils.Pair<Socket, Future<?>>>();
		
		public ServerSocketHandler(int port, ExecutorService thdPool) throws Exception 
		{
			this.sock = new ServerSocket(port);
			this.thdPool = thdPool;
			DebugUtils.printDebugInfo("Server socket created - waiting for incoming connections...", 
				RemoteObjectManager.class, null, getClass(), DebugFlag.CONNECTION);
		}
		
		public void run()
		{
			Socket newClientConn;
			
			while (!sock.isClosed()) {
				try {
					newClientConn = sock.accept();
					DebugUtils.printDebugInfo("Accepted client connection from host '" + 
						newClientConn.getInetAddress().getHostAddress() + "'", 
						RemoteObjectManager.class, null, getClass(), DebugFlag.CONNECTION);
					//Clear finished client handlers
					for (int i = 0, cnt = pendingClientHandlers.size(); i < cnt; ) {
						if (pendingClientHandlers.get(i).second.isDone()) {
							//Remove finished client handler
							if (newClientConn == null) {
								pendingClientHandlers.remove(i);
								continue;
							}
							//Reuse existing slot in pendingClientHandlers
							pendingClientHandlers.set(i, new Utils.Pair<Socket, Future<?>>(
								newClientConn, thdPool.submit(new ClientSocketHandler(
								newClientConn))));
							newClientConn = null;
						}
						i++;
					}
					//Add new client handler
					if (newClientConn != null)
						pendingClientHandlers.add(new Utils.Pair<Socket, Future<?>>(newClientConn, 
							thdPool.submit(new ClientSocketHandler(newClientConn))));
				} 
				catch (Exception e) {
					if (!sock.isClosed()) 
						ExceptionHandler.handle("Failed to accept incomming client connection!", e,
							RemoteObjectManager.class, null, getClass());
					else
						return;
				}
			}
		}
		
		public void shutdown()
		{
			try {
				//Close server socket 
				sock.close();
				join();
				//Wait for all client handlers to be finished
				for (Utils.Pair<Socket, Future<?>> pendClientHandler: pendingClientHandlers) {
					try {
						pendClientHandler.first.close();
						pendClientHandler.second.get();
					}
					catch (Exception e) {}
				}
			}
			catch (Exception e) {};
			DebugUtils.printDebugInfo("Server socket shutdown",	RemoteObjectManager.class, null, 
				getClass(), DebugFlag.CONNECTION);
		}
	}

	/**
	 * Exception class for RemoteObjectManager specific exceptions 
	 */
	public static class RemoteObjectManagerException extends Exception {
		
		private static final long serialVersionUID = -9194732425281074132L;

		public RemoteObjectManagerException(String exception)
		{
			super(exception);
		}
	}
	
	/**
	 * Exception class for remote object specific exceptions 
	 */
	public static class RemoteObjectException extends Exception {
		
		private static final long serialVersionUID = 5777065625112497706L;

		public RemoteObjectException(String exception)
		{
			super(exception);
		}
	}

	
	private static Map<String, Utils.Pair<LocalObjectAccessor, Map<InetAddress, 
		RemoteObjectAccessor>>> registeredObjs = 
			new HashMap<String, Utils.Pair<LocalObjectAccessor, Map<InetAddress, 
					RemoteObjectAccessor>>>();
	private static ServerSocketHandler serverSockHandler; 
	
	private static synchronized LocalObjectAccessor getLocalObjectAccessor(Class<?> remObjInterface) 
		throws Exception
	{
		if (!registeredObjs.containsKey(remObjInterface.getName()))
			throw new RemoteObjectManagerException("Unknown remote object '" + 
						   remObjInterface.getName() + "'!");
		
		Utils.Pair<LocalObjectAccessor, Map<InetAddress, RemoteObjectAccessor>> objAccessors =
			registeredObjs.get(remObjInterface.getName());
		
		if (objAccessors.first == null)
			objAccessors.first = new LocalObjectAccessor(Class.forName(remObjInterface.getName() + 
									     "Impl").newInstance());
		return objAccessors.first;
	}
	
	private static synchronized RemoteObjectAccessor getRemoteObjectAccessor(
		Class<?> remObjInterface, InetAddress remAddr) throws Exception
	{
		if (!registeredObjs.containsKey(remObjInterface.getName()))
			throw new RemoteObjectManagerException("Unknown remote object '" + 
						   remObjInterface.getName() + "'!");
		
		Utils.Pair<LocalObjectAccessor, Map<InetAddress, RemoteObjectAccessor>> objAccessors =
			registeredObjs.get(remObjInterface.getName());
		
		if (objAccessors.second == null) 
			objAccessors.second = new HashMap<InetAddress, RemoteObjectAccessor>();
		
		RemoteObjectAccessor remObjAccessor = objAccessors.second.get(remAddr);
		
		if (remObjAccessor == null) {
			remObjAccessor = new RemoteObjectAccessor(remObjInterface, remAddr);
			objAccessors.second.put(remAddr, remObjAccessor);
		}
		return remObjAccessor;
	}
	
	public static void start(ExecutorService thdPool) throws Exception
	{
		REMOTE_OBJECT_MGR_PORT = CrawlerConfig.getRemoteObjMgrPort();
		DebugUtils.printDebugInfo("RemoteObjectManager started (Port: " + REMOTE_OBJECT_MGR_PORT + 
			")", RemoteObjectManager.class);
		serverSockHandler = new ServerSocketHandler(REMOTE_OBJECT_MGR_PORT, thdPool);
		serverSockHandler.start();
	}
	
	public static void shutdown()
	{
		if (serverSockHandler != null) 		
			serverSockHandler.shutdown();
		DebugUtils.printDebugInfo("RemoteObjectManager shutdown", RemoteObjectManager.class);
	}
	
	public static synchronized void registerRemoteObject(Class<?> remObjInterface)
	{
		registeredObjs.put(remObjInterface.getName(), new Utils.Pair<LocalObjectAccessor, 
			Map<InetAddress, RemoteObjectAccessor>>(null, null));
	}
	
	public static synchronized void unregisterRemoteObject(Class<?> remObjInterface)
	{
		registeredObjs.remove(remObjInterface.getName());
	}
	
	@SuppressWarnings("unchecked")
	public static synchronized <RemoteObject> RemoteObject getRemoteObject(
		Class<RemoteObject> remObjInterface, InetAddress remAddr) throws Exception
	{
		if (remAddr == null || InetAddress.getLocalHost() == remAddr) {
			DebugUtils.printDebugInfo("Locale host is responsible for remote object '" + 
				remObjInterface + "'", RemoteObjectManager.class, DebugFlag.REMOTE_OBJECT);
			return (RemoteObject)getLocalObjectAccessor(remObjInterface).getObject();
		}
		DebugUtils.printDebugInfo("Remote host '" + remAddr.getHostAddress() + 
			"' is responsible for remote object '" + remObjInterface  + "'", 
			RemoteObjectManager.class, DebugFlag.REMOTE_OBJECT);
		return (RemoteObject)getRemoteObjectAccessor(remObjInterface, remAddr).getObject();
	}
	
	public static synchronized <RemoteObject> RemoteObject getRemoteObject(
		Class<RemoteObject> remObjInterface) throws Exception
	{
		return getRemoteObject(remObjInterface, null);
	}
	
	public static synchronized void closeRemoteObject(Class<?> remObjInterface, 
		InetAddress remAddr) throws Exception
	{
		Utils.Pair<LocalObjectAccessor, Map<InetAddress, RemoteObjectAccessor>> objAccessors =
			registeredObjs.get(remObjInterface.getName());
		
		if (remAddr == null || InetAddress.getLocalHost() == remAddr) {
			if (objAccessors.first != null) {
				objAccessors.first.close();
				objAccessors.first = null;
			}
		} 
		else if (objAccessors.second != null) {
			RemoteObjectAccessor objAccessor = objAccessors.second.get(remAddr);
			
			if (objAccessor != null)
				objAccessor.close();
			objAccessors.second.remove(remAddr);
			if (objAccessors.second.isEmpty())
				objAccessors.second = null;
		}
	}
	
	public static synchronized void closeRemoteObject(Class<?> remObjInterface) throws Exception
	{
		closeRemoteObject(remObjInterface, null);
	}
	
	public static synchronized void closeAllRemoteObjects(Class<?> remObjInterface) throws Exception
	{
		Utils.Pair<LocalObjectAccessor, Map<InetAddress, RemoteObjectAccessor>> objAccessors =
			registeredObjs.get(remObjInterface.getName());
		
		if (objAccessors.first != null) {
			objAccessors.first.close();
			objAccessors.first = null;
		}
		if (objAccessors.second != null) {
			for (ObjectAccessor objAccessor: objAccessors.second.values()) 
				objAccessor.close();
			objAccessors.second.clear();
			objAccessors.second = null;
		}
	}
	
	public static synchronized void closeAllRemoteObjects() throws Exception
	{
		for (Utils.Pair<LocalObjectAccessor, Map<InetAddress, RemoteObjectAccessor>> objAccessors:
			registeredObjs.values()) {
			if (objAccessors.first != null) {
				objAccessors.first.close();
				objAccessors.first = null;
			}
			if (objAccessors.second != null) {
				for (ObjectAccessor objAccessor: objAccessors.second.values()) 
					objAccessor.close();
				objAccessors.second.clear();
				objAccessors.second = null;
			}
		}
	}
}
