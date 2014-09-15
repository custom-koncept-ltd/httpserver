package koncept.http.server.sysfilter;

import java.io.IOException;
import java.util.Map;

import koncept.http.server.ConfigurationOption;
import koncept.http.server.exchange.HttpExchangeImpl;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

public class KeepAliveFilter extends Filter {
	public static final ConfigurationOption KEEP_ALIVE = new ConfigurationOption("server.keep-alive", "true", "false");
	public static final String CONNECTION_HEADER = "Connection";
	
	private final Map<ConfigurationOption, String> options;
	public KeepAliveFilter(Map<ConfigurationOption, String> options) {
		this.options = options;
	}
	
	@Override
	public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
		HttpExchangeImpl exchangeImpl = (HttpExchangeImpl)exchange;
		
		String connectionHeader = exchangeImpl.getRequestHeaders().getFirst(CONNECTION_HEADER);
		String httpProtocol = exchangeImpl.getProtocol();
		Boolean keepAlive = isKeepAlive(httpProtocol, connectionHeader);
		if (keepAlive == null) exchangeImpl.setConnectionPersistor(null);
		else if (keepAlive) exchangeImpl.setConnectionPersistor(new KeepAlive());
		else exchangeImpl.setConnectionPersistor(new Close());
		chain.doFilter(exchange);
	}
	
	private Boolean isKeepAlive(String protocol, String connectionHeader) {
		String keepAliveEnabled = options.get(KEEP_ALIVE);
		if (keepAliveEnabled == null) return null;
		if (!Boolean.valueOf(keepAliveEnabled)) return false;
		
		if (protocol.equalsIgnoreCase("HTTP/1.1")) {
			if (connectionHeader != null && connectionHeader.equalsIgnoreCase("Close")) return false;
			return true;
		} else if (protocol.equalsIgnoreCase("HTTP/1.0")) {
			if (connectionHeader != null && connectionHeader.equalsIgnoreCase("Keep-Alive")) return true;
			return false;
		}
		return null;
	}

	@Override
	public String description() {
		return "KeepAliveFilter";
	}

	public static interface ConnectionPersistor {
		public void onResponse(HttpExchangeImpl exchange);
		public boolean isCloseConnection();
	}
	
	public static class Close implements ConnectionPersistor {
		@Override
		public void onResponse(HttpExchangeImpl exchange) {
			exchange.getResponseHeaders().set(CONNECTION_HEADER, "Close");
		}
		@Override
		public boolean isCloseConnection() {
			return true;
		}
	}
	
	public static class KeepAlive implements ConnectionPersistor {
		boolean closeConnection = false;
		@Override
		public void onResponse(HttpExchangeImpl exchange) {
			String keepAliveHeader = exchange.getResponseHeaders().getFirst(CONNECTION_HEADER);
			if (keepAliveHeader != null && keepAliveHeader.equalsIgnoreCase("Close")) {
				closeConnection = true; //if the connection has been explicitly closed by the user... respect it
			} else {
				exchange.getResponseHeaders().set(CONNECTION_HEADER, "Keep-Alive");
			}
			//if a response header of close was set... close the connection (!!)
		}
		
		@Override
		public boolean isCloseConnection() {
			return closeConnection;
		}
	}
	
}
