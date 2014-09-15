package koncept.http.server.nio2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;

import koncept.http.server.ConfigurableHttpsServer;
import koncept.http.server.ConfigurationOption;
import koncept.io.StreamingSocketAcceptor;
import koncept.io.StreamingSocketConnection;
import koncept.nio.StreamedByteChannel;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

public class ComposableHttpsNIO2Server extends ConfigurableHttpsServer {
	private HttpsConfigurator configurator = null;
	
	private ComposableHttpServerWrapper wrapped;
	
	public ComposableHttpsNIO2Server() {
		wrapped = new ComposableHttpServerWrapper();
	}
	
	public HttpsServer getHttpsServer() {
		return this;
	}
	
	public ComposableHttpNIO2Server getWrapped() {
		return wrapped;
	}
	
	@Override
	public void setHttpsConfigurator(HttpsConfigurator configurator) {
		if (configurator == null) {
            throw new NullPointerException ("null HttpsConfigurator");
        }
//        if (started) {
//            throw new IllegalStateException ("server already started");
//        }
        this.configurator = configurator;
	}
	
	@Override
	public HttpsConfigurator getHttpsConfigurator() {
		return configurator;
	}
	
	private class ComposableHttpServerWrapper extends ComposableHttpNIO2Server {
		@Override
		public HttpServer getHttpServer() {
			return getHttpsServer();
		}
		
		@Override
		public StreamingSocketAcceptor openSocket(InetSocketAddress addr,
				int backlog) throws IOException {
			ServerSocketChannel ssChannel = ServerSocketChannel.open();
			ssChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
			ssChannel.bind(addr, backlog);
			ssChannel.configureBlocking(false);
			
			SSLContext ssl = configurator.getSSLContext();
			return new SSLServerSocketChannelAcceptor(ssChannel, ssl);
		}
		
	}
	
	@Override
	public void resetOptionsToJVMStandard() {
		wrapped.resetOptionsToJVMStandard();
	}

	@Override
	public void resetOptionsToDefaults() {
		wrapped.resetOptionsToDefaults();
	}

	@Override
	public Map<ConfigurationOption, String> options() {
		return wrapped.options();
	}
	
	@Override
	public void setOption(ConfigurationOption option, String value) {
		wrapped.setOption(option, value);
	}
	
	@Override
	public void bind(InetSocketAddress addr, int backlog) throws IOException {
		wrapped.bind(addr, backlog);
	}

	@Override
	public void start() {
		wrapped.start();
	}

	@Override
	public void setExecutor(Executor executor) {
		wrapped.setExecutor(executor);
	}

	@Override
	public Executor getExecutor() {
		return wrapped.getExecutor();
	}

	@Override
	public void stop(int secondsDelay) {
		wrapped.stop(secondsDelay);
	}

	@Override
	public HttpContext createContext(String path, HttpHandler handler) {
		return wrapped.createContext(path, handler);
	}

	@Override
	public HttpContext createContext(String path) {
		return wrapped.createContext(path);
	}

	@Override
	public void removeContext(String path) throws IllegalArgumentException {
		wrapped.removeContext(path);
	}

	@Override
	public void removeContext(HttpContext context) {
		wrapped.removeContext(context);
	}

	@Override
	public InetSocketAddress getAddress() {
		return wrapped.getAddress();
	}
	
	public static class SSLServerSocketChannelAcceptor implements StreamingSocketAcceptor<ServerSocketChannel, SocketChannel> {
		private final ServerSocketChannel ssChannel;
		private final SSLContext ssl;
		public SSLServerSocketChannelAcceptor(ServerSocketChannel ssChannel, SSLContext ssl) {
			this.ssChannel = ssChannel;
			this.ssl = ssl;
		}
		
		@Override
		public StreamingSocketConnection<SocketChannel> accept() throws SocketClosedException,
				IOException {
			SocketChannel channel = ssChannel.accept();
			if (channel == null) return null;
			return new SSLSocketChannelConnection(channel, ssl.createSSLEngine());
		}
		
		@Override
		public void close() throws IOException {
			ssChannel.close();
		}
		
		@Override
		public ServerSocketChannel underlying() {
			return ssChannel;
		}
	}
	
