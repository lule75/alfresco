/*
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
 */
package org.alfresco.repo.content;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.content.encoding.ContentCharsetFinder;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.util.PropertyCheck;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.config.Config;
import org.springframework.extensions.config.ConfigElement;
import org.springframework.extensions.config.ConfigLookupContext;
import org.springframework.extensions.config.ConfigService;

/**
 * Provides a bidirectional mapping between well-known mimetypes and
 * the registered file extensions.  All mimetypes and extensions
 * are stored and handled as lowercase.
 * 
 * @author Derek Hulley
 */
public class MimetypeMap implements MimetypeService
{
    public static final String PREFIX_TEXT = "text/";
    public static final String EXTENSION_BINARY = "bin";
    
    public static final String MIMETYPE_TEXT_PLAIN = "text/plain";
    public static final String MIMETYPE_TEXT_MEDIAWIKI = "text/mediawiki";
    public static final String MIMETYPE_TEXT_CSS = "text/css";    
    public static final String MIMETYPE_TEXT_JAVASCRIPT = "text/javascript";    
    public static final String MIMETYPE_XML = "text/xml";
    public static final String MIMETYPE_HTML = "text/html";
    public static final String MIMETYPE_XHTML = "application/xhtml+xml";
    public static final String MIMETYPE_PDF = "application/pdf";
    public static final String MIMETYPE_JSON = "application/json";
    public static final String MIMETYPE_WORD = "application/msword";
    public static final String MIMETYPE_EXCEL = "application/vnd.ms-excel";
    public static final String MIMETYPE_BINARY = "application/octet-stream";
    public static final String MIMETYPE_PPT = "application/vnd.ms-powerpoint";
    public static final String MIMETYPE_APP_DWG = "application/dwg";
    public static final String MIMETYPE_IMG_DWG = "image/x-dwg";
    public static final String MIMETYPE_FLASH = "application/x-shockwave-flash";
    public static final String MIMETYPE_IMAGE_GIF = "image/gif";
    public static final String MIMETYPE_IMAGE_JPEG = "image/jpeg";
    public static final String MIMETYPE_IMAGE_RGB = "image/x-rgb";
    public static final String MIMETYPE_IMAGE_SVG = "image/svg";
    public static final String MIMETYPE_IMAGE_PNG = "image/png";
    public static final String MIMETYPE_JAVASCRIPT = "application/x-javascript";
    public static final String MIMETYPE_ZIP = "application/zip";
    public static final String MIMETYPE_OPENSEARCH_DESCRIPTION = "application/opensearchdescription+xml";
    public static final String MIMETYPE_ATOM = "application/atom+xml";
    public static final String MIMETYPE_RSS = "application/rss+xml";
    public static final String MIMETYPE_RFC822 = "message/rfc822";
    public static final String MIMETYPE_OUTLOOK_MSG = "application/vnd.ms-outlook";
    // Open Document
    public static final String MIMETYPE_OPENDOCUMENT_TEXT = "application/vnd.oasis.opendocument.text";
    public static final String MIMETYPE_OPENDOCUMENT_TEXT_TEMPLATE = "application/vnd.oasis.opendocument.text-template";
    public static final String MIMETYPE_OPENDOCUMENT_GRAPHICS = "application/vnd.oasis.opendocument.graphics";
    public static final String MIMETYPE_OPENDOCUMENT_GRAPHICS_TEMPLATE= "application/vnd.oasis.opendocument.graphics-template";
    public static final String MIMETYPE_OPENDOCUMENT_PRESENTATION= "application/vnd.oasis.opendocument.presentation";
    public static final String MIMETYPE_OPENDOCUMENT_PRESENTATION_TEMPLATE= "application/vnd.oasis.opendocument.presentation-template";
    public static final String MIMETYPE_OPENDOCUMENT_SPREADSHEET= "application/vnd.oasis.opendocument.spreadsheet";
    public static final String MIMETYPE_OPENDOCUMENT_SPREADSHEET_TEMPLATE= "application/vnd.oasis.opendocument.spreadsheet-template";
    public static final String MIMETYPE_OPENDOCUMENT_CHART= "application/vnd.oasis.opendocument.chart";
    public static final String MIMETYPE_OPENDOCUMENT_CHART_TEMPLATE= "applicationvnd.oasis.opendocument.chart-template";
    public static final String MIMETYPE_OPENDOCUMENT_IMAGE= "application/vnd.oasis.opendocument.image";
    public static final String MIMETYPE_OPENDOCUMENT_IMAGE_TEMPLATE= "applicationvnd.oasis.opendocument.image-template";
    public static final String MIMETYPE_OPENDOCUMENT_FORMULA= "application/vnd.oasis.opendocument.formula";
    public static final String MIMETYPE_OPENDOCUMENT_FORMULA_TEMPLATE= "applicationvnd.oasis.opendocument.formula-template";
    public static final String MIMETYPE_OPENDOCUMENT_TEXT_MASTER= "application/vnd.oasis.opendocument.text-master";
    public static final String MIMETYPE_OPENDOCUMENT_TEXT_WEB= "application/vnd.oasis.opendocument.text-web";
    public static final String MIMETYPE_OPENDOCUMENT_DATABASE= "application/vnd.oasis.opendocument.database";
    // Open Office
    public static final String MIMETYPE_OPENOFFICE1_WRITER = "application/vnd.sun.xml.writer";
    public static final String MIMETYPE_OPENOFFICE1_CALC = "application/vnd.sun.xml.calc";
    public static final String MIMETYPE_OPENOFFICE1_DRAW = "application/vnd.sun.xml.draw";
    public static final String MIMETYPE_OPENOFFICE1_IMPRESS = "application/vnd.sun.xml.impress";
    // Open XML
    public static final String MIMETYPE_OPENXML_WORDPROCESSING = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    public static final String MIMETYPE_OPENXML_SPREADSHEET = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String MIMETYPE_OPENXML_PRESENTATION = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
    // Star Office
    public static final String MIMETYPE_STAROFFICE5_DRAW = "application/vnd.stardivision.draw";
    public static final String MIMETYPE_STAROFFICE5_CALC = "application/vnd.stardivision.calc";
    public static final String MIMETYPE_STAROFFICE5_IMPRESS = "application/vnd.stardivision.impress";
    public static final String MIMETYPE_STAROFFICE5_IMPRESS_PACKED = "application/vnd.stardivision.impress-packed";
    public static final String MIMETYPE_STAROFFICE5_CHART = "application/vnd.stardivision.chart";
    public static final String MIMETYPE_STAROFFICE5_WRITER = "application/vnd.stardivision.writer";
    public static final String MIMETYPE_STAROFFICE5_WRITER_GLOBAL = "application/vnd.stardivision.writer-global";
    public static final String MIMETYPE_STAROFFICE5_MATH = "application/vnd.stardivision.math";
    // WordPerfect
    public static final String MIMETYPE_WORDPERFECT = "application/wordperfect";
    // Audio
    public static final String MIMETYPE_MP3 = "audio/x-mpeg";
    // Alfresco
    public static final String MIMETYPE_ACP = "application/acp";
    
