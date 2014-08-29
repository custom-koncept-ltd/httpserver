package koncept.http.server.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

import koncept.http.server.ComposableHttpServer;
import koncept.http.server.ConfigurationOption;
import koncept.http.server.parse.ReadRequestLineStage;
import koncept.io.LineStreamer;
import koncept.io.StreamingSocketAcceptor;
import koncept.io.StreamingSocketConnection;
import koncept.sp.ProcSplit;
import koncept.sp.resource.NonCleanableResource;
import koncept.sp.resource.SimpleCloseableResource;

public class ComposableHttpIOServer extends ComposableHttpServer {
	
	private final SocketKeepAlive keepAlive;
	
	public ComposableHttpIOServer() {
		super();
		options.put(SOCKET_TIMEOUT, "");
		keepAlive = new SocketKeepAlive(options);
		resetOptionsToDefaults();
	}
	
	@Override
	public void keepAlive(StreamingSocketConnection connection) {
		keepAlive.keepAlive((SocketConnection)connection);
	}
	
	@Override
	public ServerSocketAcceptor openSocket(InetSocketAddress addr,
			int backlog) throws IOException {
		return new ServerSocketAcceptor(new ServerSocket(addr.getPort(), backlog, addr.getAddress()));
	}
	
	
	
	@Override
    public void resetOptionsToDefaults() {
		super.resetOptionsToDefaults();
    	ConfigurationOption.set(options, SOCKET_TIMEOUT, "1000");
    }
    
    @Override
    public void resetOptionsToJVMStandard() {
    	super.resetOptionsToJVMStandard();
    	ConfigurationOption.set(options, SOCKET_TIMEOUT, "-1"); //no timeout (?!?)
    }

	@Override
	public void start() {
		super.start();
		executor.execute(keepAlive);
	}
	
	public class ServerSocketAcceptor implements StreamingSocketAcceptor<ServerSocket, Socket> {
		private final ServerSocket ss;
		public ServerSocketAcceptor(ServerSocket ss) throws IOException {
			this.ss = ss;
		}
		
		@Override
		public StreamingSocketConnection<Socket> accept() throws SocketClosedException, IOException {
			try {
				return new SocketConnection(ss.accept());
			} catch (SocketException e) {
				if (e.getMessage().equals("socket closed")) {
					throw new SocketClosedException(e.getMessage(), e);
				} else throw e;
			}
		}
		
		@Override
		public void close() throws IOException {
			ss.close();
		}
		
		@Override
		public ServerSocket underlying() {
			return ss;
		}

	}
	
	public class SocketConnection implements StreamingSocketConnection<Socket> {
		private final Socket s;
		public SocketConnection(Socket s) throws SocketException {
			this.s = s;
			
			onRebind();
			
		}
		
		public void onRebind() throws SocketException {
			int timeout = new Integer(options.get(SOCKET_TIMEOUT));
			if (timeout != -1)
				s.setSoTimeout(timeout);
		}
		
		@Override
		public Socket underlying() {
			return s;
		}
		
		@Override
		public InetSocketAddress localAddress() {
			return new InetSocketAddress(s.getLocalAddress(), s.getLocalPort());
		}
		
		@Override
		public InetSocketAddress remoteAddress() {
			return new InetSocketAddress(s.getInetAddress(), s.getPort());
		}
		
		@Override
		public void close() throws IOException {
			s.close();
		}
		
		@Override
		public InputStream in() throws IOException {
			return s.getInputStream();
		}
		
		@Override
		public OutputStream out() throws IOException {
			return s.getOutputStream();
		}
	}
	
	/**
	 * This is a hack... need to refactor the stack to allow Connection: Keep-Alive or Connection: Close to be sent with the response headers
	 * @author koncept
	 *
	 */
	public class SocketKeepAlive implements Runnable {
		private boolean MODE_SWITCH_ReadIsAsync = false;
		
		private final Collection<ZombieSocket> collection; //needs to be a concurrent collection
		private final Map<ConfigurationOption, String> options;
		public SocketKeepAlive(Map<ConfigurationOption, String> options) {
			this(new CopyOnWriteArraySet<ZombieSocket>(), options);
		}
		public SocketKeepAlive(Collection<ZombieSocket> collection, Map<ConfigurationOption, String> options) {
			this.collection = collection;
			this.options = options;
		}
		public void keepAlive(SocketConnection s) {
			try {
				if(MODE_SWITCH_ReadIsAsync)
					collection.add(new ZombieSocket(s));
				else 
					execWithSyncRead(new ZombieSocket(s));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		public void run() {
			if (MODE_SWITCH_ReadIsAsync)
				execWithAsyncRead();
		}
		
		public boolean isMODE_SWITCH_ReadIsAsync() {
			return MODE_SWITCH_ReadIsAsync;
		}
		
		public void setMODE_SWITCH_ReadIsAsync(boolean MODE_SWITCH_ReadIsAsync) {
			this.MODE_SWITCH_ReadIsAsync = MODE_SWITCH_ReadIsAsync;
		}
		
		public void execWithAsyncRead() {
			Collection<ZombieSocket> toRemove = new HashSet<>();
			for(ZombieSocket zs: collection) try {
				if (zs.isClosed()) {
					toRemove.add(zs);
				} else {
					String requestLine = zs.readLineIfAvailable();
					if (requestLine != null) {
						toRemove.add(zs);
						reSubmit(zs.connection(), requestLine);
					} else if (zs.count() > 10) {
						toRemove.add(zs);
					}					
				}
			} catch (Exception e) {
				toRemove.add(zs);
			}
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			
			collection.removeAll(toRemove);
			
			if (!stopRequested.get())
				executor.execute(this);
		}
		
		//horrible, hacky, and expensive in terms of threads
		public void execWithSyncRead(final ZombieSocket s) {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					try {
						int timeout = new Integer(options.get(SOCKET_TIMEOUT));
						if (timeout < 0)
							timeout = 2000;
						else
							timeout = Math.min(timeout, 2000);
						String requestLine = s.lines().readLine();
						
						ProcSplit split = new ProcSplit();
						split.add("StreamingSocketConnection", new SimpleCloseableResource(s.connection()));
						split.add("LineStreamer", new NonCleanableResource(s.lines()));
						split.add("in", new NonCleanableResource(s.connection().in()));
						split.add("out", new NonCleanableResource(s.connection().out()));
						
						split.add(ReadRequestLineStage.RequestLine, new NonCleanableResource(requestLine));
						s.connection().onRebind();
						processor.submit(split);
					} catch (IOException e) {
						if (e instanceof SocketException && e.getMessage().equals("Socket Closed")) {
					} else {
							e.printStackTrace();
						}
					}
				}
			});
			
			
		}
	}
	
	private class ZombieSocket {
		final SocketConnection s;
		int count; //times this socket has been 'retried'
		final LineStreamer lines;
		public ZombieSocket(SocketConnection s) throws IOException {
			this.s = s;
			lines = new LineStreamer(s.in());
		}
		public boolean isClosed() {
			if (s.underlying() instanceof Socket)
				return ((Socket)s.underlying()).isClosed(); //not reliable, can return false
			return false; //default false
		}
		public int count() {
			return count;
		}
		public SocketConnection connection() {
			return s;
		}
		public LineStreamer lines() {
			return lines;
		}
		public String readLineIfAvailable() throws IOException {
			count++;
			return lines.readLine(0);
		}
	}
}
