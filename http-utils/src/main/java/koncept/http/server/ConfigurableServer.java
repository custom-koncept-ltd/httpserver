package koncept.http.server;

import java.util.Map;

public interface ConfigurableServer {
	
	public void resetOptionsToJVMStandard();
	
	public void resetOptionsToDefaults();
	
	public Map<ConfigurationOption, String> options();
}
