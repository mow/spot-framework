package at.spot.core.infrastructure.interceptor;

import at.spot.core.infrastructure.exception.ItemInterceptorException;
import at.spot.core.model.Item;

public interface ItemCreateInterceptor<T extends Item> extends ItemInterceptor<T> {
	/**
	 * The newly created item has been initialized from the persistence layer.
	 * Further initialization (eg. default values) can be set here.
	 * 
	 * @param item
	 *            the newly instantiated item instance
	 * @throws ItemInterceptorException
	 *             if thrown the item instantiation will be cancelled
	 */
	void onCreate(T item) throws ItemInterceptorException;
}
