package koncept.http.server;

import static koncept.http.server.exchange.HttpExchangeImpl.ATTRIBUTE_SCOPE;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import koncept.http.server.context.ContextLookupStage;
import koncept.http.server.context.HttpContextHolder;
import koncept.http.server.exchange.ExecSystemFilterStage;
import koncept.http.server.exchange.ExecUserFilterChainStage;
import koncept.http.server.exchange.HttpExchangeImpl;
import koncept.http.server.parse.ParseHeadersStage;
import koncept.http.server.parse.ReadRequestLineStage;
import koncept.io.LineStreamer;
import koncept.sp.ProcSplit;
import koncept.sp.pipe.ProcPipe;
import koncept.sp.pipe.SingleExecutorProcPipe;
import koncept.sp.resource.CleanableResource;
import koncept.sp.resource.ProcTerminator;
import koncept.sp.resource.SimpleCleanableResource;
import koncept.sp.resource.SimpleProcPipeCleaner;
import koncept.sp.stage.SplitProcStage;
import koncept.sp.tracker.BlockingJobTracker;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class ComposableHttpServer extends ConfigurableHttpServer {
	public static final ConfigurationOption SOCKET_TIMEOUT = new ConfigurationOption("socket.SO_TIMEOUT ", "-1", "0", "500", "1000", "30000");
	public static final ConfigurationOption ALLOW_REUSE_SOCKET = new ConfigurationOption("socket.SO_REUSEADDR", "true", "false", "none");
	public static final ConfigurationOption FILTER_ORDER = new ConfigurationOption("server.filter-order", "system-first", "system-last");
	public static final ConfigurationOption KEEP_ALIVE = new ConfigurationOption("server.keep-alive", "true", "false");
	public static final ConfigurationOption EXPECT_100_CONTINUE = new ConfigurationOption("server.expect-100-continue", "true", "false");
	
	private ExecutorService executor;
	private ProcPipe processor;
	private final HttpContextHolder contexts;
	private final SocketKeepAlive keepAlive;
	
	
	private InetSocketAddress addr;
	private int backlog;
	
	private ServerSocket serverSocket;

	private final AtomicBoolean stopRequested = new AtomicBoolean(false);

	private final Map<ConfigurationOption, String> options = new ConcurrentHashMap<>();

	public ComposableHttpServer() {
		contexts = new HttpContextHolder(getHttpServer());
		keepAlive = new SocketKeepAlive(options);
		options.put(ATTRIBUTE_SCOPE, "");
		options.put(SOCKET_TIMEOUT, "");
		options.put(ALLOW_REUSE_SOCKET, "");
		options.put(FILTER_ORDER, "");
		options.put(KEEP_ALIVE, "");
		options.put(EXPECT_100_CONTINUE, "");
		resetOptionsToDefaults();
	}
	
	public HttpServer getHttpServer() {
		return this;
	}
	
	public ServerSocket openSocket(InetSocketAddress addr, int backlog) throws IOException {
		ServerSocket ss = new ServerSocket(addr.getPort(), backlog, addr.getAddress());
		return ss;
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
    	ConfigurationOption.set(options, SOCKET_TIMEOUT, "1000");
    	ConfigurationOption.set(options, ALLOW_REUSE_SOCKET, "true");
    	ConfigurationOption.set(options, FILTER_ORDER, "system-first");
    	ConfigurationOption.set(options, KEEP_ALIVE, "true");
    	ConfigurationOption.set(options, EXPECT_100_CONTINUE, "true");
    }
    
    @Override
    public void resetOptionsToJVMStandard() {
    	ConfigurationOption.set(options, ATTRIBUTE_SCOPE, "context");
    	ConfigurationOption.set(options, SOCKET_TIMEOUT, "1000");
    	ConfigurationOption.set(options, ALLOW_REUSE_SOCKET, "true");
    	ConfigurationOption.set(options, FILTER_ORDER, "system-last");
    	ConfigurationOption.set(options, KEEP_ALIVE, "true");
    	ConfigurationOption.set(options, EXPECT_100_CONTINUE, "true");
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
			serverSocket = openSocket(addr, backlog);
			String allowResuse = options.get(ALLOW_REUSE_SOCKET);
			if (allowResuse.equalsIgnoreCase("true"))
				serverSocket.setReuseAddress(true);
			else if (allowResuse.equalsIgnoreCase("false"))
				serverSocket.setReuseAddress(false);
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

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
				ComposableHttpServer.class.getName(),
				new BlockingJobTracker(),
				executor,
				stages,
				new KeepAliveProcTerminators(),
				new SimpleProcPipeCleaner());
		executor.execute(new RebindServerSocketAcceptor(serverSocket));
		executor.execute(keepAlive);
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
			if (serverSocket != null) {
				serverSocket.close();
				serverSocket = null;
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
	
	private class RebindServerSocketAcceptor implements Runnable {
		private final ServerSocket ss;
		public RebindServerSocketAcceptor(ServerSocket ss) {
			this.ss = ss;
		}
		public void run() {
			try {
				Socket s = ss.accept();
				int timeout = new Integer(options.get(SOCKET_TIMEOUT));
				if (timeout != -1)
					s.setSoTimeout(timeout);
				
				
				ProcSplit split = new ProcSplit();
				split.add("Socket", new ClosableResource(s));
				split.add("LineStreamer", new SimpleCleanableResource(new LineStreamer(s.getInputStream()), null));
				split.add("in", new SimpleCleanableResource(s.getInputStream(), null));
				split.add("out", new SimpleCleanableResource(s.getOutputStream(), null));

				
				processor.submit(split);
				if (!stopRequested.get())
					executor.execute(this);
			} catch (IOException e) {
				if (ss.isClosed()) return; //socket closed... not actually an error
				throw new RuntimeException(e);
			}
		}
	}
	
	private class ClosableResource implements CleanableResource {
		private final Closeable c;
		public ClosableResource(Closeable c) {
			this.c = c;
		}
		public void clean() {
			try {
				if (c instanceof Flushable)
					((Flushable)c).flush();
				c.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		public Object get() {
			return c;
		}
	}
	
	private class SocketKeepAlive implements Runnable {
		
		public final boolean MODE_SWITCH_ReadIsAsync = false;
		
		private final Collection<ZombieSocket> collection; //needs to be a concurrent collection
		private final Map<ConfigurationOption, String> options;
		public SocketKeepAlive(Map<ConfigurationOption, String> options) {
			this(new CopyOnWriteArraySet<ZombieSocket>(), options);
		}
		public SocketKeepAlive(Collection<ZombieSocket> collection, Map<ConfigurationOption, String> options) {
			this.collection = collection;
			this.options = options;
		}
		public boolean isKeepAlive() {
			return Boolean.parseBoolean(options.get(KEEP_ALIVE));
		}
		public void keepAlive(Socket s) {
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
		
		public void execWithAsyncRead() {
			Collection<ZombieSocket> toRemove = new HashSet<>();
			Collection<ProcSplit> toProcess = new HashSet<>();
			for(ZombieSocket zs: collection) try {
				if (zs.isClosed()) {
					toRemove.add(zs);
				} else {
					String requestLine = zs.readLineIfAvailable();
					if (requestLine != null) {
						toRemove.add(zs);
						
						ProcSplit split = new ProcSplit();
						Socket s = zs.socket();
						split.add("Socket", new ClosableResource(s));
						split.add("LineStreamer", new SimpleCleanableResource(zs.lines(), null));
						split.add("in", new ClosableResource(s.getInputStream()));
						split.add("out", new ClosableResource(s.getOutputStream()));
						split.add(ReadRequestLineStage.RequestLine, new SimpleCleanableResource(requestLine, null));
						toProcess.add(split);
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
			for(ProcSplit in: toProcess) {
				processor.submit(in);
			}
			
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
						s.socket().setSoTimeout(timeout);
						String requestLine = s.lines().readLine();
						
						ProcSplit split = new ProcSplit();
						split.add("Socket", new ClosableResource(s.socket()));
						split.add("LineStreamer", new SimpleCleanableResource(s.lines(), null));
						split.add("in", new ClosableResource(s.socket().getInputStream()));
						split.add("out", new ClosableResource(s.socket().getOutputStream()));
						
						timeout = new Integer(options.get(SOCKET_TIMEOUT));
						if (timeout != -1)
							s.socket().setSoTimeout(timeout);
						
						split.add(ReadRequestLineStage.RequestLine, new SimpleCleanableResource(requestLine, null));
						processor.submit(split);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
			
			
		}
	}
	
	private class ZombieSocket {
		final Socket s;
		int count; //times this socket has been 'retried'
		final LineStreamer lines;
		public ZombieSocket(Socket s) throws IOException {
			this.s = s;
			lines = new LineStreamer(s.getInputStream());
		}
		public boolean isClosed() {
			return s.isClosed();
		}
		public int count() {
			return count;
		}
		public Socket socket() {
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
	
	
	private class KeepAliveProcTerminators<T> implements ProcTerminator<T> {
		public boolean connectionEligibleForKeepAlive(ProcSplit finalResult) {
			HttpExchangeImpl exchange = (HttpExchangeImpl)finalResult.getResource("HttpExchange");
			if (exchange == null) return false;
			if (exchange.getProtocol()== null) return false;
			String connecteader = exchange.getRequestHeaders().getFirst("Connection");
			if (exchange.getProtocol().equalsIgnoreCase("HTTP/1.1")) {
				if (connecteader != null && connecteader.equalsIgnoreCase("close")) return false;
				return true;
			} else if (exchange.getProtocol().equalsIgnoreCase("HTTP/1.0")) {
				if (connecteader != null && connecteader.equalsIgnoreCase("Keep-Alive")) return true;
			}
			return false;
		}
		@Override
		public void clean(ProcSplit finalResult) {
			
			finalResult.clean();
		}
		@Override
		public T extractFinalResult(ProcSplit finalResult) {
			if (keepAlive.isKeepAlive() && connectionEligibleForKeepAlive(finalResult)) {
				CleanableResource socketResource = finalResult.removeCleanableResource("Socket");
				finalResult.removeCleanableResource("in"); //also remove the IO streams
				finalResult.removeCleanableResource("out");
				Socket s = (Socket)socketResource.get();
				if (!s.isClosed())
					keepAlive.keepAlive(s);
			}
			return null;
		}
	}

}