	public static class SSLSocketChannelConnection implements StreamingSocketConnection<SocketChannel> {
		private final SocketChannel channel;
		private final StreamedByteChannel streams;
		public SSLSocketChannelConnection(SocketChannel channel, SSLEngine ssl) throws SSLException, IOException {
			this.channel = channel;
			channel.configureBlocking(false);
			ssl.setEnabledCipherSuites(ssl.getSupportedCipherSuites());
			ssl.setUseClientMode(false);
			ssl.setNeedClientAuth(true);
			SSLByteChannel sslChan = new SSLByteChannel(channel, ssl);
			sslChan.handshake();
			streams = new StreamedByteChannel(sslChan);
		}
		
		@Override
		public InetSocketAddress localAddress() {
			try {
				return (InetSocketAddress)channel.getLocalAddress();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		@Override
		public InetSocketAddress remoteAddress() {
			try {
				return (InetSocketAddress)channel.getRemoteAddress();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		@Override
		public void close() throws IOException {
			streams.close();
		}
		
		@Override
		public InputStream in() throws IOException {
			return streams.getIn();
		}
		
		@Override
		public OutputStream out() throws IOException {
			return streams.getOut();
		}
	}
	
	static class SSLByteChannel implements ByteChannel {
		private static final ByteBuffer emptyBuffer = ByteBuffer.allocate(0);
		private final SSLEngine engine;
		private final ByteChannel chan;
		
		//'network side' buffers - app side will be passed into read/write methods
		private final ByteBuffer in;
		private final ByteBuffer out;
		
		public SSLByteChannel(ByteChannel chan, SSLEngine engine) {
			this.chan = chan;
			this.engine = engine;
			in = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());
			in.flip();
			out = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());
		}
		
		public void handshake() throws SSLException {
			engine.beginHandshake();
		}
		
		public void handshakeIfRequired() throws IOException {
			HandshakeStatus hs = engine.getHandshakeStatus();
			int maxTimes = 10;
			while(hs != null && maxTimes-- > 0) {
				switch(hs) {
				case FINISHED:
				case NOT_HANDSHAKING:
					return;
				case NEED_TASK:
					engine.getDelegatedTask().run();
					break;
				case NEED_UNWRAP:
					unwrap(emptyBuffer);
					break;
				case NEED_WRAP:
					wrap(emptyBuffer);
					break;
				default:
					throw new IllegalStateException("Unknown status: " + hs);
				}
				hs = engine.getHandshakeStatus();
			}
		}
		
		private int wrap(ByteBuffer src) throws IOException {
			SSLEngineResult res = engine.wrap(src, out);
			flush();
			switch(res.getStatus()) {
			case BUFFER_OVERFLOW: //not enough space in netdata.
				return res.bytesProduced();
			case BUFFER_UNDERFLOW: //not enough app data... who cares (!!)
				return res.bytesProduced();
			case CLOSED:
			case OK:
				return 0;
			default:
				throw new IllegalStateException("Unknown status: " + res.getStatus());
			}
		}
		
		private int unwrap(ByteBuffer dst) throws IOException {
			pull();
			SSLEngineResult res = engine.unwrap(in, dst);
			switch(res.getStatus()) {
			case BUFFER_OVERFLOW:
				return res.bytesProduced();
			case BUFFER_UNDERFLOW:
				return res.bytesProduced();
			case CLOSED:
			case OK:
				return 0;
			default:
				throw new IllegalStateException("Unknown status: " + res.getStatus());
			}
		}
		
		public void flush() throws IOException {
			out.flip();
			while(out.hasRemaining()) {
			    int written = chan.write(out);
			    if (written == 0) break;
			    if (out.hasRemaining()) try {
			    	Thread.sleep(1);
			    } catch (InterruptedException e) {
			    	e.printStackTrace();
			    }
			}
			out.compact();
		}
		
		private int pull() throws IOException {
			in.compact();
			int read =  chan.read(in);
			in.flip();
			return read;
		}
			
		@Override
		public void close() throws IOException {
			flush();
			engine.closeOutbound();
//			engine.closeInbound();
//			flush();
			chan.close();
		}
		
		@Override
		public boolean isOpen() {
			return chan.isOpen();
		}
		
		@Override
		public int read(ByteBuffer dst) throws IOException {
			handshakeIfRequired();
			return unwrap(dst);
		}
		
		@Override
		public int write(ByteBuffer src) throws IOException {
			handshakeIfRequired();
			return wrap(src);
		}
	}
}
