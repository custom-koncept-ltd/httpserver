package koncept.http.server;

import static koncept.http.server.exchange.HttpExchangeImpl.ATTRIBUTE_SCOPE;
import static koncept.http.server.sysfilter.KeepAliveFilter.KEEP_ALIVE;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import koncept.http.server.context.ContextLookupStage;
import koncept.http.server.context.HttpContextHolder;
import koncept.http.server.exchange.ExecSystemFilterStage;
import koncept.http.server.exchange.ExecUserFilterChainStage;
import koncept.http.server.exchange.HttpExchangeImpl;
import koncept.http.server.parse.ParseHeadersStage;
import koncept.http.server.parse.ReadRequestLineStage;
import koncept.http.server.sysfilter.KeepAliveFilter.ConnectionPersistor;
import koncept.io.LineStreamer;
import koncept.io.StreamingSocketAcceptor;
import koncept.io.StreamingSocketAcceptor.SocketClosedException;
import koncept.io.StreamingSocketConnection;
import koncept.sp.ProcSplit;
import koncept.sp.pipe.ProcPipe;
import koncept.sp.pipe.SingleExecutorProcPipe;
import koncept.sp.resource.NonCleanableResource;
import koncept.sp.resource.ProcTerminator;
import koncept.sp.resource.SimpleCloseableResource;
import koncept.sp.resource.SimpleProcPipeCleaner;
import koncept.sp.stage.SplitProcStage;
import koncept.sp.tracker.BlockingJobTracker;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public abstract class ComposableHttpServer extends ConfigurableHttpServer {
	public static final ConfigurationOption SOCKET_TIMEOUT = new ConfigurationOption("socket.SO_TIMEOUT ", "-1", "0", "500", "1000", "30000");
	public static final ConfigurationOption ALLOW_REUSE_SOCKET = new ConfigurationOption("socket.SO_REUSEADDR", "true", "false", "none");
	public static final ConfigurationOption FILTER_ORDER = new ConfigurationOption("server.filter-order", "system-first", "system-last");
	public static final ConfigurationOption EXPECT_100_CONTINUE = new ConfigurationOption("server.expect-100-continue", "true", "false");
	
	protected ExecutorService executor;
	protected ProcPipe<?> processor;
	protected final HttpContextHolder contexts;
	
	private StreamingSocketAcceptor<?,?> socketAcceptor;
	private InetSocketAddress addr;
	private int backlog;

	protected final AtomicBoolean stopRequested = new AtomicBoolean(false);

	protected final Map<ConfigurationOption, String> options = new ConcurrentHashMap<>();

	public ComposableHttpServer() {
		contexts = new HttpContextHolder(getHttpServer());
		options.put(ATTRIBUTE_SCOPE, "");
		options.put(ALLOW_REUSE_SOCKET, "");
		options.put(FILTER_ORDER, "");
		options.put(KEEP_ALIVE, "");
		options.put(EXPECT_100_CONTINUE, "");
		resetOptionsToDefaults();
	}
	
	/**
	 * Open the service listener. Typically some soret of Server Socket, this allows for IO Independence in the server implementation.
	 * @param addr
	 * @param backlog
	 * @return
	 * @throws IOException
	 */
	public abstract StreamingSocketAcceptor openSocket(InetSocketAddress addr, int backlog) throws IOException;
	
	/**
	 * Advises the Server Impl that this connection should be persistent<br>
	 * It is the responsibility of the Server Impl to re-scan for any activity.<br>
	 * @param connection
	 */
	public abstract void keepAlive(StreamingSocketConnection connection);
	
	
	protected void reSubmit(StreamingSocketConnection connection, String requestLine) throws IOException {
		submit(connection, requestLine);
	}
	
	
	
	public HttpServer getHttpServer() {
		return this;
	}
	
	public ProcPipe getProcessor() {
		return processor;
	}
	
	@Override
	public Map<ConfigurationOption, String> options() {
		return Collections.unmodifiableMap(options);
	}
	
	@Override
    public void resetOptionsToDefaults() {
    	ConfigurationOption.set(options, ATTRIBUTE_SCOPE, "exchange");
    	ConfigurationOption.set(options, ALLOW_REUSE_SOCKET, "true");
    	ConfigurationOption.set(options, FILTER_ORDER, "system-first");
    	ConfigurationOption.set(options, EXPECT_100_CONTINUE, "true");
    	ConfigurationOption.set(options, KEEP_ALIVE, "true");
    	
    }
    
    @Override
    public void resetOptionsToJVMStandard() {
    	ConfigurationOption.set(options, ATTRIBUTE_SCOPE, "context");
    	ConfigurationOption.set(options, ALLOW_REUSE_SOCKET, "true");
    	ConfigurationOption.set(options, FILTER_ORDER, "system-last");
    	ConfigurationOption.set(options, EXPECT_100_CONTINUE, "true");
    	ConfigurationOption.set(options, KEEP_ALIVE, "true");
    }
    
    @Override
    public void setOption(ConfigurationOption option, String value) {
    	if (!options.containsKey(option))
    		throw new IllegalArgumentException("Option not understood: " + option);
    	ConfigurationOption.set(options, option, value);
    }
	
	@Override
	public void bind(InetSocketAddress addr, int backlog) throws IOException {
		this.addr = addr;
		this.backlog = backlog;
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
		return addr;
	}

	@Override
	public Executor getExecutor() {
		return executor;
	}
	
	/**
	 * The default (404) error handler (context not found)
	 * @return
	 */
	public HttpHandler getErrorHandler() {
		return contexts.getFailureContext().getHandler();
	}
	
	/**
	 * The default (404) error handler (context not found)
	 * @param errorHandler
	 */
	public void setErrorHandler(HttpHandler errorHandler) {
		if (errorHandler == null) throw new NullPointerException();
		contexts.getFailureContext().setHandler(errorHandler);
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

		try {
			socketAcceptor = openSocket(addr, backlog);
			
			String filterOrder = options.get(FILTER_ORDER);
			boolean systemFirst = filterOrder.equals("system-first");
			
			List<SplitProcStage> stages = new ArrayList<>();
			stages.add(new ReadRequestLineStage());
			stages.add(new ContextLookupStage(contexts));
			stages.add(new ParseHeadersStage(options));
			if (systemFirst) {
				stages.add(new ExecSystemFilterStage(options));
				stages.add(new ExecUserFilterChainStage());
			} else {
				stages.add(new ExecUserFilterChainStage(new ExecSystemFilterStage(options)));
			}
			
			processor = new SingleExecutorProcPipe(
					Logger.getLogger(ComposableHttpServer.class.getName()),
					new BlockingJobTracker(),
					executor,
					stages,
					new RequestFinished(),
					new SimpleProcPipeCleaner());
			executor.execute(new RebindServerSocketAcceptor(socketAcceptor));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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
			processor.stop(true, false, false); //will no longer accept new requests
			if (socketAcceptor != null) {
				socketAcceptor.close();
				socketAcceptor = null;
			}
			if (secondsDelay != 0) {
				long endTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(secondsDelay);
				while (
						System.currentTimeMillis() < endTime && //
						!processor.tracker().live().isEmpty() && 
						!processor.tracker().queued().isEmpty()) {
					Thread.sleep(100);
				}
//				processor.stop(true, true, true); //abort anything else thats still running (!!)
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void submit(StreamingSocketConnection connection, String requestLine) throws IOException {
		ProcSplit split = new ProcSplit();
		split.add("StreamingSocketConnection", new SimpleCloseableResource(connection));
		InputStream in = connection.in();
		OutputStream out = connection.out();
		if (requestLine != null) split.add(ReadRequestLineStage.RequestLine, new NonCleanableResource(requestLine));
		split.add("LineStreamer", new NonCleanableResource(new LineStreamer(in)));
		split.add("in", new NonCleanableResource(in));
		split.add("out", new NonCleanableResource(out));

		processor.submit(split);
	}
	
	private class RebindServerSocketAcceptor implements Runnable {
		private final StreamingSocketAcceptor<?,?> socketAcceptor;
		public RebindServerSocketAcceptor(StreamingSocketAcceptor<?,?> socketAcceptor) {
			this.socketAcceptor = socketAcceptor;
		}
		public void run() {
			try {
				StreamingSocketConnection socketConnection = socketAcceptor.accept();
				while (socketConnection != null && !stopRequested.get()) {
					submit(socketConnection, null);
					socketConnection = socketAcceptor.accept();
				}
				
				if (!stopRequested.get())
					executor.execute(this);
			} catch (SocketClosedException e) {
				//just accept the closing socket
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	private class RequestFinished implements ProcTerminator {
		@Override
		public void clean(ProcSplit finalResult) throws Exception {
			//no special handling here... just unwind
			finalResult.clean();
		}
		@Override
		public Object extractFinalResult(ProcSplit finalResult) {
			if (isKeepAlive(finalResult)) {
				finalResult.removeCleanableResource("HttpExchange");
				//these won't be cleaned... so don't bother
//				finalResult.removeCleanableResource("LineStreamer");
//				finalResult.removeCleanableResource("in");
//				finalResult.removeCleanableResource("out");
				
				keepAlive((StreamingSocketConnection)finalResult.removeCleanableResource("StreamingSocketConnection").get());
			}
			return null;
		}
		private boolean isKeepAlive(ProcSplit finalResult) {
			HttpExchangeImpl exchange = (HttpExchangeImpl)finalResult.getResource("HttpExchange");
			ConnectionPersistor persistor = exchange.getConnectionPersistor();
			return !persistor.isCloseConnection();
		}
	}

}
