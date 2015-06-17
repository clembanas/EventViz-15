/**
 * @author Bernhard Weber
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;


/**
 * Manages remote connections and provides remote object control facilities.
 */
public class RemoteObjectManager {

	private static InetAddress LOCAL_HOST_ADDR;
	private static int REMOTE_OBJECT_MGR_PORT = 9125;

	
	/**
	 * Server connection listener interface
	 */
	public static interface ServerConnectionListener {
		
		public void established(InetAddress client);
		public void closed(InetAddress client);
	}
	
	
	/**
	 * Available debug flags
	 */
	public static enum DebugFlag implements DebugUtils.DebugFlagBase {
		CONNECTION(1),
		REMOTE_OBJECT(2),
		METHOD_ARGUMENTS(4);
		
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
	 * Interface used by the RemoteObjectManager to create an instance of a remote object.
	 * Note: Its up to the implementing class to provide singleton facilities.
	 */
	public static interface RemoteObjectCreator<RemObjInterface> {
		
		RemObjInterface createRemoteObject() throws Exception;
	}
	
	/**
	 * Internally used remote object identifier.
	 */
	private static class RemoteObjectID {
		
		private String remObjClassName;
		private String remObjContext;
		
		public RemoteObjectID(Class<?> remObjClass, String remObjContext)
		{
			this.remObjClassName = remObjClass.getName();
			this.remObjContext = remObjContext;
		}
		
		public RemoteObjectID(String remObjID) throws Exception
		{
			if (!remObjID.contains("@")) {
				this.remObjClassName = remObjID;
				this.remObjContext = null;
			}
			else {
				String[] parts = remObjID.split("@");
			
				if (parts.length != 2)
					throw new Exception("Invalid remote object identifier!");
				this.remObjClassName = parts[0];
				this.remObjContext = parts[1];
			}
		}
		
		public int hashCode()
		{
			return remObjContext == null ? remObjClassName.hashCode() : 
					   (remObjClassName + "@" + remObjContext).hashCode();
		}
		
		public boolean equals(Object obj)
		{
			return obj instanceof RemoteObjectID &&
					   remObjClassName.equals(((RemoteObjectID)obj).remObjClassName) &&
					   ((remObjContext != null && 
					   remObjContext.equals(((RemoteObjectID)obj).remObjContext)) ||
					   (remObjContext == null && ((RemoteObjectID)obj).remObjContext == null));
		}
		
		public String toString()
		{
			return remObjContext == null ? remObjClassName : (remObjClassName + "@" + 
					   remObjContext);
		}
		
		public static String formatAsID(Class<?> remObjClass, String remObjContext)
		{
			return remObjContext == null ? remObjClass.getName() : (remObjClass.getName() + "@" + 
					   remObjContext);
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
		
		public RemoteConnection(Socket sock) throws Exception
		{
			this.sock = sock;
			sockIn = new DataInputStream(sock.getInputStream());
			sockOut = new DataOutputStream(sock.getOutputStream());
		}
		
		public RemoteConnection(InetAddress remAddr) throws Exception
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
			if (dataLen == 0)
				return null;
			
			byte[] data = new byte[dataLen];
			try {
				sockIn.readFully(data);
			}
			catch (EOFException e) {
				throw new RemoteObjectException("Incomplete data transmission (Expected " + 
							  dataLen + " Bytes)!", e);
			}
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
				if (data == null || data.length != dataLen)
					data = new byte[dataLen];
				try {
					sockIn.readFully(data);
				}
				catch (EOFException e) {
					throw new RemoteObjectException("Incomplete data transmission (Expected " + 
								  dataLen + " Bytes)!", e);
				}
				objs[i] = (new ObjectInputStream(new ByteArrayInputStream(data))).readObject();
			}
			return objs;
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
				objOut.flush();
				sockOut.writeInt(byteOut.size());
				sockOut.write(byteOut.toByteArray());
			}
		}
		
