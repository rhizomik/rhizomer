package net.rhizomik.wiki.fckeditor;

import javax.servlet.http.HttpServletRequest;

import net.fckeditor.requestcycle.UserAction;

/**
 * Custor implementation of the user access check in FCKEditor browser.
 * It checks that the user is in role "rhizomer"
 */
public class UserActionImpl implements UserAction {

	public boolean isEnabledForFileBrowsing(final HttpServletRequest request) 
	{
		return request.isUserInRole("rhizomer");
	}

	public boolean isEnabledForFileUpload(final HttpServletRequest request) 
	{
		return request.isUserInRole("rhizomer");
	}

}
