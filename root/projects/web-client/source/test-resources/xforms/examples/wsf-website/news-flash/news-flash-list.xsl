<?xml version="1.0" encoding="UTF-8"?>
<!--
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.

-->
<xsl:stylesheet version="1.0" xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:nf="http://www.alfresco.org/alfresco/nf"
	xmlns:fn="http://www.w3.org/2005/02/xpath-functions" exclude-result-prefixes="xhtml">
	<xsl:output method="html" encoding="UTF-8" indent="yes"
		doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
		doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"/>

	<xsl:preserve-space elements="*"/>

	<xsl:template match="/">
		<xsl:text disable-output-escaping="yes">&lt;%String channel = (String) request.getParameter(&quot;channel&quot;);%&gt;</xsl:text>
		<table>
		   <tr>
			<td>
				<xsl:element name="a">
      					<xsl:attribute name="href">/views/pages/details.jsp?channel=&lt;%=channel%&gt;&amp;amp;content=/content/news-flash/<xsl:value-of select="fn:replaceAll(string($alf:form_instance_data_file_name), '.xml', '.html')"/>&amp;amp;title=<xsl:value-of select="/nf:news_flash/nf:title"/></xsl:attribute>
      					<xsl:value-of select="/nf:news_flash/nf:title"/>
    				</xsl:element>
			</td>
		    </tr>	
		</table>
	</xsl:template>
</xsl:stylesheet>
