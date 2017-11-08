package at.spot.core.infrastructure.http;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.iterators.IteratorEnumeration;
import org.joda.time.DateTime;

public class Session {

	protected boolean isValid = true;

	protected String id;
	protected DateTime creationTime = new DateTime();
	protected DateTime lastAccessedTime = new DateTime();
	protected final Map<String, Object> attributes = new HashMap<>();

	protected int maxInactiveInterval;

	public Session(final String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public Object getAttribute(final String name) {
		return attributes.get(name);
	}

	public <T> T getAttribute(final String name, Class<T> type) {
		return (T) getAttribute(name);
	}

	public void setAttribute(final String name, final Object value) {
		attributes.put(name, value);
	}

	public boolean isValid() {
		return isValid;
	}

	public void invalidate() {
		this.isValid = false;
	}

	public Enumeration<String> getAttributeNames() {
		return new IteratorEnumeration<String>(attributes.keySet().iterator());
	}

	public long getCreationTime() {
		return creationTime.toDate().getTime();
	}

	public long getLastAccessedTime() {
		return lastAccessedTime.toDate().getTime();
	}

	public int getMaxInactiveInterval() {
		return maxInactiveInterval;
	}

	public void removeAttribute(final String attributeName) {
		attributes.remove(attributeName);
	}

	public void setMaxInactiveInterval(final int maxInactiveInterval) {
		this.maxInactiveInterval = maxInactiveInterval;
	}
}
