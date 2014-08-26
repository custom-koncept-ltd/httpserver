package koncept.http.server.nio2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import koncept.http.server.ComposableHttpServer;
import koncept.io.StreamingSocketAcceptor;
import koncept.io.StreamingSocketConnection;
import koncept.nio2.StreamedByteChannel;
import koncept.sp.resource.ProcTerminator;
import koncept.sp.resource.SimpleProcTerminator;

public class ComposableHttpNIO2Server extends ComposableHttpServer {
	
	
	@Override
	public ProcTerminator getTerminator() {
		return new SimpleProcTerminator(); //TODO... keepalive.... ?
	}
	
	@Override
	public StreamingSocketAcceptor openSocket(InetSocketAddress addr, int backlog) throws IOException {
		ServerSocketChannel ssChannel = ServerSocketChannel.open();
		ssChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
		ssChannel.bind(addr, backlog);
		ssChannel.configureBlocking(false);
		return new ServerSocketChannelAcceptor(ssChannel);
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
		
		@Override
		public SocketChannel underlying() {
			return channel;
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

}