    private static final String CONFIG_AREA = "mimetype-map";
    private static final String CONFIG_CONDITION = "Mimetype Map";
    private static final String ELEMENT_MIMETYPES = "mimetypes";
    private static final String ATTR_MIMETYPE = "mimetype";
    private static final String ATTR_DISPLAY = "display";
    private static final String ATTR_DEFAULT = "default";
    private static final String ATTR_TEXT = "text";
    
    private static final Log logger = LogFactory.getLog(MimetypeMap.class);
    
    private ConfigService configService;
    private ContentCharsetFinder contentCharsetFinder;
    
    private List<String> mimetypes;
    private Map<String, String> extensionsByMimetype;
    private Map<String, String> mimetypesByExtension;
    private Map<String, String> displaysByMimetype;
    private Map<String, String> displaysByExtension;
    private Set<String> textMimetypes;
    
    /**
     * Default constructor
     * 
     * @since 2.1
     */
    public MimetypeMap()
    {
    }

    @Deprecated
    public MimetypeMap(ConfigService configService)
    {
        logger.warn(
                "MimetypeMap(ConfigService configService) has been deprecated.  " +
                "Use the default constructor and property 'configService'");
        this.configService = configService;
    }
    
    /**
     * @param configService         the config service to use to read mimetypes from
     */
    public void setConfigService(ConfigService configService)
    {
        this.configService = configService;
    }

    /**
     * {@inheritDoc}
     */
    public ContentCharsetFinder getContentCharsetFinder()
    {
        return contentCharsetFinder;
    }

    /**
     * Set the system default content characterset decoder
     */
    public void setContentCharsetFinder(ContentCharsetFinder contentCharsetFinder)
    {
        this.contentCharsetFinder = contentCharsetFinder;
    }

