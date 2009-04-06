<#-- Look up page title from message bundles where possible -->
<#assign pageTitle=page.title />
<#if page.titleId??>
<#assign pageTitle=(msg(page.titleId))!page.title />
</#if>

<#--
   Future - JavaScript and CSS minimisation via YUI Compressor.
-->
<#macro script type src>
   <script type="${type}" src="${src}"></script>
</#macro>
<#macro link rel type href>
   <link rel="${rel}" type="${type}" href="${href}" />
</#macro>

<#assign theme="default">

<#--
   Template "templateHeader" macro.
   Pulls in template assets.
-->                                                                           
<#macro templateHeader doctype="strict">
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
   <title>Alfresco Mobile &raquo; ${pageTitle}</title>

<!-- Shortcut Icons -->
   <link rel="shortcut icon" href="${url.context}/favicon.ico" type="image/vnd.microsoft.icon" /> 
   <link rel="icon" href="${url.context}/favicon.ico" type="image/vnd.microsoft.icon" />

<!-- Site-wide Common Assets -->
   <@link rel="stylesheet" type="text/css" href="${url.context}/themes/${theme}/base.css" />
   <script type="text/javascript" src="${url.context}/service/messages.js?locale=${locale}"></script>
   <@script type="text/javascript" src="${url.context}/js/mobile.js"></@script>
   <script type="text/javascript">//<![CDATA[
      Mobile.constants.PROXY_URI = window.location.protocol + "//" + window.location.host + "${url.context}/proxy/alfresco/";
      Mobile.constants.PROXY_URI_RELATIVE = "${url.context}/proxy/alfresco/";
      Mobile.constants.PROXY_FEED_URI = window.location.protocol + "//" + window.location.host + "${url.context}/proxy/alfresco-feed/";
      Mobile.constants.THEME = "${theme}";
      Mobile.constants.URL_CONTEXT = "${url.context}/";
      Mobile.constants.URL_PAGECONTEXT = "${url.context}/p/";
      Mobile.constants.URL_SERVICECONTEXT = "${url.context}/service/";
      Mobile.constants.URL_FEEDSERVICECONTEXT = "${url.context}/feedservice/";
      Mobile.constants.USERNAME = "${user.name!""}";
   //]]></script>

<!-- Template Assets -->
<#nested>

<!-- Component Assets -->
${head}
</head>
</#macro>

<#--
   Template "templateBody" macro.
   Pulls in main template body.
-->
<#macro templateBody>
<body>
<#-- Template-specific body markup -->
<#nested>
<#--
   Pulls in template footer.
-->
   <div id="alf-ft">
      <@region id="footer" scope="global" protected=true />
   </div>
</body>
</html>
</#macro>