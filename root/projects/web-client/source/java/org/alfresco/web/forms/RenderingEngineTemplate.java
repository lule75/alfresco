/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.web.forms;

import org.alfresco.service.cmr.repository.NodeRef;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Map;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Describes a template that is used for rendering form instance data.
 *
 * @author Ariel Backenroth
 */
public interface RenderingEngineTemplate
   extends Serializable
{
   /** the name of the rendering engine template */
   public String getName();

   /** the title of the rendering engine template */
   public String getTitle();

   /** the description of the rendering engine template */
   public String getDescription();

   /** the output path pattern for renditions */
   public String getOutputPathPattern();
   
   /**
    * Provides the rendering engine to use to process this template.
    *
    * @return the rendering engine to use to process this template.
    */
   public RenderingEngine getRenderingEngine();

   /**
    * Provides an input stream to the rendering engine template.
    * 
    * @return the input stream to the rendering engine template.
    */
   public InputStream getInputStream()
      throws IOException;

   /**
    * Returns the output path for the rendition.
    *
    * @return the output path for the rendition.
    */
   public String getOutputPathForRendition(final FormInstanceData formInstanceData);

   /**
    * Returns the mimetype to use when generating content for this
    * output method.
    *
    * @return the mimetype to use when generating content for this
    * output method, such as text/html, text/xml, application/pdf.
    */
   public String getMimetypeForRendition();

   /**
    * Produces a rendition of the provided formInstanceData.
    *
    * @param formInstanceData the form instance data for which to produce
    * the rendition.
    */
   public Rendition render(final FormInstanceData formInstanceData)
      throws IOException,
      SAXException,
      RenderingEngine.RenderingException;

   /**
    * Produces a rendition of the provided formInstanceData to an existing
    * rendition.
    *
    * @param formInstanceData the form instance data for which to produce
    * the rendition.
    * @param rendition the rendition to rerender
    */
   public void render(final FormInstanceData formInstanceData,
                      final Rendition rendition)
      throws IOException,
      SAXException,
      RenderingEngine.RenderingException;
}
