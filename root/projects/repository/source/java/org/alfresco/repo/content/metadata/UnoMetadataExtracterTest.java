/*
 * Copyright (C) 2005 Jesper Steen M�ller
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
package org.alfresco.repo.content.metadata;


/**
 * @author Jesper Steen M�ller
 */
public class UnoMetadataExtracterTest extends AbstractMetadataExtracterTest
{
    private UnoMetadataExtracter extracter;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        extracter = new UnoMetadataExtracter();
        extracter.setMimetypeService(mimetypeMap);
        extracter.init();
    }

    /**
     * @return Returns the same extracter regardless - it is allowed
     */
    protected MetadataExtracter getExtracter()
    {
        return extracter;
    }

    public void testReliability() throws Exception
    {
        if (!extracter.isConnected())
        {
            return;
        }
        
        for (String mimetype : UnoMetadataExtracter.SUPPORTED_MIMETYPES)
        {
            double reliability = extracter.getReliability(mimetype);
            assertTrue("Expected above zero reliability", reliability > 0.0);
        }
    }

    public void testSupportedMimetypes() throws Exception
    {
        if (!extracter.isConnected())
        {
            return;
        }
        for (String mimetype : UnoMetadataExtracter.SUPPORTED_MIMETYPES)
        {
            testExtractFromMimetype(mimetype);
        }
    }
}
