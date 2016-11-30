
package at.spot.core.infrastructure.service.impl;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import at.spot.core.infrastructure.service.ConfigurationService;

@Service
public class DefaultConfigurationService extends AbstractService implements ConfigurationService {

	// order is important here
	protected Set<Properties> configurations = new LinkedHashSet<>();

	@Override
	public String getString(final String key) {
		return getProperty(key);
	}

	@Override
	public Integer getInteger(final String key) {
		Integer value = null;

		try {
			value = Integer.parseInt(getProperty(key));
		} catch (final NumberFormatException e) {
			loggingService.exception(String.format("Can't load config key %s", key), e);
		}

		return value;
	}

	@Override
	public Double getDouble(final String key) {
		Double value = null;

		try {
			value = Double.parseDouble(getProperty(key));
		} catch (final NumberFormatException e) {
			loggingService.exception(String.format("Can't load config key %s", key), e);
		}

		return value;
	}

	@Override
	public String getString(final String key, final String defaultValue) {
		final String val = getString(key);

		return val != null ? val : defaultValue;
	}

	@Override
	public Integer getInteger(final String key, final Integer defaultValue) {
		Integer val = getInteger(key);

		if (val == null) {
			val = defaultValue;
		}

		return val != null ? val : defaultValue;
	}

	@Override
	public Double getDouble(final String key, final Double defaultValue) {
		Double val = getDouble(key);

		if (val == null) {
			val = defaultValue;
		}

		return val != null ? val : defaultValue;
	}

	/**
	 * Iterates over all registered properties files.
	 * 
	 * @param key
	 * @return
	 */
	protected String getProperty(final String key) {
		String value = null;

		for (final Properties prop : configurations) {
			value = prop.getProperty(key);

			if (StringUtils.isNotBlank(value)) {
				break;
			}
		}

		return value;
	}

	@Override
	public void load(final Properties... configuration) {
		if (configuration != null) {
			configurations.addAll(Arrays.asList(configuration));
		}
	}
}
