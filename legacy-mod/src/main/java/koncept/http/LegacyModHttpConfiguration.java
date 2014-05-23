package koncept.http;

/**
 * Includes options as to whether to include or replace some aspects of legacy behaviour
 * @author koncept
 *
 */
public class LegacyModHttpConfiguration {

	public static enum HttpExchangeAttributeScoping{CONTEXT_LEGACY, EXCHANGE}
	
	private HttpExchangeAttributeScoping exchangeAttributeScoping = HttpExchangeAttributeScoping.CONTEXT_LEGACY;

	
	
	public HttpExchangeAttributeScoping getExchangeAttributeScoping() {
		return exchangeAttributeScoping;
	}
	
	public void setExchangeAttributeScoping(
			HttpExchangeAttributeScoping exchangeAttributeScoping) {
		this.exchangeAttributeScoping = exchangeAttributeScoping;
	}
	
	
}
