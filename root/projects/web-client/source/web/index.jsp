<%--
Copyright (C) 2005 Alfresco, Inc.

 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
--%>

<%@ page import="javax.faces.context.FacesContext" %>
<%@ page import="org.springframework.web.context.support.WebApplicationContextUtils" %>
<%@ page import="org.alfresco.service.cmr.security.PermissionService" %>
<%@ page import="org.alfresco.config.ConfigService" %>
<%@ page import="org.alfresco.web.app.servlet.AuthenticationHelper" %>
<%@ page import="org.alfresco.web.app.servlet.FacesHelper" %>
<%@ page import="org.alfresco.web.bean.NavigationBean" %>
<%@ page import="org.alfresco.web.bean.repository.User" %>
<%@ page import="org.alfresco.web.bean.repository.PreferencesService" %>
<%@ page import="org.alfresco.web.config.ClientConfigElement" %>

<%-- redirect to the web application's appropriate start page --%>
<%
// get the start location as configured by the web-client config
ConfigService configService = (ConfigService)WebApplicationContextUtils.getRequiredWebApplicationContext(session.getServletContext()).getBean("webClientConfigService");
ClientConfigElement configElement = (ClientConfigElement)configService.getGlobalConfig().getConfigElement("client");
String location = configElement.getInitialLocation();

// override with the users preference if they have one
User user = (User)session.getAttribute(AuthenticationHelper.AUTHENTICATION_USER);
if (user != null && (user.getUserName().equals(PermissionService.GUEST_AUTHORITY) == false))
{
   // ensure construction of the FacesContext before attemping a service call
   FacesContext fc = FacesHelper.getFacesContext(request, response, application);
   String preference = (String)PreferencesService.getPreferences(fc).getValue("start-location");
   if (preference != null)
   {
      location = preference;
   }
}
if (NavigationBean.LOCATION_MYALFRESCO.equals(location))
{
   response.sendRedirect(request.getContextPath() + "/faces/jsp/dashboards/container.jsp");
}
else
{
   response.sendRedirect(request.getContextPath() + "/faces/jsp/browse/browse.jsp");
}
%>
