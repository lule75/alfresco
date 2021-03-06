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
package org.alfresco.repo.content.transform;

import java.io.InputStream;
import java.io.OutputStream;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.util.RecordFormatException;

/**
 * Makes use of the {@link http://jakarta.apache.org/poi/ POI} library to
 * perform conversions from Excel spreadsheets to text (comma separated).
 * <p>
 * While most text extraction from spreadsheets only extract the first sheet of
 * the workbook, the method used here extracts the text from <b>all the sheets</b>.
 * This is more useful, especially when it comes to indexing spreadsheets.
 * <p>
 * In the case where there is only one sheet in the document, the results will be
 * exactly the same as most extractors.  Where there are multiple sheets, the results
 * will differ, but meaningful reimporting of the text document is not possible
 * anyway.
 * 
 * @author Derek Hulley
 */
public class PoiHssfContentTransformer extends AbstractContentTransformer2
{
    /**
     * Error message to delegate to NodeInfoBean
     */
    public static final String WRONG_FORMAT_MESSAGE_ID = "transform.err.format_or_password";
   
    /**
     * Windows carriage return line feed pair.
     */
    private static final String LINE_BREAK = "\r\n";
    private static Log logger = LogFactory.getLog(PoiHssfContentTransformer.class);
    
    /**
     * Currently the only transformation performed is that of text extraction from XLS documents.
     */
    public boolean isTransformable(String sourceMimetype, String targetMimetype, TransformationOptions options)
    {
        if (!MimetypeMap.MIMETYPE_EXCEL.equals(sourceMimetype) ||
                !MimetypeMap.MIMETYPE_TEXT_PLAIN.equals(targetMimetype))
        {
            // only support XLS -> Text
            return false;
        }
        else
        {
            return true;
        }
    }

    public void transformInternal(ContentReader reader, ContentWriter writer,  TransformationOptions options)
            throws Exception
    {
        InputStream is = reader.getContentInputStream();
        OutputStream os = writer.getContentOutputStream();
        String encoding = writer.getEncoding();
        try
        {
            // open the workbook
            HSSFWorkbook workbook = new HSSFWorkbook(is);
            // how many sheets are there?
            int sheetCount = workbook.getNumberOfSheets();
            // transform each sheet
            for (int i = 0; i < sheetCount; i++)
            {
                HSSFSheet sheet = workbook.getSheetAt(i);
                String sheetName = workbook.getSheetName(i);
                writeSheet(os, sheet, encoding);
                // write the sheet name
                PoiHssfContentTransformer.writeString(os, encoding, LINE_BREAK, false);
                PoiHssfContentTransformer.writeString(os, encoding, "End of sheet: " + sheetName, true);
                PoiHssfContentTransformer.writeString(os, encoding, LINE_BREAK, false);
                PoiHssfContentTransformer.writeString(os, encoding, LINE_BREAK, false);
            }
        }
        catch (RecordFormatException ex)
        {
            // Catching specific exception to propagate it to NodeInfoBean
            // to fix issue https://issues.alfresco.com/jira/browse/ETWOTWO-440
           
            logger.error(ex);
            throw new TransformerInfoException(WRONG_FORMAT_MESSAGE_ID, ex);
        }
        finally
        {
            if (is != null)
            {
                try { is.close(); } catch (Throwable e) {}
            }
            if (os != null)
            {
                try { os.close(); } catch (Throwable e) {}
            }
        }
    }
    
    /**
     * Dumps the text from the sheet to the stream in CSV format
     */
    private void writeSheet(OutputStream os, HSSFSheet sheet, String encoding) throws Exception
    {
        int rows = sheet.getLastRowNum();
        // transform each row
        for (int i = 0; i <= rows; i++)
        {
            HSSFRow row = sheet.getRow(i);
            if (row != null)
            {
                writeRow(os, row, encoding);
            }
            // break between rows
            if (i < rows)
            {
                PoiHssfContentTransformer.writeString(os, encoding, LINE_BREAK, false);
            }
        }
    }
    
