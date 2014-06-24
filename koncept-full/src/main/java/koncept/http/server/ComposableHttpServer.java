package koncept.http.server;

import static koncept.http.server.exchange.HttpExchangeImpl.ATTRIBUTE_SCOPE;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import koncept.http.io.StreamsWrapper;
import koncept.http.server.context.ContextLookupStage;
import koncept.http.server.context.HttpContextHolder;
import koncept.http.server.exchange.ExecFilterStage;
import koncept.http.server.exchange.ExecHandlerStage;
import koncept.http.server.parse.ParseHeadersStage;
import koncept.http.server.parse.ReadRequestLineStage;
import koncept.sp.ProcSplit;
import koncept.sp.pipe.ProcPipe;
import koncept.sp.pipe.SingleExecutorProcPipe;
import koncept.sp.resource.CleanableResource;
import koncept.sp.resource.SimpleCleanableResource;
import koncept.sp.resource.SimpleProcTerminator;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class ComposableHttpServer extends ConfigurableHttpServer implements StreamsWrapper.SocketWrapper {
	public static final ConfigurationOption SOCKET_TIMEOUT = new ConfigurationOption("socket.timeout", "-1", "0", "500", "1000", "30000");
	private ExecutorService executor;
	private ProcPipe processor;
	private final HttpContextHolder contexts;
	private ServerSocket serverSocket;

	private final AtomicBoolean stopRequested = new AtomicBoolean(false);

	private Map<ConfigurationOption, String> options = new ConcurrentHashMap<>();

	public ComposableHttpServer() {
		contexts = new HttpContextHolder(getHttpServer());
		options.put(ATTRIBUTE_SCOPE, "");
		options.put(SOCKET_TIMEOUT, "");
		resetOptionsToDefaults();
	}
	
	public HttpServer getHttpServer() {
		return this;
	}
	
	public ProcPipe getProcessor() {
		return processor;
	}
	
	@Override
	public Map<ConfigurationOption, String> options() {
		return options;
	}
	
	@Override
    public void resetOptionsToDefaults() {
    	ConfigurationOption.set(options, ATTRIBUTE_SCOPE, "exchange");
    	ConfigurationOption.set(options, SOCKET_TIMEOUT, "500");
    }
    
    @Override
    public void resetOptionsToJVMStandard() {
    	ConfigurationOption.set(options, ATTRIBUTE_SCOPE, "context");
    	ConfigurationOption.set(options, SOCKET_TIMEOUT, "500");
    }
	
	@Override
	public void bind(InetSocketAddress addr, int backlog) throws IOException {
		// TODO: check for running and rebindind
		serverSocket = new ServerSocket(addr.getPort(), backlog, addr.getAddress());
	}

	@Override
	public HttpContext createContext(String path) {
		return contexts.createContext(path, null);
	}

	@Override
	public HttpContext createContext(String path, HttpHandler handler) {
		return contexts.createContext(path, handler);
	}

	@Override
	public InetSocketAddress getAddress() {
		return new InetSocketAddress(serverSocket.getInetAddress(), serverSocket.getLocalPort());
	}

	@Override
	public Executor getExecutor() {
		return executor;
	}

	@Override
	public void removeContext(HttpContext context) {
		contexts.removeContext(context);
	}

	@Override
	public void removeContext(String path) throws IllegalArgumentException {
		contexts.removeContext(path);
	}

	@Override
	public void setExecutor(Executor executor) {
		if (executor == null) throw new NullPointerException();
		if (!(executor instanceof ExecutorService))
			throw new RuntimeException("A full Executor Service is required");
		
		// TODO - disallow change when server is running
		this.executor = (ExecutorService)executor;
	}

	@Override
	public void start() {
		if (executor == null)
			throw new RuntimeException("No Executor to run in");

		if (serverSocket == null)
			throw new RuntimeException("Not bound to an address");

		processor = new SingleExecutorProcPipe(executor, Arrays.asList(
				new ReadRequestLineStage(),
				new ContextLookupStage(contexts),
				new ParseHeadersStage(options),
				new ExecFilterStage(),
				new ExecHandlerStage()
		),
		new SimpleProcTerminator(null));

		executor.execute(new RebindServerSocketAcceptor(serverSocket));
	}

	/*
	 * public abstract void stop(int delay)
	 * 
	 * stops this server by closing the listening socket and disallowing any new
	 * exchanges from being processed. The method will then block until all
	 * current exchange handlers have completed or else when approximately delay
	 * seconds have elapsed (whichever happens sooner). Then, all open TCP
	 * connections are closed, the background thread created by start() exits,
	 * and the method returns. Once stopped, a HttpServer cannot be re-used.
	 * 
	 * Parameters: delay - the maximum time in seconds to wait until exchanges
	 * have finished. Throws: IllegalArgumentException - if delay is less than
	 * zero.
	 */
	@Override
	public void stop(int secondsDelay) {
		if (secondsDelay < 0)
			throw new IllegalArgumentException(); // -ve numbers are invalid
		try {
			stopRequested.set(true);
			serverSocket.close();
			processor.stop(true, false, false); //will no longer accept new requests
			if (secondsDelay != 0) {
				long endTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(secondsDelay);
				while (
						System.currentTimeMillis() < endTime && //
						!processor.tracker().live().isEmpty() && 
						!processor.tracker().queued().isEmpty()) {
					Thread.sleep(100);
				}
				processor.stop(true, true, true); //abort anything else thats still running (!!)
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public StreamsWrapper wrap(Socket s) throws IOException {
		return new StreamsWrapper.SimpleWrapper(s.getInputStream(), s.getOutputStream());
	}
	
	private class RebindServerSocketAcceptor implements Runnable {
		private final ServerSocket ss;
		public RebindServerSocketAcceptor(ServerSocket ss) {
			this.ss = ss;
		}
		public void run() {
			try {
				Socket s = ss.accept();
				Integer timeout = new Integer(options.get(SOCKET_TIMEOUT));
				if (timeout != -1)
					s.setSoTimeout(timeout);
				
				StreamsWrapper streams = wrap(s);
				
				ProcSplit split = new ProcSplit();
				split.add("in", new SimpleCleanableResource(streams.getIn(), null));
				split.add("out", new SimpleCleanableResource(streams.getOut(), null));
				split.add("StreamsWrapper", new SimpleCleanableResource(streams, null));
				split.add("Socket", new SocketResource(s));
				
				processor.submit(split);
				if (!stopRequested.get())
					executor.execute(this);
			} catch (IOException e) {
				if (ss.isClosed()) return; //socket closed... not actually an error
				throw new RuntimeException(e);
			}
		}
	}
	
	private class SocketResource implements CleanableResource {
		private final Socket s;
		public SocketResource(Socket s) {
			this.s = s;
		}
		public void clean() {
			try {
				if (!s.isClosed()) {
					s.getOutputStream().flush();
					s.close();
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		public Object get() {
			return s;
		}
	}

}
