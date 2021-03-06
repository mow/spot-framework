package io.spotnext.core.management.transformer;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.spotnext.core.infrastructure.http.HttpResponse;

/**
 * <p>
 * ResponseTransformer interface.
 * </p>
 *
 * @author mojo2012
 * @version 1.0
 * @since 1.0
 */
@SuppressFBWarnings("NM_SAME_SIMPLE_NAME_AS_INTERFACE")
public interface ResponseTransformer extends spark.ResponseTransformer {

	/**
	 * {@inheritDoc} Checks if the given response object is an exception (or in case it's a {@link HttpResponse} if its payload is an exception) and either
	 * calls the {@link #handleException(Throwable)} method or otherwise call the {@link #handleResponse(Object)} method.
	 * 
	 * @throw Exception when handling fails
	 */
	@Override
	default String render(Object responseObject) throws Exception {
		// some exceptions are caught and are already wrapped in an ExceptionResponse, then we just render it
		// otherwise handle the exception handle it in a special way
		if (responseObject instanceof Exception) {
			return handleException(responseObject, (Exception) responseObject);
		}

		return handleResponse(responseObject);
	}

	/**
	 * Renders the HTTP handlers return object.
	 *
	 * @param responseObject will be rendered by this transformer
	 * @return the rendered output
	 * @throws java.lang.Exception when handling fails
	 */
	String handleResponse(Object responseObject) throws Exception;

	/**
	 * Handles exception that are thrown by the HTTP handler (annotated with {@link io.spotnext.core.management.annotation.Handler}). The exception handler can
	 * throw exceptions again which will then be handled by the default exception handler.
	 *
	 * @param responseObject the original response object
	 * @param exception      that is thrown in the HTTP handler
	 * @return the rendered
	 * @throws java.lang.Exception when handling fails
	 */
	default String handleException(Object responseObject, Exception exception) throws Exception {
		throw exception;
	}
}
