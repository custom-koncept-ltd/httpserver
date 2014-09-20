package koncept.http.server.nio2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

import koncept.http.server.Code;
import koncept.http.server.ComposableHttpServer;
import koncept.io.LineStreamer;
import koncept.io.StreamingSocketAcceptor;
import koncept.io.StreamingSocketConnection;
import koncept.nio.StreamedByteChannel;

public class ComposableHttpNIO2Server extends ComposableHttpServer {
	
	private KeepAlive keepAlive;
	
	public ComposableHttpNIO2Server() {
		keepAlive = new KeepAlive();
	}
	
	@Override
	public void start() {
		super.start();
		executor.submit(keepAlive);
	}
	
	@Override
	public StreamingSocketAcceptor openSocket(InetSocketAddress addr, int backlog) throws IOException {
		ServerSocketChannel ssChannel = ServerSocketChannel.open();
		ssChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
		ssChannel.bind(addr, backlog);
		ssChannel.configureBlocking(false);
		return new ServerSocketChannelAcceptor(ssChannel);
	}
	
	@Override
	public void keepAlive(StreamingSocketConnection connection) {
		try {
			keepAlive.add((StreamingSocketConnection)connection);
		} catch (IOException e) {
			//just don't add it (!!) if an excepton occurs
			e.printStackTrace();
		}
	}
	
	@Override
	public int keptAlive() {
		return keepAlive.keepAlives.size();
	}
	
	public static class ServerSocketChannelAcceptor implements StreamingSocketAcceptor<ServerSocketChannel, SocketChannel> {
		private final ServerSocketChannel ssChannel;
		public ServerSocketChannelAcceptor(ServerSocketChannel ssChannel) {
			this.ssChannel = ssChannel;
		}
		
		@Override
		public StreamingSocketConnection<SocketChannel> accept() throws SocketClosedException,
				IOException {
			SocketChannel channel = ssChannel.accept();
			if (channel == null) return null;
			return new SocketChannelConnection(channel);
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
	
	public static class SocketChannelConnection implements StreamingSocketConnection<SocketChannel> {
		private final SocketChannel channel;
		private final StreamedByteChannel streams;
		public SocketChannelConnection(SocketChannel channel) {
			this.channel = channel;
			streams = new StreamedByteChannel(channel);
		}
			
		public SocketChannel underlying() {
			return channel;
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
			channel.close();
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

	class KeepAlive implements Runnable {
		
		Collection<KeepAliveDetails> keepAlives;
		
		public KeepAlive() {
			keepAlives = new CopyOnWriteArraySet<>();
		}
		
		public void add(StreamingSocketConnection channel) throws IOException {
			keepAlives.add(new KeepAliveDetails(channel));
		}
		
		@Override
		public void run() {
			Collection<KeepAliveDetails> toRemove = new ArrayList<>();
			for(KeepAliveDetails details: keepAlives) {
				try {
					String requestLine = details.tryReadLine();
					if(requestLine != null) {
						reSubmit(details.channel, requestLine);
						toRemove.add(details);
					} else if (details.validTill > System.currentTimeMillis()) {
						toRemove.add(details);
					}
				} catch (IOException e) {
					e.printStackTrace();
					toRemove.add(details);
				}
			}

			if (!toRemove.isEmpty()) {
				String newLine = "\r\n".intern();
				int rCode = 408;
				for(KeepAliveDetails details: toRemove) try {
	//				http://en.wikipedia.org/wiki/List_of_HTTP_status_codes#408
					PrintStream p = new PrintStream(details.channel.out());
					p.print("HTTP/1.1 " + rCode + Code.msg(rCode));
					p.print(newLine);
					p.print("Connection: close");
					p.print(newLine);
					p.flush();
					details.channel.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			keepAlives.removeAll(toRemove);
			
			if (!stopRequested.get())
				executor.execute(this);
		}
	}
	
	class KeepAliveDetails {
		final long validTill;
		final StreamingSocketConnection channel;
		final LineStreamer lineStreamer;
		public KeepAliveDetails(StreamingSocketConnection channel) throws IOException {
			this.channel = channel;
			validTill = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1);
			lineStreamer = new LineStreamer(channel.in());
		}
		
		public String tryReadLine() throws IOException {
			return lineStreamer.readLine(0);
		}
	}
	
}
