package koncept.http.server;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.spi.HttpServerProvider;

public class AuthenticationDefaultSpecTest extends DefaultSpecHttpServerTestParameteriser {
	
	public AuthenticationDefaultSpecTest(HttpServerProvider provider) {
		super(provider);
	}

	@Test
	public void basicFilterApplication() throws IOException {
		RecordingHandler handler = new RecordingHandler();
		RecordingFilter filter = new RecordingFilter();
		HttpContext context = server.createContext("/", handler);
		context.getFilters().add(filter);
		context.setAuthenticator(new BasicAuthenticator("testRealm") {
			@Override
			public boolean checkCredentials(String username, String password) {
				if (username == null || password == null) return false;
				return username.equals("testuser") && password.equals("testpass");
			}
		});
		
		assertThat(simpleUrl("/"), is(Code.HTTP_UNAUTHORIZED));
		assertThat(handler.uris.size(), is(0));
		assertThat(filter.uris.size(), is(0));
		
		assertThat(authenticatedRequest("/", "baduser", "badpass"), is(Code.HTTP_UNAUTHORIZED));
		assertThat(handler.uris.size(), is(0));
		assertThat(filter.uris.size(), is(0));
		
		assertThat(authenticatedRequest("/", "testuser", "testpass"), is(Code.HTTP_OK));
		assertThat(handler.uris.size(), is(1));
		assertThat(filter.uris.size(), is(1));
	}
	
	
	public int authenticatedRequest(String url, String username, String password) throws IOException {
	        CredentialsProvider credsProvider = new BasicCredentialsProvider();
	        credsProvider.setCredentials(
	                new AuthScope("localhost", server.getAddress().getPort()),
	                new UsernamePasswordCredentials(username, password));
	        CloseableHttpClient httpclient = HttpClients.custom()
	                .setDefaultCredentialsProvider(credsProvider)
	                .setSslcontext(getSslContext())
	                .build();
	        try {
	            HttpGet httpget = new HttpGet(getProtocol() + "localhost:" + server.getAddress().getPort() + url);
	            CloseableHttpResponse response = httpclient.execute(httpget);
	            try {
	                EntityUtils.consume(response.getEntity());
	                return response.getStatusLine().getStatusCode();
	            } finally {
	                response.close();
	            }
	        } finally {
	            httpclient.close();
	        }
	    }
}
