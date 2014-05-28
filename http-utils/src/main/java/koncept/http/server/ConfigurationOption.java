package koncept.http.server;

import java.util.Map;

public class ConfigurationOption {
	private final String key;
	private final String[] values;

	public ConfigurationOption(String key, String... values) {
		this.key = key;
		this.values = values;
	}
	
	public String key() {
		return key;
	}
	
	public String[] values() {
		return values;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj.getClass() == getClass()) return key.equals(((ConfigurationOption)obj).key);
		return false;
	}
	
	@Override
	public int hashCode() {
		return key.hashCode();
	}
	
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder(key);
		s.append("[");
		if (values.length > 0) s.append(values[0]);
		for(int i = 1; i < values.length; i++) {
			s.append(",");
			s.append(values[i]);
		}
		s.append("]");
		return s.toString();
	}
	
	public static String set(Map<ConfigurationOption, String> options, String key, String value) {
		for(ConfigurationOption option: options.keySet()) {
    		if (option.key().equals(key)) {
    			return set(options, option, value);
    		}
		}
		throw new IllegalArgumentException("invalid key: " + key);
	}
	
	public static String set(Map<ConfigurationOption, String> options, ConfigurationOption option, String value) {
		for(String validValue :option.values()) {
			if (value.equals(validValue)) {
				return options.put(option, value);
			}
		}
		throw new IllegalArgumentException("invalid value: " + value);
	}
	
	public static String get(Map<ConfigurationOption, String> options, String key) {
		for(ConfigurationOption option: options.keySet())
    		if (option.key().equals(key))
    			return options.get(key);
		throw new IllegalArgumentException("invalid key: " + key);
	}
}
