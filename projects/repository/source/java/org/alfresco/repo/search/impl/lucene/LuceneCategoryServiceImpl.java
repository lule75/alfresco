/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 * You may obtain a copy of the License at
 *
 *     http://www.gnu.org/licenses/lgpl.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.repo.search.impl.lucene;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.IndexerException;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.CategoryService;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.QName;

public class LuceneCategoryServiceImpl implements CategoryService
{
    private NodeService nodeService;

    private NamespacePrefixResolver namespacePrefixResolver;

    private DictionaryService dictionaryService;

    private LuceneIndexerAndSearcher indexerAndSearcher;

    public LuceneCategoryServiceImpl()
    {
        super();
    }

    // Inversion of control support

    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    public void setNamespacePrefixResolver(NamespacePrefixResolver namespacePrefixResolver)
    {
        this.namespacePrefixResolver = namespacePrefixResolver;
    }

    public void setDictionaryService(DictionaryService dictionaryService)
    {
        this.dictionaryService = dictionaryService;
    }

    public void setIndexerAndSearcher(LuceneIndexerAndSearcher indexerAndSearcher)
    {
        this.indexerAndSearcher = indexerAndSearcher;
    }

    public Collection<ChildAssociationRef> getChildren(NodeRef categoryRef, Mode mode, Depth depth)
    {
        if (categoryRef == null)
        {
            return Collections.<ChildAssociationRef> emptyList();
        }
        ResultSet resultSet = null;
        try
        {
            StringBuilder luceneQuery = new StringBuilder(64);

            if (!mode.equals(Mode.ALL))
            {
                luceneQuery.append(mode.equals(Mode.SUB_CATEGORIES) ? "-" : "").append("PATH_WITH_REPEATS:\"");
                luceneQuery.append(buildXPath(nodeService.getPath(categoryRef))).append("/");
                if (depth.equals(Depth.ANY))
                {
                    luceneQuery.append("/");
                }
                luceneQuery.append("member").append("\" ");
            }

            if (!mode.equals(Mode.MEMBERS))
            {
                luceneQuery.append("PATH_WITH_REPEATS:\"");
                luceneQuery.append(buildXPath(nodeService.getPath(categoryRef))).append("/");
                if (depth.equals(Depth.ANY))
                {
                    luceneQuery.append("/");
                }
                luceneQuery.append("*").append("\" ");
            }

            resultSet = indexerAndSearcher.getSearcher(categoryRef.getStoreRef(), false).query(categoryRef.getStoreRef(), "lucene", luceneQuery.toString(), null, null);

            return resultSetToChildAssocCollection(resultSet);
        }
        finally
        {
            if (resultSet != null)
            {
                resultSet.close();
            }
        }
    }

    private String buildXPath(Path path)
    {
        StringBuilder pathBuffer = new StringBuilder(64);
        for (Iterator<Path.Element> elit = path.iterator(); elit.hasNext(); /**/)
        {
            Path.Element element = elit.next();
            if (!(element instanceof Path.ChildAssocElement))
            {
                throw new IndexerException("Confused path: " + path);
            }
            Path.ChildAssocElement cae = (Path.ChildAssocElement) element;
            if (cae.getRef().getParentRef() != null)
            {
                pathBuffer.append("/");
                pathBuffer.append(getPrefix(cae.getRef().getQName().getNamespaceURI()));
                pathBuffer.append(cae.getRef().getQName().getLocalName());
            }
        }
        return pathBuffer.toString();
    }

    HashMap<String, String> prefixLookup = new HashMap<String, String>();

    private String getPrefix(String uri)
    {
        String prefix = prefixLookup.get(uri);
        if (prefix == null)
        {
            Collection<String> prefixes = namespacePrefixResolver.getPrefixes(uri);
            for (String first : prefixes)
            {
                prefix = first;
                break;
            }

            prefixLookup.put(uri, prefix);
        }
        if (prefix == null)
        {
            return "";
        }
        else
        {
            return prefix + ":";
        }

    }

    private Collection<ChildAssociationRef> resultSetToChildAssocCollection(ResultSet resultSet)
    {
        List<ChildAssociationRef> collection = new ArrayList<ChildAssociationRef>();
        if (resultSet != null)
        {
            for (ResultSetRow row : resultSet)
            {
                ChildAssociationRef car = nodeService.getPrimaryParent(row.getNodeRef());
                collection.add(car);
            }
        }
        return collection;
        // The caller closes the result set
    }

    public Collection<ChildAssociationRef> getCategories(StoreRef storeRef, QName attributeQName, Depth depth)
    {
        QName qname = dictionaryService.getProperty(attributeQName).getContainerClass().getName();
        return getChildren(getCategoryRootNode(storeRef, qname), Mode.SUB_CATEGORIES, depth);
    }

    private NodeRef getCategoryRootNode(StoreRef storeRef, QName qname)
    {
        ResultSet resultSet = null;
        try
        {
            resultSet = indexerAndSearcher.getSearcher(storeRef, false).query(storeRef, "lucene", "PATH_WITH_REPEATS:\"/" + getPrefix(qname.getNamespaceURI()) + qname.getLocalName() + "\"",
                    null, null);

            if (resultSet.length() != 1)
            {
                return null;
            }
            else
            {
                return resultSet.getNodeRef(0);
            }
        }
        finally
        {
            if (resultSet != null)
            {
                resultSet.close();
            }
        }
    }

    public Collection<ChildAssociationRef> getRootCategories(StoreRef storeRef)
    {
        ResultSet resultSet = null;
        try
        {
            resultSet = indexerAndSearcher.getSearcher(storeRef, false).query(storeRef, "lucene", "PATH_WITH_REPEATS:\"//alf:categoryRoot/*\"", null, null);
            return resultSetToChildAssocCollection(resultSet);
        }
        finally
        {
            if (resultSet != null)
            {
                resultSet.close();
            }
        }
    }

    public Collection<QName> getCategoryAspects()
    {
        List<QName> list = new ArrayList<QName>();
        for (QName aspect : dictionaryService.getAllAspects())
        {
            if (dictionaryService.isSubClass(aspect, ContentModel.ASPECT_CLASSIFIABLE))
            {
                list.add(aspect);
            }
        }
        return list;
    }

    public NodeRef newCategory(QName typeName, String attributeName)
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

}
