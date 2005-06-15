package org.alfresco.repo.content.memorystore;

import java.nio.channels.ReadableByteChannel;

import org.alfresco.repo.content.AbstractContentReader;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;

/**
 * A reader onto <tt>byte[]</tt> instances in memory.
 * <p>
 * There is no permament persistence of the content and this class should
 * therefore only be used for the most transient of content.
 * 
 * @author Derek Hulley
 */
public class MemoryContentReader extends AbstractContentReader
{
    private byte[] content;
    
    /**
     * @param contentUrl the URL by which the content can be accessed
     * @param content the physical bytes making up the the content
     */
    public MemoryContentReader(String contentUrl, byte[] content)
    {
        super(contentUrl);
        if (content == null)
        {
            throw new IllegalArgumentException("byte[] may not be null");
        }
        this.content = content;
    }

    @Override
    protected ContentReader createReader() throws ContentIOException
    {
        return new MemoryContentReader(this.getContentUrl(), this.content);
    }
 
    /**
     * @return Return true always as memory is always available
     */
    public boolean exists()
    {
        // the content always exists
        return true;
    }
    
    /**
     * @return Returns the number of bytes held in the in-memory buffer
     */
    public long getLength()
    {
        return content.length;
    }

    protected ReadableByteChannel getDirectReadableChannel() throws ContentIOException
    {
        throw new UnsupportedOperationException();
    }
}
