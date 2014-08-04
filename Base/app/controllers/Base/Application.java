package controllers.Base;

import controllers.Core.Core;
import play.Logger;
import play.db.jpa.NoTransaction;
import play.mvc.Controller;
import play.mvc.With;

@With({Core.class})
public class Application extends Controller
{
	/**
	 * The index page.
	 * <p/>
	 * Redirects to the settings page, unless the requested format is JSON.
	 */
	@NoTransaction
	public static void index()
	{
		if(!Core.FORMAT_JSON.equals(request.format))
		{
			settings();
		}
		render();
	}

	/**
	 * Display the settings page.
	 * <p/>
	 * If the requested format is not HTML, this will return a <code>406 Not Acceptable</code>.
	 */
	@NoTransaction
	public static void settings()
	{
		// Only make this available to HTML requests.
		if(Core.FORMAT_HTML.equals(request.format))
		{
			if(Logger.isDebugEnabled())
			{
				Logger.debug("Rendering settings page!\n");
			}
			render();
		}
		Core.notAcceptable(Core.FORMAT_HTML.toUpperCase());
	}
}