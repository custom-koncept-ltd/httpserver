package koncept.http.server;

import java.util.Map;

/**
 * This interface defines a way to observe and change the optional behaviour of the server.<br>
 * Note that whilst some options may be run time configurable, some won't be, and as such
 * its recommended that all options are set once, before calling .start();
 * @author nicholas.Krul@gmail.com
 *
 */
public interface ConfigurableServer {
	
	/**
	 * Calling this should set options to have basic compatibility with the default JVM implementation<br>
	 * Due to some... interesting... design decisions (or the lack thereof), this is <b>not</b> recommended.
	 */
	public void resetOptionsToJVMStandard();
	
	/**
	 * Calling this should reset the options to their defaults - the initial state of the server.
	 */
	public void resetOptionsToDefaults();
	
	/**
	 * This is a VIEW of the current options - it should be read only
	 * @return
	 */
	public Map<ConfigurationOption, String> options();
	
	/**
	 * Used to update individual configuration options<br>
	 * 
	 * Note that Validation may (and should) occur
	 * 
	 * @param option
	 * @param value
	 */
	public void setOption(ConfigurationOption option, String value);
}