		public void writeObjects(Object ... objs) throws Exception
		{
			ByteArrayOutputStream byteOut;
			ObjectOutputStream objOut;
			
			if (objs == null)
				sockOut.writeInt(0);
			else {
				sockOut.writeInt(objs.length);
				for (Object obj: objs) {
					if (obj == null) 
						sockOut.writeInt(0);
					else {
						byteOut = new ByteArrayOutputStream();
						objOut = new ObjectOutputStream(byteOut);
						objOut.writeObject(obj);
						objOut.flush();
						sockOut.writeInt(byteOut.size());
						sockOut.write(byteOut.toByteArray());
					}
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
		
		public InetAddress getRemoteAddress()
		{
			return sock.getInetAddress();
		}
		
		public String getRemoteIPAddress()
		{
			return sock.getInetAddress().getHostAddress();
		}
	}

	/**
	 * Internally used object accessor interface 
	 */
	private static interface ObjectAccessor {
		
		public Object invoke(String mthdName, Object args[]) throws Exception;
		public Class<?> getRemoteObjectClass();
		public Object getRemoteObject();
		public String getRemoteObjectStrID();
		public InetAddress getHostAddress();
	}
	
	/**
	 * Controls the access and lifetime of all remote object instances of one certain remote object 
	 * class. 
	 */
	private static class RemoteObjectController {
		
		/**
		 * Extension of the ObjectAccessor interface allowing to manage multiple references of the 
		 * an object accessor. 
		 */
		private static interface ObjectAccessorRef extends ObjectAccessor {
			
			public void createRef() throws Exception;
			public void releaseRef(boolean releaseAllRefs) throws Exception;
			public int getRefCount();
		}
		
		/**
		 * Utility class to access a local object. 
		 */
		private class LocalObjectAccessor implements ObjectAccessorRef {
			
			private int refCnt = 1;
			private Object locObjInst;
			
			public LocalObjectAccessor(Object locObjInst) throws Exception
			{
				this.locObjInst = locObjInst;
				DebugUtils.printDebugInfo("Locale instance of remote object '" + remObjStrID +
					"' created (References: " + refCnt + ")", RemoteObjectManager.class, 
					DebugFlag.REMOTE_OBJECT);
			}
			
			public synchronized void createRef() throws Exception
			{
				refCnt++;
				DebugUtils.printDebugInfo("Locale instance of remote object '" + remObjStrID +
					"' created (References: " + refCnt + ")", RemoteObjectManager.class, 
					DebugFlag.REMOTE_OBJECT);
			}

			public synchronized void releaseRef(boolean releaseAllRefs) throws Exception
			{
				if (releaseAllRefs || --refCnt == 0) {
					refCnt = 0;
					locObjInst = null;
					DebugUtils.printDebugInfo("Locale object '" + remObjStrID +	"' closed",	
						RemoteObjectManager.class, null, getClass(), DebugFlag.REMOTE_OBJECT);
				}
			}

			public synchronized int getRefCount()
			{
				return refCnt;
			}
			
			public synchronized Object invoke(String mthdName, Object args[]) throws Exception
			{
				if (locObjInst == null)
					throw new RemoteObjectException("Remote object '" + remObjStrID + "' closed!");
				
				Class<?>[] argTypes = new Class<?>[args.length];
				
				for (int i = 0; i < args.length; ++i) {
					if (args[i] == null)
						argTypes[i] = null;
					else
						argTypes[i] = args[i].getClass();
				}
				return locObjInst.getClass().getMethod(mthdName, argTypes).invoke(locObjInst, args);	
			}
			
			public Class<?> getRemoteObjectClass()
			{
				return remObjClass;
			}
			
			public Object getRemoteObject()
			{
				return locObjInst;
			}
			
			public String getRemoteObjectStrID()
			{
				return remObjStrID;
			}
			
			public InetAddress getHostAddress()
			{
				return LOCAL_HOST_ADDR;
			}
		}
		
		/**
		 * Utility class to access a remote object. 
		 */
		private class RemoteObjectAccessor implements ObjectAccessorRef {

			private Object proxy; 
			private RemoteConnection remConnection;
			
			private void performHandshake() throws Exception
			{
				DebugUtils.printDebugInfo("Performing handshake for remote object '" + remObjStrID +
					"' with server '" +	remConnection.getRemoteIPAddress() + "' ...", 
					RemoteObjectManager.class, null, getClass(), DebugFlag.REMOTE_OBJECT);
				
				remConnection.writeString(remObjStrID);
				String resp = remConnection.readString();
				
				if (!resp.equalsIgnoreCase("READY")) {
					RemoteObjectException e = new RemoteObjectException(resp);
					remConnection.close();
					ExceptionHandler.handle("Failed to perform handshake for remote object '" + 
						remObjStrID + "' with server '" + remConnection.getRemoteIPAddress() + "'!", 
						e, RemoteObjectManager.class, null, getClass());
					throw e;
				}
				DebugUtils.printDebugInfo("Performing handshake for remote object '" + remObjStrID +
					"' with server '" + remConnection.getRemoteIPAddress() + "' ... DONE", 
					RemoteObjectManager.class, null, getClass(), DebugFlag.REMOTE_OBJECT);
			}

			public RemoteObjectAccessor(InetAddress remAddr) throws Exception 
			{
				this.proxy = Proxy.newProxyInstance(remObjClass.getClassLoader(), 
							     getRemoteObjectInterfaces(), new InvocationHandler() {
									
									public Object invoke(Object proxy, Method method, Object[] args)
										throws Throwable 
									{
										return RemoteObjectAccessor.this.invoke(method.getName(), 
												   args);
									}
								});
				remConnection = new RemoteConnection(remAddr);
				try {
					performHandshake();
					DebugUtils.printDebugInfo("Remote instance of remote object '" + remObjStrID +
						"' on host '" + remAddr.getHostAddress() + "' created", 
						RemoteObjectManager.class,	DebugFlag.REMOTE_OBJECT);
				}
				catch (Exception e) {
					remConnection.close();
					throw e;
				}
			}
			
			public void createRef() 
			{
				throw new UnsupportedOperationException("Can't create multiple references of " + 
							   "remote object accessor of '" + remObjStrID + "'!");
			}

			public synchronized void releaseRef(boolean releaseAllRefs) throws Exception
			{
				DebugUtils.printDebugInfo("Closing remote object '" + remObjStrID + 
					"' on server '" + remConnection.getRemoteIPAddress() + "' ...", 
					RemoteObjectManager.class, null, getClass(), DebugFlag.REMOTE_OBJECT);
				try {
					remConnection.writeString("CLOSE");
					
					String resp = remConnection.readString();
					if (!resp.equalsIgnoreCase("CLOSED"))
						throw new RemoteObjectException(resp);
					DebugUtils.printDebugInfo("Closing remote object '" + remObjStrID + 
						"' on server '" + remConnection.getRemoteIPAddress() + "' ... DONE", 
						RemoteObjectManager.class, null, getClass(), DebugFlag.REMOTE_OBJECT);
				}
				catch (Exception e) {
					ExceptionHandler.handle("Failed to close remote object '" + remObjStrID + "'!", 
						e, RemoteObjectManager.class, null,	getClass());
					throw e;
				}
				finally {
					remConnection.close();
					proxy = null;
				}
			}
			
			public int getRefCount() 
			{
				return 1;
			}
			
			public synchronized Object invoke(String mthdName, Object[] args) throws Exception 
			{
				DebugUtils.printDebugInfo("Invoking method '" + mthdName + "(" + 
					(DebugUtils.canDebug(RemoteObjectManager.class, DebugFlag.METHOD_ARGUMENTS) ? 
					Utils.objectsToString(args) : "...") + ")' of remote object '" + 
					remObjStrID + "' on server '" + remConnection.getRemoteIPAddress() + "' ...", 
					RemoteObjectManager.class, null, getClass(), DebugFlag.REMOTE_OBJECT);
				
				remConnection.writeString("FUNC: " + mthdName);
				remConnection.writeObjects(args);
				
				String resp = remConnection.readString();
				if (!resp.toLowerCase().startsWith("result: ")) {
					RemoteObjectException e = new RemoteObjectException(resp);
					ExceptionHandler.handle("Failed to invoke method '" + mthdName + "(" + 
						(DebugUtils.canDebug(RemoteObjectManager.class, 
						DebugFlag.METHOD_ARGUMENTS) ? Utils.objectsToString(args) : "...") +
						")' of remote object '" + remObjStrID + "' on server '" + 
						remConnection.getRemoteIPAddress() + "'!", e, RemoteObjectManager.class, 
						null, getClass());
					throw e;
				}
				
				Object res = remConnection.readObject();
				if (DebugUtils.canDebug(RemoteObjectManager.class, DebugFlag.METHOD_ARGUMENTS))
					DebugUtils.printDebugInfo("Invocation of method '" + mthdName + "(" + 
						Utils.objectsToString(args) + ")' of remote object '" + remObjStrID +
						"' on server '" + remConnection.getRemoteIPAddress() + "' returned '" + 
						Utils.objectsToString(res) + "'", RemoteObjectManager.class, null, 
						getClass(), DebugFlag.REMOTE_OBJECT);
				else
					DebugUtils.printDebugInfo("Invoking method '" + mthdName + 
						"(...)' of remote object '" + remObjStrID + "' on server '" +
						remConnection.getRemoteIPAddress() + "' ... DONE",
						RemoteObjectManager.class, null, getClass(), DebugFlag.REMOTE_OBJECT);
				return res;
			}

			public Class<?> getRemoteObjectClass()
			{
				return remObjClass;
			}

			public Object getRemoteObject() 
			{
				return proxy;
			}
			
			public String getRemoteObjectStrID()
			{
				return remObjStrID;
			}
			
			public InetAddress getHostAddress()
			{
				return remConnection.getSocket().getInetAddress();
			}
		}
		
		private Map<Object, ObjectAccessorRef> objAccessorRefs = 
			new HashMap<Object, ObjectAccessorRef>();
		private Class<?> remObjClass;
		private Set<Class<?>> remObjInterfaces = new HashSet<Class<?>>(1); 
		private RemoteObjectCreator<?> remObjCreator;
		private String remObjStrID;
		
		private boolean isLocalAddress(InetAddress addr)
		{
			return addr == null || LOCAL_HOST_ADDR.equals(addr) || addr.isAnyLocalAddress() ||
					   addr.isLoopbackAddress();
		}
		
		public RemoteObjectController(Class<?> remObjInterface, Class<?> remObjClass, 
			RemoteObjectCreator<?> remObjCreator, String remObjContext)
		{
			remObjInterfaces.add(remObjInterface);
			this.remObjClass = remObjClass;
			this.remObjCreator = remObjCreator;
			remObjStrID = RemoteObjectID.formatAsID(remObjClass, remObjContext);
		}
		
		public synchronized void addRemoteObjectInterface(Class<?> remObjInterface) 
		{
			remObjInterfaces.add(remObjInterface);
		}
		
		public synchronized ObjectAccessor getAccessor(InetAddress remAddr) throws Exception
		{
			ObjectAccessorRef objAccessorRef;
			
			if (isLocalAddress(remAddr)) {
				Object remObj = remObjCreator == null ? remObjClass.newInstance() : 
									remObjCreator.createRemoteObject();
				
				//Remote object is singleton				
				if ((objAccessorRef = objAccessorRefs.get(remObj)) != null) {
					objAccessorRef.createRef();
					return objAccessorRef;
				}
				//First instance of remote object created or multiple instances possible
				objAccessorRef = new LocalObjectAccessor(remObj);
			}
			else
				objAccessorRef = new RemoteObjectAccessor(remAddr);
			objAccessorRefs.put(objAccessorRef.getRemoteObject(), objAccessorRef);
			return objAccessorRef;
		}
		
		public ObjectAccessor getAccessor() throws Exception
		{
			return getAccessor(LOCAL_HOST_ADDR);
		}
				
		public synchronized void closeAccessor(Object remObj) throws Exception
		{
			ObjectAccessorRef objAccessorRef = objAccessorRefs.get(remObj);
			
			if (objAccessorRef != null) {
				if (objAccessorRef.getRefCount() == 1)
					objAccessorRefs.remove(remObj);
				objAccessorRef.releaseRef(false);
			}
		}
		
		public synchronized void closeAllAccessors() throws Exception
		{
			Exception exception = null;
			
			for (ObjectAccessorRef objAccessorRef: objAccessorRefs.values()) {
				try {
					objAccessorRef.releaseRef(true);
				}
				catch (Exception e) {
					exception = e;
				}
			}
			objAccessorRefs.clear();
			if (exception != null)
				throw exception;
		}
		
		public synchronized Class<?>[] getRemoteObjectInterfaces()
		{
			return remObjInterfaces.toArray(new Class<?>[remObjInterfaces.size()]);
		}
	}

	/**
	 * RemoteObjectManager TCP-Server
	 */
	private static class RemoteObjectMgrServer extends Thread {
		
		/**
		 * Socket handler thread for incoming client TCP connections 
		 */
		private class ClientConnectionHandler implements Runnable {

			private RemoteConnection remConnection;
			private RemoteObjectController remObjCtrlr = null;
			private ObjectAccessor locObjAccessor = null;
			
			private void performHandshake() throws Exception
			{
				RemoteObjectID remObjID = new RemoteObjectID(remConnection.readString());
				
				DebugUtils.printDebugInfo("Performing handshake for remote object '" + 
					remObjID.toString() +	"' with client '" + remConnection.getRemoteIPAddress() + 
					"' ...", RemoteObjectManager.class, null, getClass(), DebugFlag.REMOTE_OBJECT);
				try {
					remObjCtrlr = getRemObjController(remObjID);
					if (remObjCtrlr == null)
						throw new RemoteObjectException("Unknown remote object '" + 
									  remObjID.toString() + "'!");
					locObjAccessor = remObjCtrlr.getAccessor();
				}
				catch (Exception e) {
					remConnection.writeString("ERROR: " + e.getMessage());
					ExceptionHandler.handle("Failed to perform handshake for remote object '" + 
						remObjID.toString() + "' with client '" + 
						remConnection.getRemoteIPAddress() + "'!", e, RemoteObjectManager.class, 
						null, getClass());
					throw e;
				}
				remConnection.writeString("READY");
				DebugUtils.printDebugInfo("Performing handshake for remote object '" + 
					remObjID.toString() +	"' with client '" + remConnection.getRemoteIPAddress() + 
					"' ... DONE", RemoteObjectManager.class, null, getClass(), 
					DebugFlag.REMOTE_OBJECT);
			}
			
			private void processMethodInvocation(String mthdName) throws Exception
			{
				Object args[] = remConnection.readObjects();
				
				DebugUtils.printDebugInfo("Invoking method '" + mthdName + "(" + 
					(DebugUtils.canDebug(RemoteObjectManager.class, DebugFlag.METHOD_ARGUMENTS) ? 
					Utils.objectsToString(args) : "...") + ")' of remote object '" + 
					locObjAccessor.getRemoteObjectStrID() + "' for client '" + 
					remConnection.getRemoteIPAddress() + "' ...", 
					RemoteObjectManager.class, null, getClass(), DebugFlag.REMOTE_OBJECT);
				try {
					Object res = locObjAccessor.invoke(mthdName, args);
					
					remConnection.writeString("RESULT: ");
					remConnection.writeObject(res);
					if (DebugUtils.canDebug(RemoteObjectManager.class, DebugFlag.METHOD_ARGUMENTS)) 
						DebugUtils.printDebugInfo("Invocation of method '" + mthdName + "(" + 
							Utils.objectsToString(args) + ")' of remote object '" + 
							locObjAccessor.getRemoteObjectStrID() + "' for client '" + 
							remConnection.getRemoteIPAddress() + 
							"' returned '" + Utils.objectsToString(res) + "'", 
							RemoteObjectManager.class, null, getClass(), DebugFlag.REMOTE_OBJECT);
					else 
						DebugUtils.printDebugInfo("Invoking method '" + mthdName + 
							"(...)' of remote object '" + locObjAccessor.getRemoteObjectStrID() + 
							"' for client '" + remConnection.getRemoteIPAddress() + "' ... DONE", 
							RemoteObjectManager.class, null, getClass(), DebugFlag.REMOTE_OBJECT);
				}
				catch (Exception e) {
					remConnection.writeString("ERROR: " + e.getMessage());
					ExceptionHandler.handle("Failed to invoke method '" + mthdName + "(" + 
						(DebugUtils.canDebug(RemoteObjectManager.class, 
						DebugFlag.METHOD_ARGUMENTS) ? Utils.objectsToString(args) : "...") +
						")' of remote object '" + locObjAccessor.getRemoteObjectStrID() +
						"' for client '" + remConnection.getRemoteIPAddress() + "'!", e, 
						RemoteObjectManager.class, null, getClass());
				}
			}
			
			public ClientConnectionHandler(Socket clientSock) throws Exception 
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
								remConnection.getRemoteIPAddress() + "'", RemoteObjectManager.class, 
								null, getClass(), DebugFlag.CONNECTION);
							break;
						}
						else if (cmd.toLowerCase().startsWith("func: ")) 
							processMethodInvocation(cmd.substring(6));
					}
				}
				catch (Exception e) {
					if (serverSock.isClosed())
						DebugUtils.printDebugInfo("Closing connection to client '" + 
							remConnection.getRemoteIPAddress() + 
							"' because of RemoteObjectManager-Server shutdown!",  
							RemoteObjectManager.class, null, getClass());
					else 
						ExceptionHandler.handle("Failed to handle connection to client '" + 
							remConnection.getRemoteIPAddress() + "'!", e, RemoteObjectManager.class, 
							null, getClass());
				}
				finally {
					ServerConnectionListener serverConnListener;
					
					try {
						remConnection.writeString("CLOSED");
					}
					catch (Exception e) {};
					try {
						if (remObjCtrlr != null)
							remObjCtrlr.closeAccessor(locObjAccessor.getRemoteObject());
					}
					catch (Exception e) {}
					if ((serverConnListener = getServerConnectionListener()) != null)
						serverConnListener.closed(remConnection.getRemoteAddress());
					remConnection.close();
					DebugUtils.printDebugInfo("Connection to client '" + 
						remConnection.getRemoteIPAddress() + "' closed", RemoteObjectManager.class, 
						null, getClass(), DebugFlag.CONNECTION);
				}
			}
		}
		
		
		private ServerSocket serverSock;
		private ExecutorService thdPool;
		private List<Utils.Pair<Socket, Future<?>>> pendingClientHandlers = 
			new ArrayList<Utils.Pair<Socket, Future<?>>>();
		
		public RemoteObjectMgrServer(int port, ExecutorService thdPool) throws Exception 
		{
			this.serverSock = new ServerSocket(port);
			this.thdPool = thdPool;
			DebugUtils.printDebugInfo("RemoteObjectManager-Server started - waiting for incoming " +
				"connections on port " + port + "...", RemoteObjectManager.class, null, getClass(), 
				DebugFlag.CONNECTION);
		}
		
		public void run()
		{
			Socket newClientConn;
			ServerConnectionListener serverConnListener;
			
			while (!serverSock.isClosed()) {
				try {
					newClientConn = serverSock.accept();
					serverConnListener = getServerConnectionListener();
					if (serverConnListener != null)
						serverConnListener.established(newClientConn.getInetAddress());
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
								newClientConn, thdPool.submit(new ClientConnectionHandler(
								newClientConn))));
							newClientConn = null;
						}
						i++;
					}
					//Add new client handler
					if (newClientConn != null)
						pendingClientHandlers.add(new Utils.Pair<Socket, Future<?>>(newClientConn, 
							thdPool.submit(new ClientConnectionHandler(newClientConn))));
				} 
				catch (Exception e) {
					if (!serverSock.isClosed()) 
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
				serverSock.close();
				join();
			}
			catch (Exception e) {};
			//Stop all client connections handlers
			for (Utils.Pair<Socket, Future<?>> pendClientHandler: pendingClientHandlers) {
				try {
					pendClientHandler.first.close();
					pendClientHandler.second.get();
				}
				catch (Exception e) {}
			}
			pendingClientHandlers.clear();
			DebugUtils.printDebugInfo("RemoteObjectManager-Server shutdown",	
				RemoteObjectManager.class, null, getClass(), DebugFlag.CONNECTION);
		}
	}

	/**
	 * Exception class for RemoteObjectManager specific exceptions 
	 */
	public static class RemoteObjectManagerException extends Exception {
		
		private static final long serialVersionUID = -9194732425281074132L;
		
		public RemoteObjectManagerException() 
		{
			super();
		}

		public RemoteObjectManagerException(String exception)
		{
			super(exception);
		}
		
		public RemoteObjectManagerException(String exception, Throwable cause)
		{
			super(exception, cause);
		}
	}
	
	/**
	 * Exception class for remote object specific exceptions 
	 */
	public static class RemoteObjectException extends Exception {
		
		private static final long serialVersionUID = 5777065625112497706L;

		public RemoteObjectException() 
		{
			super();
		}
		
		public RemoteObjectException(String exception)
		{
			super(exception);
		}
		
		public RemoteObjectException(String exception, Throwable cause)
		{
			super(exception, cause);
		}
	}

	
	private static Map<RemoteObjectID, RemoteObjectController> registeredRemObjs = 
		new HashMap<RemoteObjectID, RemoteObjectController>();
	private static RemoteObjectMgrServer remObjMgrServer; 
	private static ServerConnectionListener serverConnListener = null;
	
	private static synchronized RemoteObjectController getRemObjController(RemoteObjectID remObjID)
	{
		return registeredRemObjs.get(remObjID);
	}
	
	public static void start(ExecutorService thdPool) throws Exception
	{
		LOCAL_HOST_ADDR = InetAddress.getLocalHost();
		REMOTE_OBJECT_MGR_PORT = CrawlerConfig.getRemoteObjMgrPort();
		DebugUtils.printDebugInfo("RemoteObjectManager started (Port: " + REMOTE_OBJECT_MGR_PORT + 
			")", RemoteObjectManager.class);
		remObjMgrServer = new RemoteObjectMgrServer(REMOTE_OBJECT_MGR_PORT, thdPool);
		remObjMgrServer.start();
	}
	
	public static void shutdown()
	{
		try {
			closeAllRemoteObjects();
		} 
		catch (Exception e) {
			ExceptionHandler.handle("Failed to close remote objects!", e, 
				RemoteObjectManager.class);
		}
		if (remObjMgrServer != null) 		
			remObjMgrServer.shutdown();
		DebugUtils.printDebugInfo("RemoteObjectManager shutdown", RemoteObjectManager.class);
	}
	
	public static synchronized <RemObjInterface> void registerRemoteObject(
		Class<RemObjInterface> remObjInterface, Class<? extends RemObjInterface> remObjClass,
		RemoteObjectCreator<RemObjInterface> remObjCreator, String remObjContext)
	{
		RemoteObjectID remObjID = new RemoteObjectID(remObjClass, remObjContext);
		RemoteObjectController remObjCtrlr = registeredRemObjs.get(remObjID);
		
		if (remObjCtrlr == null)
			registeredRemObjs.put(remObjID, new RemoteObjectController(remObjInterface, 
				remObjClass, remObjCreator, remObjContext));
		else
			remObjCtrlr.addRemoteObjectInterface(remObjInterface);
	}
	
	public static <RemObjInterface> void registerRemoteObject(
		Class<RemObjInterface> remObjInterface, Class<? extends RemObjInterface> remObjClass,
		RemoteObjectCreator<RemObjInterface> remObjCreator)
	{
		registerRemoteObject(remObjInterface, remObjClass, remObjCreator, null);
	}
	
	public static <RemObjInterface> void registerRemoteObject(
		Class<RemObjInterface> remObjInterface, Class<? extends RemObjInterface> remObjClass,
		String remObjContext)
	{
		registerRemoteObject(remObjInterface, remObjClass, null, remObjContext);
	}
	
	public static <RemObjInterface> void registerRemoteObject(
		Class<RemObjInterface> remObjInterface, Class<? extends RemObjInterface> remObjClass)
	{
		registerRemoteObject(remObjInterface, remObjClass, null, null);
	}
	
	@SuppressWarnings("unchecked")
	public static <RemoteObject> RemoteObject getRemoteObject(Class<RemoteObject> remObjClass, 
		InetAddress remAddr, String remObjContext) throws Exception
	{
		RemoteObjectID remObjID = new RemoteObjectID(remObjClass, remObjContext);
		RemoteObjectController remObjCtrlr = getRemObjController(remObjID);
		
		if (remObjCtrlr == null)
			throw new RemoteObjectManagerException("Unknown remote object class '" + 
						  remObjClass.getName() + "' (aka '" + remObjID.toString() + "')!");
		return (RemoteObject)remObjCtrlr.getAccessor(remAddr).getRemoteObject();
	}
	
	public static <RemoteObject> RemoteObject getRemoteObject(Class<RemoteObject> remObjClass, 
		String remObjContext) throws Exception
	{
		return getRemoteObject(remObjClass, LOCAL_HOST_ADDR, remObjContext);
	}
	
	public static <RemoteObject> RemoteObject getRemoteObject(Class<RemoteObject> remObjClass,
		InetAddress remAddr) throws Exception
	{
		return getRemoteObject(remObjClass, remAddr, null);
	}
	
	public static <RemoteObject> RemoteObject getRemoteObject(Class<RemoteObject> remObjClass) 
		throws Exception
	{
		return getRemoteObject(remObjClass, LOCAL_HOST_ADDR, null);
	}
	
	public static void closeRemoteObject(Class<?> remObjClass, Object remObj, String remObjContext) 
		throws Exception
	{
		RemoteObjectController remObjCtrlr = getRemObjController(new RemoteObjectID(remObjClass, 
												 remObjContext));
		
		if (remObjCtrlr != null)
			remObjCtrlr.closeAccessor(remObj);
	}
	
	public static void closeRemoteObject(Class<?> remObjClass, Object remObj) 
		throws Exception
	{
		closeRemoteObject(remObjClass, remObj, null);
	}
	
	public static void closeAllRemoteObjects() throws Exception
	{
		RemoteObjectID[] remObjIDs;
		Exception exception = null;
		
		synchronized (RemoteObjectManager.class) {
			remObjIDs = registeredRemObjs.keySet().toArray(
					        new RemoteObjectID[registeredRemObjs.size()]);
		}
		for (RemoteObjectID remObjID: remObjIDs) {
			try {
				getRemObjController(remObjID).closeAllAccessors();
			}
			catch (Exception e) {
				exception = e;
			}
		}
		if (exception != null)
			throw exception;
	}
	
	public synchronized static ServerConnectionListener getServerConnectionListener()
	{
		return serverConnListener;
	}
	
	public synchronized static void setServerConnectionListener(
		ServerConnectionListener newListener)
	{
		serverConnListener = newListener;
	}
}
