package koncept.http.server.io;

import static koncept.http.server.sysfilter.KeepAliveFilter.KEEP_ALIVE;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import koncept.http.server.ComposableHttpServer;
import koncept.http.server.ConfigurationOption;
import koncept.io.StreamingSocketAcceptor;
import koncept.io.StreamingSocketConnection;

public class ComposableHttpIOServer extends ComposableHttpServer {
	
	
	public ComposableHttpIOServer() {
		super();
		options.put(SOCKET_TIMEOUT, "");
		resetOptionsToDefaults();
	}
	
	@Override
	public void keepAlive(StreamingSocketConnection connection) {
		
	}
	
	@Override
	public int keptAlive() {
		return 0;
	}
	
	@Override
	public ServerSocketAcceptor openSocket(InetSocketAddress addr,
			int backlog) throws IOException {
		return new ServerSocketAcceptor(new ServerSocket(addr.getPort(), backlog, addr.getAddress()));
	}
	
	
	@Override
    public void resetOptionsToDefaults() {
		super.resetOptionsToDefaults();
		ConfigurationOption.set(options, KEEP_ALIVE, "false");
    	ConfigurationOption.set(options, SOCKET_TIMEOUT, "1000");
    }
    
    @Override
    public void resetOptionsToJVMStandard() {
    	super.resetOptionsToJVMStandard();
    	ConfigurationOption.set(options, KEEP_ALIVE, "false");
    	ConfigurationOption.set(options, SOCKET_TIMEOUT, "-1"); //no timeout (?!?)
    }
    
    @Override
    public void setOption(ConfigurationOption option, String value) {
    	if (KEEP_ALIVE.equals(option) && Boolean.valueOf(value))
    		throw new IllegalArgumentException("Cannot enable Keep-Alive in a java.io based server");
    	super.setOption(option, value);
    }

	@Override
	public void start() {
		super.start();
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
}
