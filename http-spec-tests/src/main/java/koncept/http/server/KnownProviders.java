package koncept.http.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.sun.net.httpserver.spi.HttpServerProvider;

public class KnownProviders {

	/**
	 * 
	 * @return the providers, all ready in JUnit format
	 */
	public static List<HttpServerProvider> providers() {
		return Arrays.asList(
				new sun.net.httpserver.DefaultHttpServerProvider(),
				new koncept.http.LegacyModHttpServerProvider(),
				new koncept.http.KonceptHttpServerProvider()
		);
	}
	
	public static List<Boolean> withHttps(Boolean b) {
		if (b != null) return Arrays.asList(b);
		return Arrays.asList(false, true);
	}
	
	/**
	 * 
	 * @return filtered providers, in JUnit format
	 */
	public static List<ConfigurableHttpServerProvider> configurableProviders() {
		return configurableProviders(providers());
	}
	
	public static List<ConfigurableHttpServerProvider> configurableProviders(Collection<HttpServerProvider> providers) {
		List<ConfigurableHttpServerProvider> configurableProviders = new ArrayList<>();
		for(HttpServerProvider provider: providers) {
			if (provider instanceof ConfigurableHttpServerProvider)
				configurableProviders.add((ConfigurableHttpServerProvider)provider);
		}
		return configurableProviders;
	}
	

	public static Collection<Object[]> junitJoin(Collection<? extends Object>... allJunitParams) {
		if (allJunitParams.length == 0) throw new IllegalArgumentException();		
		return junitJoin(new ArrayList(), allJunitParams[0], tail(allJunitParams));
	}
	
	private static Collection<? extends Object>[] tail(Collection<? extends Object>... allJunitParams) {
		if (allJunitParams.length == 0) return null;
		Collection<? extends Object>[] tail = new Collection[allJunitParams.length - 1];
		System.arraycopy(allJunitParams, 1, tail, 0, tail.length);
		return tail;
	}
	private static Collection<? extends Object> head(Collection<? extends Object>... allJunitParams) {
		if (allJunitParams.length == 0) return null;
		return allJunitParams[0];
	}
	private static Collection<Object[]> junitJoin(List currentRow, Collection head, Collection... tail) {
		Collection<Object[]> joined = new ArrayList<>();
		if (head != null) {
			for(Object junitParam: head) {
				List row = new ArrayList<>(currentRow);
				row.add(junitParam);
				joined.addAll(junitJoin(row, head(tail), tail(tail)));
			}
		} else {
			joined.add(currentRow.toArray());
		}
		return joined;
	}
	
}