    /**
     * Initialises the map using the configuration service provided
     */
    public void init()
    {
        PropertyCheck.mandatory(this, "configService", configService);
        PropertyCheck.mandatory(this, "contentCharsetFinder", contentCharsetFinder);
        
        this.mimetypes = new ArrayList<String>(40);
        this.extensionsByMimetype = new HashMap<String, String>(59);
        this.mimetypesByExtension = new HashMap<String, String>(59);
        this.displaysByMimetype = new HashMap<String, String>(59);
        this.displaysByExtension = new HashMap<String, String>(59);
        this.textMimetypes = new HashSet<String>(23);

        Config config = configService.getConfig(CONFIG_CONDITION, new ConfigLookupContext(CONFIG_AREA));
        ConfigElement mimetypesElement = config.getConfigElement(ELEMENT_MIMETYPES);
        List<ConfigElement> mimetypes = mimetypesElement.getChildren();
        int count = 0;
        for (ConfigElement mimetypeElement : mimetypes)
        {
            count++;
            // add to list of mimetypes
            String mimetype = mimetypeElement.getAttribute(ATTR_MIMETYPE);
            if (mimetype == null || mimetype.length() == 0)
            {
                logger.warn("Ignoring empty mimetype " + count);
                continue;
            }
            // we store it as lowercase
            mimetype = mimetype.toLowerCase();
            if (this.mimetypes.contains(mimetype))
            {
                throw new AlfrescoRuntimeException("Duplicate mimetype definition: " + mimetype);
            }
            this.mimetypes.add(mimetype);
            // add to map of mimetype displays
            String mimetypeDisplay = mimetypeElement.getAttribute(ATTR_DISPLAY);
            if (mimetypeDisplay != null && mimetypeDisplay.length() > 0)
            {
                this.displaysByMimetype.put(mimetype, mimetypeDisplay);
            }

            // Check if it is a text format
            String isTextStr = mimetypeElement.getAttribute(ATTR_TEXT);
            boolean isText = Boolean.parseBoolean(isTextStr);
            if (isText || mimetype.startsWith(PREFIX_TEXT))
            {
                this.textMimetypes.add(mimetype);
            }
            
            // get all the extensions
            boolean isFirst = true;
            List<ConfigElement> extensions = mimetypeElement.getChildren();
            for (ConfigElement extensionElement : extensions)
            {
                // add to map of mimetypes by extension
                String extension = extensionElement.getValue();
                if (extension == null || extension.length() == 0)
                {
                    logger.warn("Ignoring empty extension for mimetype: " + mimetype);
                    continue;
                }
                // put to lowercase
                extension = extension.toLowerCase();
                this.mimetypesByExtension.put(extension, mimetype);
                // add to map of extension displays
                String extensionDisplay = extensionElement.getAttribute(ATTR_DISPLAY);
                if (extensionDisplay != null && extensionDisplay.length() > 0)
                {
                    this.displaysByExtension.put(extension, extensionDisplay);
                }
                else if (mimetypeDisplay != null && mimetypeDisplay.length() > 0)
                {
                    // no display defined for the extension - use the mimetype's display
                    this.displaysByExtension.put(extension, mimetypeDisplay);
                }
                // add to map of extensions by mimetype if it is the default or first extension
                String isDefaultStr = extensionElement.getAttribute(ATTR_DEFAULT);
                boolean isDefault = Boolean.parseBoolean(isDefaultStr);
                if (isDefault || isFirst)
                {
                    this.extensionsByMimetype.put(mimetype, extension);
                }
                // Loop again
                isFirst = false;
            }
            // check that there were extensions defined
            if (extensions.size() == 0)
            {
                logger.warn("No extensions defined for mimetype: " + mimetype);
            }
        }
        
        // make the collections read-only
        this.mimetypes = Collections.unmodifiableList(this.mimetypes);
        this.extensionsByMimetype = Collections.unmodifiableMap(this.extensionsByMimetype);
        this.mimetypesByExtension = Collections.unmodifiableMap(this.mimetypesByExtension);
        this.displaysByMimetype = Collections.unmodifiableMap(this.displaysByMimetype);
        this.displaysByExtension = Collections.unmodifiableMap(this.displaysByExtension);
    }
    
    /**
     * Get the file extension associated with the mimetype.
     * 
     * @param mimetype a valid mimetype
     * @return Returns the default extension for the mimetype.  Returns the {@link #MIMETYPE_BINARY binary}
     *      mimetype extension.
     * 
     * @see #MIMETYPE_BINARY
     * @see #EXTENSION_BINARY
     */
    public String getExtension(String mimetype)
    {
        String extension = extensionsByMimetype.get(mimetype);
        return (extension == null ? EXTENSION_BINARY : extension);
    }
    
    /**
     * Get the mimetype for the specified extension
     * 
     * @param extension a valid file extension
     * @return Returns a valid mimetype if found, or {@link #MIMETYPE_BINARY binary} as default.
     */
    public String getMimetype(String extension)
    {
        String mimetype = MIMETYPE_BINARY;
        if (extension != null)
        {
            extension = extension.toLowerCase();
            if (mimetypesByExtension.containsKey(extension))
            {
                mimetype = mimetypesByExtension.get(extension);
            }
        }
        return mimetype;
    }

    public Map<String, String> getDisplaysByExtension()
    {
        return displaysByExtension;
    }

    public Map<String, String> getDisplaysByMimetype()
    {
        return displaysByMimetype;
    }

    public Map<String, String> getExtensionsByMimetype()
    {
        return extensionsByMimetype;
    }

    public List<String> getMimetypes()
    {
        return mimetypes;
    }

    public Map<String, String> getMimetypesByExtension()
    {
        return mimetypesByExtension;
    }

    public boolean isText(String mimetype)
    {
        return textMimetypes.contains(mimetype);
    }

    /**
     * @see #MIMETYPE_BINARY
     */
    public String guessMimetype(String filename)
    {
        String mimetype = MIMETYPE_BINARY;
        // extract the extension
        int index = filename.lastIndexOf('.');
        if (index > -1 && (index < filename.length() - 1))
        {
            String extension = filename.substring(index + 1).toLowerCase();
            if (mimetypesByExtension.containsKey(extension))
            {
                mimetype = mimetypesByExtension.get(extension);
            }
        }
        return mimetype;
    }
}
