package koncept.http.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class KnownProviders {

	/**
	 * 
	 * @return the providers, all ready in JUnit format
	 */
	public static Collection<Object[]> providers() {
		return Arrays.asList(new Object[][]{
				{new sun.net.httpserver.DefaultHttpServerProvider()},
				{new koncept.http.LegacyModHttpServerProvider()},
				{new koncept.http.KonceptHttpServerProvider()}
		});
	}
	
	/**
	 * 
	 * @return filtered providers, in JUnit format
	 */
	public static Collection<Object[]> configurableProviders() {
		List<Object[]> configurableProviders = new ArrayList<>();
		for(Object[] provider: providers()) {
			if (provider[0] instanceof ConfigurableHttpServerProvider)
				configurableProviders.add(provider);
		}
		return configurableProviders;
	}
	
}
