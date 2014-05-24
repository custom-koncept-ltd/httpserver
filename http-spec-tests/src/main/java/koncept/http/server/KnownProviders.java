package koncept.http.server;

import java.util.Arrays;
import java.util.Collection;

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
	
}