    private void writeRow(OutputStream os, HSSFRow row, String encoding) throws Exception
    {
        short firstCellNum = row.getFirstCellNum(); 
        short lastCellNum = row.getLastCellNum();
        // pad out to first cell
        for (int i = 0; i < firstCellNum; i++)
        {
            PoiHssfContentTransformer.writeString(os, encoding, ",", false);   // CSV up to first cell
        }
        // write each cell
        for (int i = 0; i <= lastCellNum; i++)
        {
            HSSFCell cell = row.getCell(i);
            if (cell != null)
            {
            	int cellType = cell.getCellType();

            	StringBuilder sb = new StringBuilder(10);
				switch (cellType)
                {
                    case HSSFCell.CELL_TYPE_BLANK:
                        // ignore
                        break;
                    case HSSFCell.CELL_TYPE_BOOLEAN:
                        sb.append(cell.getBooleanCellValue());
                        break;
                    case HSSFCell.CELL_TYPE_ERROR:
                        sb.append("ERROR");
                        break;
                    case HSSFCell.CELL_TYPE_NUMERIC:
                        sb.append(cell.getNumericCellValue());
                        break;
                    case HSSFCell.CELL_TYPE_STRING:
                        sb.append(cell.getStringCellValue());
                        break;
                    case HSSFCell.CELL_TYPE_FORMULA:
                    	final int formulaResultType = cell.getCachedFormulaResultType();
                    	if (HSSFCell.CELL_TYPE_NUMERIC == formulaResultType)
                    	{
                    		sb.append(cell.getNumericCellValue());
                    	}
                    	else if (HSSFCell.CELL_TYPE_STRING == formulaResultType)
                    	{
                    		sb.append(cell.getStringCellValue());
                    	}
                    	else if (HSSFCell.CELL_TYPE_BOOLEAN == formulaResultType)
                    	{
                    		sb.append(cell.getBooleanCellValue());
                    	}
                    	else if (HSSFCell.CELL_TYPE_ERROR == formulaResultType)
                    	{
                    		sb.append(cell.getErrorCellValue());
                    	}
                    	else
                    	{
                    		throw new RuntimeException("Unknown formula result type: " + formulaResultType);
                    	}
                    	break;
                    default:
                        throw new RuntimeException("Unknown HSSF cell type: " + cell);
                }
                String data = sb.toString();
                PoiHssfContentTransformer.writeString(os, encoding, data, true);
            }
            // comma separate if required
            if (i < lastCellNum)
            {
                PoiHssfContentTransformer.writeString(os, encoding, ",", false);
            }
        }
    }
    
    /**
     * Writes the given data to the stream using the encoding specified.  If the encoding
     * is not given, the default <tt>String</tt> to <tt>byte[]</tt> conversion will be
     * used.
     * <p>
     * The given data string will be escaped appropriately.
     * 
     * @param os the stream to write to
     * @param encoding the encoding to use, or null if the default encoding is acceptable
     * @param value the string to write
     * @param isData true if the value represents a human-readable string, false if the
     *      value represents formatting characters, separating characters, etc.
     * @throws Exception
     */
    public static void writeString(OutputStream os, String encoding, String value, boolean isData) throws Exception
    {
        if (value == null)
        {
            // nothing to do
            return;
        }
        int dataLength = value.length();
        if (dataLength == 0)
        {
            // nothing to do
            return;
        }
        
        // escape the string
        StringBuilder sb = new StringBuilder(dataLength + 5);   // slightly longer than the data
        for (int i = 0; i < dataLength; i++)
        {
            char currentChar = value.charAt(i);
            if (currentChar == '\"')         // inverted commas
            {
                sb.append("\"");      // CSV escaping of inverted commas 
            }
            // append the char
            sb.append(currentChar);
        }
        // enclose in inverted commas for safety
        if (isData)
        {
            sb.insert(0, "\"");
            sb.append("\"");
        }
        // escaping complete
        value = sb.toString();
        
        byte[] bytes = null;
        if (encoding == null)
        {
            // use default encoding
            bytes = value.getBytes();
        }
        else
        {
            bytes = value.getBytes(encoding);
        }
        // write to the stream
        os.write(bytes);
        // done
    }
}
