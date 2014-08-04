package controllers.Core;

import play.Logger;
import play.Play;
import play.data.validation.Validation;
import play.db.jpa.NoTransaction;
import play.i18n.Messages;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Finally;
import play.mvc.Http.StatusCode;
import play.mvc.Util;
import play.mvc.With;

import static play.templates.JavaExtensions.camelCase;

public class Core extends Controller
{
	/* Common HTTP constants like statuscodes, formats and headers */
	/**
	 * HTTP status code 406, indicating the server has no response that matches
	 * the <code>Accept</code> header sent by the client.
	 */
	public static final int STATUSCODE_NOT_ACCEPTABLE = 406;
	/**
	 * HTTP status code 409, indicating the server can't figure out what to do with
	 * the request.
	 */
	public static final int STATUSCODE_CONFLICT = 409;
	/**
	 * Format string for JSON.
	 */
	public static final String FORMAT_JSON = "json";
	/**
	 * Format string for HTML.
	 */
	public static final String FORMAT_HTML = "html";
	/**
	 * String for HTTP header field Accept.
	 */
	public static final String HEADER_ACCEPT = "Accept";
	/**
	 * String for HTTP header field Location.
	 */
	public static final String HEADER_LOCATION = "Location";
	/**
	 * String for HTTP header field Link.
	 */
	public static final String HEADER_LINK = "Link";
	/**
	 * String for HTTP header field If-Modified-Since.
	 */
	public static final String HEADER_IF_MODIFIED_SINCE = "If-Modified-Since";
	/**
	 * String for HTTP header field Vary.
	 */
	public static final String HEADER_VARY = "Vary";
	/**
	 * String for HTTP header field WWW-Authenticate.
	 */
	public static final String HEADER_AUTHENTICATE = "WWW-Authenticate";
	/**
	 * String for HTTP header field Authorization.
	 */
	public static final String HEADER_AUTHORIZATION = "Authorization";

	/* Utility functions */
	/**
	 * Return a <code>406 (Not Acceptable)</code> response to the user.
	 *
	 * @param acceptable The methods that are acceptable.
	 */
	@Util
	public static void notAcceptable(String acceptable)
	{
		// Set the status code, the message and render the template.
		response.status = STATUSCODE_NOT_ACCEPTABLE;
		String message = Messages.get("error.406.title", camelCase(request.path), acceptable);
		renderTemplate("errors/406.html", message);
	}

	/* Common setup stuff */
	/**
	 * Check the headers and set the format before anything else gets executed.
	 */
	@Before
	static void setFormat()
	{
		// Specify that this response is cacheable based on the Content-Type header.
		if(!response.headers.containsKey(HEADER_VARY.toLowerCase()) && ("GET".equals(request.method) || "HEAD".equals(request.method)))
		{
			response.setHeader(HEADER_VARY, "Content-Type");
			// This addition causes the browser to only use the cached version of a page
			// if the cookie header is set (which is only true if the user is logged in).
			if(Play.modules.containsKey("Secure") && !"Secure".equals(request.controller))
			{
				response.setHeader(HEADER_VARY, response.getHeader(HEADER_VARY) + ",Cookie");
			}
			// This addition makes sure that requests with and without body tags don't get mixed.
			if(request.isAjax())
			{
				response.setHeader(HEADER_VARY, response.getHeader(HEADER_VARY) + ",X-Requested-With");
			}
		}
	}
	/**
	 * Catch any validation errors and return immediately.
	 */
	@Before
	static void catchValidationErrors()
	{
		// Check if there are errors.
		if(Validation.hasErrors())
		{
			// Catch and stop if any errors have been found.
			response.status = StatusCode.BAD_REQUEST;
			// First try to use the response type as a guideline.
			if(response.contentType != null && response.contentType.contains(Core.FORMAT_JSON))
			{
				renderTemplate("/tags/Core/body.json");
			}
			else if(response.contentType != null && response.contentType.contains(Core.FORMAT_HTML))
			{
				renderTemplate("/errors/400.html");
			}
			// Fallback to using the request type.
			if(Core.FORMAT_JSON.equals(request.format))
			{
				renderTemplate("/tags/Core/body.json");
			}
			else if(Core.FORMAT_HTML.equals(request.format))
			{
				renderTemplate("/errors/400.html");
			}
			// Fallback on standard procedure.
			badRequest();
		}
	}

	/* Logging statements */
	/**
	 * On receiving a request, log it.
	 */
	@Before
	static void logRequest()
	{
		if(Logger.isDebugEnabled())
		{
			Logger.debug("Received %s request for '%s'... [session: %s]", request.format, request.path, session.getId());
		}
	}
	/**
	 * After everything else is done, make a log statement about the returned status.
	 */
	@Finally // Use finally because after doesn't catch 404's.
	static void logResponse()
	{
		// Log warning messages for these specific status codes.
		switch(response.status)
		{
			case StatusCode.NO_RESPONSE:
			{
				if(Logger.isDebugEnabled())
				{
					Logger.debug("Returning 204 (No Content)! [session: %s]\n", session.getId());
				}
				break;
			}
			case StatusCode.NOT_MODIFIED:
			{
				if(Logger.isDebugEnabled())
				{
					Logger.debug("Returning 304 (Not Modified)! [session: %s]\n", session.getId());
				}
				break;
			}
			case StatusCode.BAD_REQUEST:
			{
				Logger.warn("Returning 400 (Bad Request) for '%s'! [session: %s]\n", request.path, session.getId());
				break;
			}
			case StatusCode.UNAUTHORIZED:
			{
				Logger.warn("Returning 401 (Unauthorized) [session: %s]\n", session.getId());
				break;
			}
			case StatusCode.NOT_FOUND:
			{
				Logger.warn("Returning 404 (Not Found) for '%s'! [session: %s]\n", request.path, session.getId());
				break;
			}
			case STATUSCODE_NOT_ACCEPTABLE:
			{
				Logger.warn("Returning 406 (Not Acceptable) for '%s' with format '%s'! [session: %s]\n", request.path, request.format, session.getId());
				break;
			}
			case STATUSCODE_CONFLICT:
			{
				Logger.warn("Returning 409 (Conflict) for '%s'! [session: %s]\n", request.path, request.format, session.getId());
				break;
			}
			case StatusCode.INTERNAL_ERROR:
			{
				Logger.warn("Returning 500 (Internal Error) for '%s'! [session: %s]\n", request.path, session.getId());
				break;
			}
			default:
			{
				break;
			}
		}
	}
}