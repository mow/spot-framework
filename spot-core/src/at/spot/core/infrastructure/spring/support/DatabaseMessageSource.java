package at.spot.core.infrastructure.spring.support;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.AbstractMessageSource;

import at.spot.core.infrastructure.service.ModelService;
import at.spot.core.model.internationalization.LocalizationKey;

/**
 * Fetches spring message values from the database, instead of the property
 * files.
 */
// @Component("databaseMessageSource")
public class DatabaseMessageSource extends AbstractMessageSource {

	@Autowired
	protected ModelService modelService;

	@Override
	protected MessageFormat resolveCode(final String key, final Locale locale) {
		MessageFormat msg = null;

		final Map<String, Comparable<?>> keyParam = new HashMap<>();
		keyParam.put("key", key);

		List<LocalizationKey> translations = null;
		translations = modelService.getAll(LocalizationKey.class, keyParam);

		if (translations != null && translations.size() > 0) {
			msg = new MessageFormat(translations.get(0).value, locale);
		}

		return msg;
	}

}
