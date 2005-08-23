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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.transaction.UserTransaction;

import junit.framework.TestCase;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.dictionary.DictionaryDAO;
import org.alfresco.repo.dictionary.M2Model;
import org.alfresco.repo.node.BaseNodeServiceTest;
import org.alfresco.repo.search.QueryParameterDefImpl;
import org.alfresco.repo.search.QueryRegisterComponent;
import org.alfresco.repo.search.impl.lucene.fts.FullTextSearchIndexer;
import org.alfresco.repo.search.results.ChildAssocRefResultSet;
import org.alfresco.repo.search.results.DetachedResultSet;
import org.alfresco.repo.search.transaction.LuceneIndexLock;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyTypeDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.QueryParameter;
import org.alfresco.service.cmr.search.QueryParameterDefinition;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.namespace.DynamicNamespacePrefixResolver;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.ApplicationContextHelper;
import org.alfresco.util.CachingDateFormat;
import org.springframework.context.ApplicationContext;

/**
 * @author andyh
 * 
 */
public class LuceneTest extends TestCase
{
    private static final String TEST_NAMESPACE = "http://www.alfresco.org/test/lucenetest";
    private static final QName ASSOC_TYPE_QNAME = QName.createQName(TEST_NAMESPACE, "assoc");

    private static ApplicationContext ctx = ApplicationContextHelper.getApplicationContext();

    TransactionService transactionService;
    NodeService nodeService;
    DictionaryService dictionaryService;
    LuceneIndexLock luceneIndexLock;
    private NodeRef rootNodeRef;
    private NodeRef n1;
    private NodeRef n2;
    private NodeRef n3;
    private NodeRef n4;
    private NodeRef n5;
    private NodeRef n6;
    private NodeRef n7;
    private NodeRef n8;
    private NodeRef n9;
    private NodeRef n10;
    private NodeRef n11;
    private NodeRef n12;
    private NodeRef n13;
    private NodeRef n14;
    private DictionaryDAO dictionaryDAO;
    private FullTextSearchIndexer luceneFTS;
    private QName testType = QName.createQName(TEST_NAMESPACE, "testType");
    private QName testSuperType = QName.createQName(TEST_NAMESPACE, "testSuperType");
    private QName testAspect = QName.createQName(TEST_NAMESPACE, "testAspect");
    private QName testSuperAspect = QName.createQName(TEST_NAMESPACE, "testSuperAspect");
    private ContentService contentService;
    private QueryRegisterComponent queryRegisterComponent;
    private NamespacePrefixResolver namespacePrefixResolver;
    private LuceneIndexerAndSearcher indexerAndSearcher;

    private ServiceRegistry serviceRegistry;

    public LuceneTest()
    {
        super();
    }

    public void setUp() throws Exception
    {
        nodeService = (NodeService) ctx.getBean("dbNodeService");
        luceneIndexLock = (LuceneIndexLock) ctx.getBean("luceneIndexLock");
        dictionaryService = (DictionaryService) ctx.getBean("dictionaryService");
        dictionaryDAO = (DictionaryDAO) ctx.getBean("dictionaryDAO");
        luceneFTS = (FullTextSearchIndexer) ctx.getBean("LuceneFullTextSearchIndexer");
        contentService = (ContentService) ctx.getBean("contentService");
        queryRegisterComponent = (QueryRegisterComponent) ctx.getBean("queryRegisterComponent");
        namespacePrefixResolver = (NamespacePrefixResolver) ctx.getBean("namespaceService");
        indexerAndSearcher = (LuceneIndexerAndSearcher) ctx.getBean("luceneIndexerAndSearcherFactory");
        transactionService = (TransactionService) ctx.getBean("transactionComponent");
        serviceRegistry = (ServiceRegistry) ctx.getBean(ServiceRegistry.SERVICE_REGISTRY);
        
        queryRegisterComponent.loadQueryCollection("testQueryRegister.xml");

        assertEquals(true, ctx.isSingleton("luceneIndexLock"));
        assertEquals(true, ctx.isSingleton("LuceneFullTextSearchIndexer"));

        UserTransaction tx = transactionService.getUserTransaction();
        tx.begin();
 
        // load in the test model
        ClassLoader cl = BaseNodeServiceTest.class.getClassLoader();
        InputStream modelStream = cl.getResourceAsStream("org/alfresco/repo/search/impl/lucene/LuceneTest_model.xml");
        assertNotNull(modelStream);
        M2Model model = M2Model.createModel(modelStream);
        dictionaryDAO.putModel(model);

        StoreRef storeRef = nodeService.createStore(StoreRef.PROTOCOL_WORKSPACE, "Test_" + System.currentTimeMillis());
        rootNodeRef = nodeService.getRootNode(storeRef);

        n1 = nodeService.createNode(rootNodeRef, ContentModel.ASSOC_CHILDREN, QName.createQName("{namespace}one"), testSuperType).getChildRef();
        nodeService.setProperty(n1, QName.createQName("{namespace}property-1"), "ValueOne");
        n2 = nodeService.createNode(rootNodeRef, ContentModel.ASSOC_CHILDREN, QName.createQName("{namespace}two"), testSuperType).getChildRef();
        nodeService.setProperty(n2, QName.createQName("{namespace}property-1"), "valueone");
        nodeService.setProperty(n2, QName.createQName("{namespace}property-2"), "valuetwo");

        n3 = nodeService.createNode(rootNodeRef, ContentModel.ASSOC_CHILDREN, QName.createQName("{namespace}three"), testSuperType).getChildRef();

        ObjectOutputStream oos;
        try
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(n3);
            oos.close();

            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
            Object o = ois.readObject();
            ois.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (ClassNotFoundException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Map<QName, Serializable> testProperties = new HashMap<QName, Serializable>();
        testProperties.put(QName.createQName(TEST_NAMESPACE, "text-indexed-stored-tokenised-atomic"), "TEXT THAT IS INDEXED STORED AND TOKENISED ATOMICALLY KEYONE");
        testProperties.put(QName.createQName(TEST_NAMESPACE, "text-indexed-stored-tokenised-nonatomic"),
                "TEXT THAT IS INDEXED STORED AND TOKENISED BUT NOT ATOMICALLY KEYTWO");
        testProperties.put(QName.createQName(TEST_NAMESPACE, "int-ista"), Integer.valueOf(1));
        testProperties.put(QName.createQName(TEST_NAMESPACE, "long-ista"), Long.valueOf(2));
        testProperties.put(QName.createQName(TEST_NAMESPACE, "float-ista"), Float.valueOf(3.4f));
        testProperties.put(QName.createQName(TEST_NAMESPACE, "double-ista"), Double.valueOf(5.6));
        testProperties.put(QName.createQName(TEST_NAMESPACE, "date-ista"), new Date());
        testProperties.put(QName.createQName(TEST_NAMESPACE, "datetime-ista"), new Date());
        testProperties.put(QName.createQName(TEST_NAMESPACE, "boolean-ista"), Boolean.valueOf(true));
        testProperties.put(QName.createQName(TEST_NAMESPACE, "qname-ista"), QName.createQName("{wibble}wobble"));
        testProperties.put(QName.createQName(TEST_NAMESPACE, "guid-ista"), "My-GUID");
        testProperties.put(QName.createQName(TEST_NAMESPACE, "category-ista"), "CategoryId");
        testProperties.put(QName.createQName(TEST_NAMESPACE, "noderef-ista"), n1);
        testProperties.put(QName.createQName(TEST_NAMESPACE, "path-ista"), nodeService.getPath(n3));
        testProperties.put(QName.createQName(TEST_NAMESPACE, "null"), null);
        testProperties.put(QName.createQName(TEST_NAMESPACE, "list"), new ArrayList());
        ArrayList<Object> testList = new ArrayList<Object>();
        testList.add(null);
        testProperties.put(QName.createQName(TEST_NAMESPACE, "nullList"), testList);
        ArrayList<Object> testList2 = new ArrayList<Object>();
        testList2.add("woof");
        testList2.add(null);

        n4 = nodeService.createNode(rootNodeRef, ContentModel.ASSOC_CHILDREN, QName.createQName("{namespace}four"), testType, testProperties).getChildRef();

        nodeService.getProperties(n1);
        nodeService.getProperties(n2);
        nodeService.getProperties(n3);
        nodeService.getProperties(n4);

        n5 = nodeService.createNode(n1, ASSOC_TYPE_QNAME, QName.createQName("{namespace}five"), testSuperType).getChildRef();
        n6 = nodeService.createNode(n1, ASSOC_TYPE_QNAME, QName.createQName("{namespace}six"), testSuperType).getChildRef();
        n7 = nodeService.createNode(n2, ASSOC_TYPE_QNAME, QName.createQName("{namespace}seven"), testSuperType).getChildRef();
        n8 = nodeService.createNode(n2, ASSOC_TYPE_QNAME, QName.createQName("{namespace}eight-2"), testSuperType).getChildRef();
        n9 = nodeService.createNode(n5, ASSOC_TYPE_QNAME, QName.createQName("{namespace}nine"), testSuperType).getChildRef();
        n10 = nodeService.createNode(n5, ASSOC_TYPE_QNAME, QName.createQName("{namespace}ten"), testSuperType).getChildRef();
        n11 = nodeService.createNode(n5, ASSOC_TYPE_QNAME, QName.createQName("{namespace}eleven"), testSuperType).getChildRef();
        n12 = nodeService.createNode(n5, ASSOC_TYPE_QNAME, QName.createQName("{namespace}twelve"), testSuperType).getChildRef();
        n13 = nodeService.createNode(n12, ASSOC_TYPE_QNAME, QName.createQName("{namespace}thirteen"), testSuperType).getChildRef();
       

        Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
        properties.put(ContentModel.PROP_MIME_TYPE, "text/plain");
        // properties.put(DictionaryBootstrap.PROP_QNAME_MIME_TYPE,
        // "application/msword");
        properties.put(ContentModel.PROP_ENCODING, "UTF-16");
        n14 = nodeService.createNode(n13, ASSOC_TYPE_QNAME, QName.createQName("{namespace}fourteen"), ContentModel.TYPE_CONTENT, properties).getChildRef();
        //nodeService.addAspect(n14, DictionaryBootstrap.ASPECT_QNAME_CONTENT, properties);

        ContentWriter writer = contentService.getUpdatingWriter(n14);
        // InputStream is =
        // this.getClass().getClassLoader().getResourceAsStream("test.doc");
        // writer.putContent(is);
        writer.putContent("The quick brown fox jumped over the lazy dog");

        nodeService.addChild(rootNodeRef, n8, ContentModel.ASSOC_CHILDREN, QName.createQName("{namespace}eight-0"));
        nodeService.addChild(n1, n8, ASSOC_TYPE_QNAME, QName.createQName("{namespace}eight-1"));
        nodeService.addChild(n2, n13, ASSOC_TYPE_QNAME, QName.createQName("{namespace}link"));

        nodeService.addChild(n1, n14, ASSOC_TYPE_QNAME, QName.createQName("{namespace}common"));
        nodeService.addChild(n2, n14, ASSOC_TYPE_QNAME, QName.createQName("{namespace}common"));
        nodeService.addChild(n5, n14, ASSOC_TYPE_QNAME, QName.createQName("{namespace}common"));
        nodeService.addChild(n6, n14, ASSOC_TYPE_QNAME, QName.createQName("{namespace}common"));
        nodeService.addChild(n12, n14, ASSOC_TYPE_QNAME, QName.createQName("{namespace}common"));
        nodeService.addChild(n13, n14, ASSOC_TYPE_QNAME, QName.createQName("{namespace}common"));
        
        tx.commit();        
    }

    public LuceneTest(String arg0)
    {
        super(arg0);
    }

    
    public void testSort() throws Exception
    {
        UserTransaction tx = transactionService.getUserTransaction();
        tx.begin();
        luceneFTS.pause();
        buildBaseIndex();
        runBaseTests();

        LuceneSearcherImpl searcher = LuceneSearcherImpl.getSearcher(rootNodeRef.getStoreRef(), indexerAndSearcher);
        searcher.setNodeService(nodeService);
        searcher.setDictionaryService(dictionaryService);
        searcher.setNamespacePrefixResolver(getNamespacePrefixReolsver("namespace"));

        SearchParameters sp = new SearchParameters();
        sp.addStore(rootNodeRef.getStoreRef());
        sp.setQuery("lucene", "PATH:\"//.\"");
        sp.addSort("ID", true);
        ResultSet results = searcher.query(sp);

        String current = null;
        for (ResultSetRow row : results)
        {
            String id = row.getNodeRef().getId();

            if (current != null)
            {
                if (current.compareTo(id) > 0)
                {
                    fail();
                }
            }
            current = id;
        }
        results.close();
        
        SearchParameters sp2 = new SearchParameters();
        sp2.addStore(rootNodeRef.getStoreRef());
        sp2.setQuery("lucene", "PATH:\"//.\"");
        sp2.addSort("ID", false);
        results = searcher.query(sp2);

        current = null;
        for (ResultSetRow row : results)
        {
            String id = row.getNodeRef().getId();
            if (current != null)
            {
                if (current.compareTo(id) < 0)
                {
                    fail();
                }
            }
            current = id;
        }
        results.close();

        luceneFTS.resume();
        tx.rollback();
    }
    
    public void test1() throws Exception
    {
        UserTransaction tx = transactionService.getUserTransaction();
        tx.begin();
        luceneFTS.pause();
        buildBaseIndex();
        runBaseTests();
        luceneFTS.resume();
        tx.rollback();
    }

    public void test2() throws Exception
    {
        UserTransaction tx = transactionService.getUserTransaction();
        tx.begin();
        luceneFTS.pause();
        buildBaseIndex();
        runBaseTests();
        luceneFTS.resume();
        tx.rollback();
    }

    public void test3() throws Exception
    {
        UserTransaction tx = transactionService.getUserTransaction();
        tx.begin();
        luceneFTS.pause();
        buildBaseIndex();
        runBaseTests();
        luceneFTS.resume();
        tx.rollback();
    }

    public void test4() throws Exception
    {
        UserTransaction tx = transactionService.getUserTransaction();
        tx.begin();
        luceneFTS.pause();
        buildBaseIndex();

        LuceneSearcherImpl searcher = LuceneSearcherImpl.getSearcher(rootNodeRef.getStoreRef(), indexerAndSearcher);
        searcher.setDictionaryService(dictionaryService);

        ResultSet results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "\\@\\{namespace\\}property\\-2:\"valuetwo\"", null, null);
        results.close();
        luceneFTS.resume();
        tx.rollback();
    }

    public void test5() throws Exception
    {
        UserTransaction tx = transactionService.getUserTransaction();
        tx.begin();
        luceneFTS.pause();
        buildBaseIndex();
        runBaseTests();
        luceneFTS.resume();
        tx.rollback();
    }

    public void test6() throws Exception
    {
        UserTransaction tx = transactionService.getUserTransaction();
        tx.begin();
        luceneFTS.pause();
        buildBaseIndex();
        runBaseTests();
        luceneFTS.resume();
        tx.rollback();
    }

    public void testNoOp() throws Exception
    {
        UserTransaction tx = transactionService.getUserTransaction();
        tx.begin();
        luceneFTS.pause();
        LuceneIndexerImpl indexer = LuceneIndexerImpl.getUpdateIndexer(rootNodeRef.getStoreRef(), "delta" + System.currentTimeMillis() + "_1", indexerAndSearcher);

        indexer.setNodeService(nodeService);
        indexer.setLuceneIndexLock(luceneIndexLock);
        indexer.setDictionaryService(dictionaryService);
        indexer.setLuceneFullTextSearchIndexer(luceneFTS);
        indexer.setContentService(contentService);

        indexer.prepare();
        indexer.commit();
        luceneFTS.resume();
        tx.rollback();
    }

    /**
     * Test basic index and search
     * 
     * @throws InterruptedException
     * 
     */

    public void testStandAloneIndexerCommit() throws Exception
    {
        UserTransaction tx = transactionService.getUserTransaction();
        tx.begin();
        luceneFTS.pause();
        LuceneIndexerImpl indexer = LuceneIndexerImpl.getUpdateIndexer(rootNodeRef.getStoreRef(), "delta" + System.currentTimeMillis() + "_1", indexerAndSearcher);

        indexer.setNodeService(nodeService);
        indexer.setLuceneIndexLock(luceneIndexLock);
        indexer.setDictionaryService(dictionaryService);
        indexer.setLuceneFullTextSearchIndexer(luceneFTS);
        indexer.setContentService(contentService);

        ////indexer.clearIndex();

        indexer.createNode(new ChildAssociationRef(null, null, null, rootNodeRef));
        indexer.createNode(new ChildAssociationRef(ContentModel.ASSOC_CHILDREN, rootNodeRef, QName.createQName("{namespace}one"), n1));
        indexer.createNode(new ChildAssociationRef(ContentModel.ASSOC_CHILDREN, rootNodeRef, QName.createQName("{namespace}two"), n2));
        indexer.updateNode(n1);
        // indexer.deleteNode(new ChildRelationshipRef(rootNode, "path",
        // newNode));

        indexer.prepare();
        indexer.commit();

        LuceneSearcherImpl searcher = LuceneSearcherImpl.getSearcher(rootNodeRef.getStoreRef(), indexerAndSearcher);
        searcher.setNodeService(nodeService);
        searcher.setDictionaryService(dictionaryService);
        searcher.setNamespacePrefixResolver(getNamespacePrefixReolsver("namespace"));

        ResultSet results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "\\@\\{namespace\\}property\\-2:\"valuetwo\"", null, null);
        simpleResultSetTest(results);
        
        ChildAssocRefResultSet r2 = new ChildAssocRefResultSet(nodeService, results.getNodeRefs(), null, false);
        simpleResultSetTest(r2);
        
        ChildAssocRefResultSet r3 = new ChildAssocRefResultSet(nodeService, results.getNodeRefs(), null, true);
        simpleResultSetTest(r3);
        
        ChildAssocRefResultSet r4 = new ChildAssocRefResultSet(nodeService, results.getChildAssocRefs(), null);
        simpleResultSetTest(r4);
        
        DetachedResultSet r5 = new DetachedResultSet(results, null);
        simpleResultSetTest(r5);
        
        DetachedResultSet r6 = new DetachedResultSet(r2, null);
        simpleResultSetTest(r6);
        
        DetachedResultSet r7 = new DetachedResultSet(r3, null);
        simpleResultSetTest(r7);
        
        DetachedResultSet r8 = new DetachedResultSet(r4, null);
        simpleResultSetTest(r8);
        
        DetachedResultSet r9 = new DetachedResultSet(r5, null);
        simpleResultSetTest(r9);
        
        results.close();

        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "\\@\\{namespace\\}property\\-1:\"valueone\"", null, null);
        assertEquals(2, results.length());
        assertEquals(n2.getId(), results.getNodeRef(0).getId());
        assertEquals(n1.getId(), results.getNodeRef(1).getId());
        assertEquals(1.0f, results.getScore(0));
        assertEquals(1.0f, results.getScore(1));
        results.close();

        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "\\@namespace\\:property\\-1:\"valueone\"", null, null);
        assertEquals(2, results.length());
        assertEquals(n2.getId(), results.getNodeRef(0).getId());
        assertEquals(n1.getId(), results.getNodeRef(1).getId());
        assertEquals(1.0f, results.getScore(0));
        assertEquals(1.0f, results.getScore(1));
        results.close();

        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "\\@property\\-1:\"valueone\"", null, null);
        assertEquals(2, results.length());
        assertEquals(n2.getId(), results.getNodeRef(0).getId());
        assertEquals(n1.getId(), results.getNodeRef(1).getId());
        assertEquals(1.0f, results.getScore(0));
        assertEquals(1.0f, results.getScore(1));
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "\\@property\\-1:\"Valueone\"", null, null);
        assertEquals(2, results.length());
        assertEquals(n2.getId(), results.getNodeRef(0).getId());
        assertEquals(n1.getId(), results.getNodeRef(1).getId());
        assertEquals(1.0f, results.getScore(0));
        assertEquals(1.0f, results.getScore(1));
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "\\@property\\-1:ValueOne", null, null);
        assertEquals(2, results.length());
        assertEquals(n2.getId(), results.getNodeRef(0).getId());
        assertEquals(n1.getId(), results.getNodeRef(1).getId());
        assertEquals(1.0f, results.getScore(0));
        assertEquals(1.0f, results.getScore(1));
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "\\@property\\-1:valueone", null, null);
        assertEquals(2, results.length());
        assertEquals(n2.getId(), results.getNodeRef(0).getId());
        assertEquals(n1.getId(), results.getNodeRef(1).getId());
        assertEquals(1.0f, results.getScore(0));
        assertEquals(1.0f, results.getScore(1));
        results.close();
        
        

        QName qname = QName.createQName("", "property-1");

        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "ID:\"" + n1.getId() + "\"", null, null);

        assertEquals(2, results.length());

        results.close();
        luceneFTS.resume();
        tx.rollback();
    }

    private void simpleResultSetTest(ResultSet results)
    {
        assertEquals(1, results.length());
        assertEquals(n2.getId(), results.getNodeRef(0).getId());
        assertEquals(n2, results.getNodeRef(0));
        assertEquals(new ChildAssociationRef(ContentModel.ASSOC_CHILDREN, rootNodeRef, QName.createQName("{namespace}two"), n2), results.getChildAssocRef(0));
        assertEquals(1, results.getChildAssocRefs().size());
        assertNotNull(results.getChildAssocRefs());
        assertEquals(0, results.getRow(0).getIndex());
        assertEquals(1.0f, results.getRow(0).getScore());
        assertEquals(new ChildAssociationRef(ContentModel.ASSOC_CHILDREN, rootNodeRef, QName.createQName("{namespace}two"), n2), results.getRow(0).getChildAssocRef());
        assertEquals(n2, results.getRow(0).getNodeRef());
        assertEquals(QName.createQName("{namespace}two"), results.getRow(0).getQName());
        assertEquals("valuetwo", results.getRow(0).getValue(QName.createQName("{namespace}property-2")));
        for(ResultSetRow row: results)
        {
            assertNotNull(row);
        }
    }

    public void testStandAlonePathIndexer() throws Exception
    {
        UserTransaction tx = transactionService.getUserTransaction();
        tx.begin();
        luceneFTS.pause();
        buildBaseIndex();

        LuceneSearcherImpl searcher = LuceneSearcherImpl.getSearcher(rootNodeRef.getStoreRef(), indexerAndSearcher);
        searcher.setNodeService(nodeService);
        searcher.setDictionaryService(dictionaryService);

        ResultSet results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "@\\{namespace\\}property-1:valueone", null, null);
        try
        {
            assertEquals(2, results.length());
            assertEquals(n1.getId(), results.getNodeRef(0).getId());
            assertEquals(n2.getId(), results.getNodeRef(1).getId());
            //assertEquals(1.0f, results.getScore(0));
            //assertEquals(1.0f, results.getScore(1));

            QName qname = QName.createQName("", "property-1");

        }
        finally
        {
            results.close();
        }

        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "+ID:\"" + n1.getId() + "\"", null, null);
        try
        {
            assertEquals(2, results.length());
        }
        finally
        {
            results.close();
        }

        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "ID:\"" + rootNodeRef.getId() + "\"", null, null);
        try
        {
            assertEquals(1, results.length());
        }
        finally
        {
            results.close();
        }
        luceneFTS.resume();
        tx.rollback();
    }

    private void buildBaseIndex()
    {
        LuceneIndexerImpl indexer = LuceneIndexerImpl.getUpdateIndexer(rootNodeRef.getStoreRef(), "delta" + System.currentTimeMillis() + "_" + (new Random().nextInt()), indexerAndSearcher);
        indexer.setNodeService(nodeService);
        indexer.setLuceneIndexLock(luceneIndexLock);
        indexer.setDictionaryService(dictionaryService);
        indexer.setLuceneFullTextSearchIndexer(luceneFTS);
        indexer.setContentService(contentService);
        //indexer.clearIndex();
        indexer.createNode(new ChildAssociationRef(null, null, null, rootNodeRef));
        indexer.createNode(new ChildAssociationRef(ContentModel.ASSOC_CHILDREN, rootNodeRef, QName.createQName("{namespace}one"), n1));
        indexer.createNode(new ChildAssociationRef(ContentModel.ASSOC_CHILDREN, rootNodeRef, QName.createQName("{namespace}two"), n2));
        indexer.createNode(new ChildAssociationRef(ContentModel.ASSOC_CHILDREN, rootNodeRef, QName.createQName("{namespace}three"), n3));
        indexer.createNode(new ChildAssociationRef(ContentModel.ASSOC_CHILDREN, rootNodeRef, QName.createQName("{namespace}four"), n4));
        indexer.createNode(new ChildAssociationRef(ASSOC_TYPE_QNAME, n1, QName.createQName("{namespace}five"), n5));
        indexer.createNode(new ChildAssociationRef(ASSOC_TYPE_QNAME, n1, QName.createQName("{namespace}six"), n6));
        indexer.createNode(new ChildAssociationRef(ASSOC_TYPE_QNAME, n2, QName.createQName("{namespace}seven"), n7));
        indexer.createNode(new ChildAssociationRef(ASSOC_TYPE_QNAME, n2, QName.createQName("{namespace}eight"), n8));
        indexer.createNode(new ChildAssociationRef(ASSOC_TYPE_QNAME, n5, QName.createQName("{namespace}nine"), n9));
        indexer.createNode(new ChildAssociationRef(ASSOC_TYPE_QNAME, n5, QName.createQName("{namespace}ten"), n10));
        indexer.createNode(new ChildAssociationRef(ASSOC_TYPE_QNAME, n5, QName.createQName("{namespace}eleven"), n11));
        indexer.createNode(new ChildAssociationRef(ASSOC_TYPE_QNAME, n5, QName.createQName("{namespace}twelve"), n12));
        indexer.createNode(new ChildAssociationRef(ASSOC_TYPE_QNAME, n12, QName.createQName("{namespace}thirteen"), n13));
        indexer.createNode(new ChildAssociationRef(ASSOC_TYPE_QNAME, n13, QName.createQName("{namespace}fourteen"), n14));
        indexer.prepare();
        indexer.commit();
    }

    public void testAllPathSearch() throws Exception
    {
        UserTransaction tx = transactionService.getUserTransaction();
        tx.begin();
        luceneFTS.pause();
        buildBaseIndex();

        runBaseTests();
        luceneFTS.resume();
        tx.rollback();
    }

    private void runBaseTests()
    {
        LuceneSearcherImpl searcher = LuceneSearcherImpl.getSearcher(rootNodeRef.getStoreRef(), indexerAndSearcher);
        searcher.setNodeService(nodeService);
        searcher.setDictionaryService(dictionaryService);
        searcher.setNamespacePrefixResolver(getNamespacePrefixReolsver("namespace"));
        searcher.setQueryRegister(queryRegisterComponent);
        ResultSet results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:two\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:three\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:four\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:eight-0\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:five\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:one\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:two\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:two/namespace:one\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:two/namespace:two\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:five\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:six\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:two/namespace:seven\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:eight-1\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:two/namespace:eight-2\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:eight-2\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:two/namespace:eight-1\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:two/namespace:eight-0\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:eight-0\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:five/namespace:nine\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:five/namespace:ten\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:five/namespace:eleven\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:five/namespace:twelve\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:five/namespace:twelve/namespace:thirteen\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:five/namespace:twelve/namespace:thirteen/namespace:fourteen\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:*\"", null, null);
        assertEquals(5, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"/namespace:*/namespace:*\"", null, null);
        assertEquals(8, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:*/namespace:*\"", null, null);
        assertEquals(6, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:*/namespace:five\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"/namespace:*/namespace:*/namespace:*\"", null, null);
        assertEquals(8, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:*/namespace:*/namespace:*\"", null, null);
        assertEquals(5, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:*\"", null, null);
        assertEquals(4, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:*/namespace:five/namespace:*\"", null, null);
        assertEquals(5, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:*/namespace:nine\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/*\"", null, null);
        assertEquals(5, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"/*/*\"", null, null);
        assertEquals(8, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/*/*\"", null, null);
        assertEquals(6, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/*/namespace:five\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"/*/*/*\"", null, null);
        assertEquals(8, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/*/*/*\"", null, null);
        assertEquals(5, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/*\"", null, null);
        assertEquals(4, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/*/namespace:five/*\"", null, null);
        assertEquals(5, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/*/namespace:nine\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"//.\"", null, null);
        assertEquals(26, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"//.\"", null, null);
        assertEquals(15, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"//*\"", null, null);
        assertEquals(14, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"//*\"", null, null);
        assertEquals(25, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"//*/.\"", null, null);
        assertEquals(14, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"//*/.\"", null, null);
        assertEquals(25, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"//*/./.\"", null, null);
        assertEquals(14, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"//*/./.\"", null, null);
        assertEquals(25, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"//./*\"", null, null);
        assertEquals(25, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"//./*\"", null, null);
        assertEquals(14, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"//././*/././.\"", null, null);
        assertEquals(14, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"//././*/././.\"", null, null);
        assertEquals(25, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"//common\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"//common\"", null, null);
        assertEquals(7, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/one//common\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"/one//common\"", null, null);
        assertEquals(5, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/one/five//*\"", null, null);
        assertEquals(6, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"/one/five//*\"", null, null);
        assertEquals(9, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/one/five//.\"", null, null);
        assertEquals(7, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"/one/five//.\"", null, null);
        assertEquals(10, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/one//five/nine\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/one//thirteen/fourteen\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/one//thirteen/fourteen//.\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/one//thirteen/fourteen//.//.\"", null, null);
        assertEquals(1, results.length());
        results.close();

        // Type search tests

        QName qname = QName.createQName(TEST_NAMESPACE, "int-ista");
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "\\@" + escapeQName(qname) + ":\"01\"", null, null);
        assertEquals(1, results.length());
        assertNotNull(results.getRow(0).getValue(qname));
        results.close();

        qname = QName.createQName(TEST_NAMESPACE, "long-ista");
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "\\@" + escapeQName(qname) + ":\"2\"", null, null);
        assertEquals(1, results.length());
        assertNotNull(results.getRow(0).getValue(qname));
        results.close();

        qname = QName.createQName(TEST_NAMESPACE, "float-ista");
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "\\@" + escapeQName(qname) + ":\"3.4\"", null, null);
        assertEquals(1, results.length());
        assertNotNull(results.getRow(0).getValue(qname));
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "\\@"
                +escapeQName(QName.createQName(TEST_NAMESPACE, "double-ista")) + ":\"5.6\"", null, null);
        assertEquals(1, results.length());
        results.close();

        Date date = new Date();
        String sDate = CachingDateFormat.getDateFormat().format(date);
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "\\@"
                +escapeQName(QName.createQName(TEST_NAMESPACE, "date-ista")) + ":\""+sDate+"\"", null, null);
        assertEquals(1, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "\\@"
                +escapeQName(QName.createQName(TEST_NAMESPACE, "datetime-ista")) + ":\""+sDate+"\"", null, null);
        assertEquals(1, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "\\@"
                +escapeQName(QName.createQName(TEST_NAMESPACE, "boolean-ista")) + ":\"true\"", null, null);
        assertEquals(1, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "\\@"
                +escapeQName(QName.createQName(TEST_NAMESPACE, "qname-ista")) + ":\"{wibble}wobble\"", null, null);
        assertEquals(1, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "\\@"
                +escapeQName(QName.createQName(TEST_NAMESPACE, "guid-ista")) + ":\"My-GUID\"", null, null);
        assertEquals(1, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "\\@"
                +escapeQName(QName.createQName(TEST_NAMESPACE, "category-ista")) + ":\"CategoryId\"", null, null);
        assertEquals(1, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "\\@"
                +escapeQName(QName.createQName(TEST_NAMESPACE, "noderef-ista")) + ":\""+n1+"\"", null, null);
        assertEquals(1, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "\\@"
                +escapeQName(QName.createQName(TEST_NAMESPACE, "path-ista")) + ":\""+nodeService.getPath(n3)+"\"", null, null);
        assertEquals(1, results.length());
        assertNotNull(results.getRow(0).getValue(QName.createQName(TEST_NAMESPACE, "path-ista")));
        results.close();

        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "TYPE:\"" + testType.toString() + "\"", null, null);
        assertEquals(1, results.length());
        results.close();

        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "TYPE:\"" + testSuperType.toString() + "\"", null, null);
        assertEquals(13, results.length());
        results.close();

        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "ASPECT:\"" + testAspect.toString() + "\"", null, null);
        assertEquals(1, results.length());
        results.close();

        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "ASPECT:\"" + testSuperAspect.toString() + "\"", null, null);
        assertEquals(1, results.length());
        results.close();

        // FTS test

        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "TEXT:\"fox\"", null, null);
        assertEquals(1, results.length());
        results.close();

        QName queryQName = QName.createQName("alf:test1", namespacePrefixResolver);
        results = searcher.query(rootNodeRef.getStoreRef(), queryQName, null);
        assertEquals(1, results.length());
        results.close();

        // Parameters

        queryQName = QName.createQName("alf:test2", namespacePrefixResolver);
        results = searcher.query(rootNodeRef.getStoreRef(), queryQName, null);
        assertEquals(1, results.length());
        results.close();

        queryQName = QName.createQName("alf:test2", namespacePrefixResolver);
        QueryParameter qp = new QueryParameter(QName.createQName("alf:banana", namespacePrefixResolver), "woof");
        results = searcher.query(rootNodeRef.getStoreRef(), queryQName, new QueryParameter[] { qp });
        assertEquals(0, results.length());
        results.close();

        queryQName = QName.createQName("alf:test3", namespacePrefixResolver);
        qp = new QueryParameter(QName.createQName("alf:banana", namespacePrefixResolver), "/one/five//*");
        results = searcher.query(rootNodeRef.getStoreRef(), queryQName, new QueryParameter[] { qp });
        assertEquals(6, results.length());
        results.close();

        // TODO: should not have a null property type definition
        QueryParameterDefImpl paramDef = new QueryParameterDefImpl(QName.createQName("alf:lemur", namespacePrefixResolver), (PropertyTypeDefinition) null, true, "fox");
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "TEXT:\"${alf:lemur}\"", null, new QueryParameterDefinition[] { paramDef });
        assertEquals(1, results.length());
        results.close();

        paramDef = new QueryParameterDefImpl(QName.createQName("alf:intvalue", namespacePrefixResolver), (PropertyTypeDefinition) null, true, "1");
        qname = QName.createQName(TEST_NAMESPACE, "int-ista");
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "\\@" + escapeQName(qname) + ":\"${alf:intvalue}\"", null, new QueryParameterDefinition[] { paramDef });
        assertEquals(1, results.length());
        assertNotNull(results.getRow(0).getValue(qname));
        results.close();

    }

    public void testPathSearch() throws Exception
    {
        UserTransaction tx = transactionService.getUserTransaction();
        tx.begin();
        luceneFTS.pause();
        buildBaseIndex();

        LuceneSearcherImpl searcher = LuceneSearcherImpl.getSearcher(rootNodeRef.getStoreRef(), indexerAndSearcher);
        searcher.setNodeService(nodeService);
        searcher.setDictionaryService(dictionaryService);
        searcher.setNamespacePrefixResolver(getNamespacePrefixReolsver("namespace"));

        // //*

        ResultSet

        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"//common\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"//common\"", null, null);
        assertEquals(7, results.length());
        results.close();

        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/one//common\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"/one//common\"", null, null);
        assertEquals(5, results.length());
        results.close();

        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/one/five//*\"", null, null);
        assertEquals(6, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"/one/five//*\"", null, null);
        assertEquals(9, results.length());
        results.close();

        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/one/five//.\"", null, null);
        assertEquals(7, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"/one/five//.\"", null, null);
        assertEquals(10, results.length());
        results.close();

        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/one//five/nine\"", null, null);
        assertEquals(1, results.length());
        results.close();

        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/one//thirteen/fourteen\"", null, null);
        assertEquals(1, results.length());
        results.close();
        luceneFTS.resume();
        
        tx.rollback();
    }

    public void testXPathSearch() throws Exception
    {
        UserTransaction tx = transactionService.getUserTransaction();
        tx.begin();
        luceneFTS.pause();
        buildBaseIndex();

        LuceneSearcherImpl searcher = LuceneSearcherImpl.getSearcher(rootNodeRef.getStoreRef(), indexerAndSearcher);
        searcher.setNodeService(nodeService);
        searcher.setDictionaryService(dictionaryService);
        searcher.setNamespacePrefixResolver(getNamespacePrefixReolsver("namespace"));

        // //*

        ResultSet

        results = searcher.query(rootNodeRef.getStoreRef(), "xpath", "//./*", null, null);
        assertEquals(14, results.length());
        results.close();
        luceneFTS.resume();
        
        QueryParameterDefinition paramDef = new QueryParameterDefImpl(QName.createQName("alf:query", namespacePrefixResolver), (PropertyTypeDefinition) null, true, "//./*");
        results = searcher.query(rootNodeRef.getStoreRef(), "xpath", "${alf:query}", null, new QueryParameterDefinition[] { paramDef });
        assertEquals(14, results.length());
        results.close();
        tx.rollback();
    }

    public void testMissingIndex() throws Exception
    {
        UserTransaction tx = transactionService.getUserTransaction();
        tx.begin();
        luceneFTS.pause();
        StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "_missing_");
        LuceneSearcherImpl searcher = LuceneSearcherImpl.getSearcher(storeRef, indexerAndSearcher);
        searcher.setNodeService(nodeService);
        searcher.setDictionaryService(dictionaryService);
        searcher.setNamespacePrefixResolver(getNamespacePrefixReolsver("namespace"));

        // //*

        ResultSet

        results = searcher.query(storeRef, "xpath", "//./*", null, null);
        assertEquals(0, results.length());
        luceneFTS.resume();
        tx.rollback();
    }

    public void testUpdateIndex() throws Exception
    {
        UserTransaction tx = transactionService.getUserTransaction();
        tx.begin();
        luceneFTS.pause();
        buildBaseIndex();

        runBaseTests();

        LuceneIndexerImpl indexer = LuceneIndexerImpl.getUpdateIndexer(rootNodeRef.getStoreRef(), "delta" + System.currentTimeMillis(), indexerAndSearcher);
        indexer.setNodeService(nodeService);
        indexer.setLuceneIndexLock(luceneIndexLock);
        indexer.setDictionaryService(dictionaryService);
        indexer.setLuceneFullTextSearchIndexer(luceneFTS);
        indexer.setContentService(contentService);

        indexer.updateNode(rootNodeRef);
        indexer.updateNode(n1);
        indexer.updateNode(n2);
        indexer.updateNode(n3);
        indexer.updateNode(n4);
        indexer.updateNode(n5);
        indexer.updateNode(n6);
        indexer.updateNode(n7);
        indexer.updateNode(n8);
        indexer.updateNode(n9);
        indexer.updateNode(n10);
        indexer.updateNode(n11);
        indexer.updateNode(n12);
        indexer.updateNode(n13);
        indexer.updateNode(n14);

        indexer.commit();

        runBaseTests();
        luceneFTS.resume();
        tx.rollback();
    }

    public void testDeleteLeaf() throws Exception
    {
        UserTransaction tx = transactionService.getUserTransaction();
        tx.begin();
        luceneFTS.pause();
        buildBaseIndex();
        runBaseTests();

        LuceneIndexerImpl indexer = LuceneIndexerImpl.getUpdateIndexer(rootNodeRef.getStoreRef(), "delta" + System.currentTimeMillis(), indexerAndSearcher);
        indexer.setNodeService(nodeService);
        indexer.setLuceneIndexLock(luceneIndexLock);
        indexer.setDictionaryService(dictionaryService);
        indexer.setLuceneFullTextSearchIndexer(luceneFTS);
        indexer.setContentService(contentService);

        indexer.deleteNode(new ChildAssociationRef(ASSOC_TYPE_QNAME, n13, QName.createQName("{namespace}fourteen"), n14));

        indexer.commit();

        LuceneSearcherImpl searcher = LuceneSearcherImpl.getSearcher(rootNodeRef.getStoreRef(), indexerAndSearcher);
        searcher.setNodeService(nodeService);
        searcher.setDictionaryService(dictionaryService);
        searcher.setNamespacePrefixResolver(getNamespacePrefixReolsver("namespace"));
        ResultSet results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:two\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:three\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:four\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:eight-0\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:five\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:one\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:two\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:two/namespace:one\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:two/namespace:two\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:five\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:six\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:two/namespace:seven\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:eight-1\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:two/namespace:eight-2\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:eight-2\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:two/namespace:eight-1\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:two/namespace:eight-0\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:eight-0\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:five/namespace:nine\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:five/namespace:ten\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:five/namespace:eleven\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:five/namespace:twelve\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:five/namespace:twelve/namespace:thirteen\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:five/namespace:twelve/namespace:thirteen/namespace:fourteen\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:*\"", null, null);
        assertEquals(5, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:*/namespace:*\"", null, null);
        assertEquals(5, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"/namespace:*/namespace:*\"", null, null);
        assertEquals(6, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:*/namespace:five\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:*/namespace:*/namespace:*\"", null, null);
        assertEquals(4, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:*\"", null, null);
        assertEquals(3, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:*/namespace:five/namespace:*\"", null, null);
        assertEquals(4, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:*/namespace:nine\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/*\"", null, null);
        assertEquals(5, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/*/*\"", null, null);
        assertEquals(5, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"/*/*\"", null, null);
        assertEquals(6, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/*/namespace:five\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/*/*/*\"", null, null);
        assertEquals(4, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/*\"", null, null);
        assertEquals(3, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/*/namespace:five/*\"", null, null);
        assertEquals(4, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/*/namespace:nine\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"//.\"", null, null);
        assertEquals(14, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"//.\"", null, null);
        assertEquals(17, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"//*\"", null, null);
        assertEquals(13, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"//*\"", null, null);
        assertEquals(16, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"//*/.\"", null, null);
        assertEquals(13, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"//*/.\"", null, null);
        assertEquals(16, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"//*/./.\"", null, null);
        assertEquals(13, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"//*/./.\"", null, null);
        assertEquals(16, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"//./*\"", null, null);
        assertEquals(13, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"//./*\"", null, null);
        assertEquals(16, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"//././*/././.\"", null, null);
        assertEquals(13, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"//././*/././.\"", null, null);
        assertEquals(16, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"//common\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/one//common\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/one/five//*\"", null, null);
        assertEquals(5, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"/one/five//*\"", null, null);
        assertEquals(5, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/one/five//.\"", null, null);
        assertEquals(6, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/one//five/nine\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/one//thirteen/fourteen\"", null, null);
        assertEquals(0, results.length());
        results.close();
        luceneFTS.resume();
        tx.rollback();
    }

    public void testDeleteContainer() throws Exception
    {
        UserTransaction tx = transactionService.getUserTransaction();
        tx.begin();
        luceneFTS.pause();
        buildBaseIndex();
        runBaseTests();

        LuceneIndexerImpl indexer = LuceneIndexerImpl.getUpdateIndexer(rootNodeRef.getStoreRef(), "delta" + System.currentTimeMillis(), indexerAndSearcher);
        indexer.setNodeService(nodeService);
        indexer.setLuceneIndexLock(luceneIndexLock);
        indexer.setDictionaryService(dictionaryService);
        indexer.setLuceneFullTextSearchIndexer(luceneFTS);
        indexer.setContentService(contentService);

        indexer.deleteNode(new ChildAssociationRef(ASSOC_TYPE_QNAME, n12, QName.createQName("{namespace}thirteen"), n13));

        indexer.commit();

        LuceneSearcherImpl searcher = LuceneSearcherImpl.getSearcher(rootNodeRef.getStoreRef(), indexerAndSearcher);
        searcher.setNodeService(nodeService);
        searcher.setDictionaryService(dictionaryService);
        searcher.setNamespacePrefixResolver(getNamespacePrefixReolsver("namespace"));
        ResultSet results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:two\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:three\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:four\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:eight-0\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:five\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:one\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:two\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:two/namespace:one\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:two/namespace:two\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:five\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:six\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:two/namespace:seven\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:eight-1\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:two/namespace:eight-2\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:eight-2\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:two/namespace:eight-1\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:two/namespace:eight-0\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:eight-0\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:five/namespace:nine\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:five/namespace:ten\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:five/namespace:eleven\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:five/namespace:twelve\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:five/namespace:twelve/namespace:thirteen\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:five/namespace:twelve/namespace:thirteen/namespace:fourteen\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:*\"", null, null);
        assertEquals(5, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"/namespace:*\"", null, null);
        assertEquals(5, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:*/namespace:*\"", null, null);
        assertEquals(4, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"/namespace:*/namespace:*\"", null, null);
        assertEquals(5, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:*/namespace:five\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:*/namespace:*/namespace:*\"", null, null);
        assertEquals(4, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:*\"", null, null);
        assertEquals(3, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:*/namespace:five/namespace:*\"", null, null);
        assertEquals(4, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:*/namespace:nine\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/*\"", null, null);
        assertEquals(5, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/*/*\"", null, null);
        assertEquals(4, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"/*/*\"", null, null);
        assertEquals(5, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/*/namespace:five\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/*/*/*\"", null, null);
        assertEquals(4, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"/*/*/*\"", null, null);
        assertEquals(4, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/*\"", null, null);
        assertEquals(3, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/*/namespace:five/*\"", null, null);
        assertEquals(4, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/*/namespace:nine\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"//.\"", null, null);
        assertEquals(13, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"//.\"", null, null);
        assertEquals(15, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"//*\"", null, null);
        assertEquals(12, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"//*\"", null, null);
        assertEquals(14, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"//*/.\"", null, null);
        assertEquals(12, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"//*/.\"", null, null);
        assertEquals(14, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"//*/./.\"", null, null);
        assertEquals(12, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"//*/./.\"", null, null);
        assertEquals(14, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"//./*\"", null, null);
        assertEquals(12, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"//./*\"", null, null);
        assertEquals(14, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"//././*/././.\"", null, null);
        assertEquals(12, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"//././*/././.\"", null, null);
        assertEquals(14, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"//common\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/one//common\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/one/five//*\"", null, null);
        assertEquals(4, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/one/five//.\"", null, null);
        assertEquals(5, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/one//five/nine\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/one//thirteen/fourteen\"", null, null);
        assertEquals(0, results.length());
        results.close();
        luceneFTS.resume();
        tx.rollback();
    }

    public void testDeleteAndAddReference() throws Exception
    {
        UserTransaction tx = transactionService.getUserTransaction();
        tx.begin();
        luceneFTS.pause();
        buildBaseIndex();
        runBaseTests();

        LuceneIndexerImpl indexer = LuceneIndexerImpl.getUpdateIndexer(rootNodeRef.getStoreRef(), "delta" + System.currentTimeMillis(), indexerAndSearcher);
        indexer.setNodeService(nodeService);
        indexer.setLuceneIndexLock(luceneIndexLock);
        indexer.setDictionaryService(dictionaryService);
        indexer.setLuceneFullTextSearchIndexer(luceneFTS);
        indexer.setContentService(contentService);

        nodeService.removeChild(n2, n13);
        indexer.deleteChildRelationship(new ChildAssociationRef(ASSOC_TYPE_QNAME, n2, QName.createQName("{namespace}link"), n13));

        indexer.commit();
        
        LuceneSearcherImpl searcher = LuceneSearcherImpl.getSearcher(rootNodeRef.getStoreRef(), indexerAndSearcher);
        searcher.setNamespacePrefixResolver(getNamespacePrefixReolsver("namespace"));
        searcher.setNodeService(nodeService);
        searcher.setDictionaryService(dictionaryService);
        ResultSet results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:two\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:three\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:four\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:eight-0\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:five\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:one\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:two\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:two/namespace:one\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:two/namespace:two\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:five\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:six\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:two/namespace:seven\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:eight-1\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:two/namespace:eight-2\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:eight-2\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:two/namespace:eight-1\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:two/namespace:eight-0\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:eight-0\"", null, null);
        assertEquals(0, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:five/namespace:nine\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:five/namespace:ten\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:five/namespace:eleven\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:five/namespace:twelve\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:five/namespace:twelve/namespace:thirteen\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:five/namespace:twelve/namespace:thirteen/namespace:fourteen\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:*\"", null, null);
        assertEquals(5, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:*/namespace:*\"", null, null);
        assertEquals(5, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"/namespace:*/namespace:*\"", null, null);
        assertEquals(7, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:*/namespace:five\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:*/namespace:*/namespace:*\"", null, null);
        assertEquals(5, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"/namespace:*/namespace:*/namespace:*\"", null, null);
        assertEquals(6, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:*\"", null, null);
        assertEquals(4, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:*/namespace:five/namespace:*\"", null, null);
        assertEquals(5, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/namespace:*/namespace:nine\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/*\"", null, null);
        assertEquals(5, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/*/*\"", null, null);
        assertEquals(5, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"/*/*\"", null, null);
        assertEquals(7, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/*/namespace:five\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/*/*/*\"", null, null);
        assertEquals(5, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"/*/*/*\"", null, null);
        assertEquals(6, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/*\"", null, null);
        assertEquals(4, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/*/namespace:five/*\"", null, null);
        assertEquals(5, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/namespace:one/*/namespace:nine\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"//.\"", null, null);
        assertEquals(15, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"//.\"", null, null);
        assertEquals(23, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"//*\"", null, null);
        assertEquals(14, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"//*\"", null, null);
        assertEquals(22, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"//*/.\"", null, null);
        assertEquals(14, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"//*/.\"", null, null);
        assertEquals(22, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"//*/./.\"", null, null);
        assertEquals(14, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"//*/./.\"", null, null);
        assertEquals(22, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"//./*\"", null, null);
        assertEquals(14, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"//./*\"", null, null);
        assertEquals(22, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"//././*/././.\"", null, null);
        assertEquals(14, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"//././*/././.\"", null, null);
        assertEquals(22, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"//common\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"//common\"", null, null);
        assertEquals(6, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/one//common\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"/one//common\"", null, null);
        assertEquals(5, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/one/five//*\"", null, null);
        assertEquals(6, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"/one/five//*\"", null, null);
        assertEquals(9, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/one/five//.\"", null, null);
        assertEquals(7, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"/one/five//.\"", null, null);
        assertEquals(10, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/one//five/nine\"", null, null);
        assertEquals(1, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/one//thirteen/fourteen\"", null, null);
        assertEquals(1, results.length());
        results.close();

        indexer = LuceneIndexerImpl.getUpdateIndexer(rootNodeRef.getStoreRef(), "delta" + System.currentTimeMillis(), indexerAndSearcher);
        indexer.setNodeService(nodeService);
        indexer.setLuceneIndexLock(luceneIndexLock);
        indexer.setDictionaryService(dictionaryService);
        indexer.setLuceneFullTextSearchIndexer(luceneFTS);
        indexer.setContentService(contentService);

        nodeService.addChild(n2, n13, ASSOC_TYPE_QNAME, QName.createQName("{namespace}link"));
        indexer.createChildRelationship(new ChildAssociationRef(ASSOC_TYPE_QNAME, n2, QName.createQName("{namespace}link"), n13));

        indexer.commit();

        runBaseTests();
        luceneFTS.resume();
        tx.rollback();
    }

    public void testRenameReference() throws Exception
    {
        UserTransaction tx = transactionService.getUserTransaction();
        tx.begin();
        luceneFTS.pause();
        buildBaseIndex();
        runBaseTests();

        LuceneSearcherImpl searcher = LuceneSearcherImpl.getSearcher(rootNodeRef.getStoreRef(), indexerAndSearcher);
        searcher.setNodeService(nodeService);
        searcher.setDictionaryService(dictionaryService);
        searcher.setNamespacePrefixResolver(getNamespacePrefixReolsver("namespace"));

        ResultSet results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"//namespace:link//.\"", null, null);
        assertEquals(2, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"//namespace:link//.\"", null, null);
        assertEquals(3, results.length());
        results.close();

        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"//namespace:renamed_link//.\"", null, null);
        assertEquals(0, results.length());
        results.close();

        LuceneIndexerImpl indexer = LuceneIndexerImpl.getUpdateIndexer(rootNodeRef.getStoreRef(), "delta" + System.currentTimeMillis(), indexerAndSearcher);
        indexer.setNodeService(nodeService);
        indexer.setLuceneIndexLock(luceneIndexLock);
        indexer.setDictionaryService(dictionaryService);
        indexer.setLuceneFullTextSearchIndexer(luceneFTS);
        indexer.setContentService(contentService);

        nodeService.removeChild(n2, n13);
        nodeService.addChild(n2, n13, ASSOC_TYPE_QNAME, QName.createQName("{namespace}renamed_link"));

        indexer.updateChildRelationship(
                new ChildAssociationRef(ASSOC_TYPE_QNAME, n2, QName.createQName("namespace", "link"), n13),
                new ChildAssociationRef(ASSOC_TYPE_QNAME, n2, QName.createQName("namespace", "renamed_link"),
                n13));

        indexer.commit();

        runBaseTests();

        searcher = LuceneSearcherImpl.getSearcher(rootNodeRef.getStoreRef(), indexerAndSearcher);
        searcher.setNamespacePrefixResolver(getNamespacePrefixReolsver("namespace"));
        searcher.setDictionaryService(dictionaryService);

        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"//namespace:link//.\"", null, null);
        assertEquals(0, results.length());
        results.close();

        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"//namespace:renamed_link//.\"", null, null);
        assertEquals(2, results.length());
        results.close();
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH_WITH_REPEATS:\"//namespace:renamed_link//.\"", null, null);
        assertEquals(3, results.length());
        results.close();
        luceneFTS.resume();
        tx.rollback();
    }

    public void testDelayIndex() throws Exception
    {
        UserTransaction tx = transactionService.getUserTransaction();
        tx.begin();
        luceneFTS.pause();
        buildBaseIndex();
        runBaseTests();

        LuceneSearcherImpl searcher = LuceneSearcherImpl.getSearcher(rootNodeRef.getStoreRef(), indexerAndSearcher);
        searcher.setNamespacePrefixResolver(getNamespacePrefixReolsver("namespace"));
        searcher.setNodeService(nodeService);
        searcher.setDictionaryService(dictionaryService);

        ResultSet results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "\\@"
                + escapeQName(QName.createQName(TEST_NAMESPACE, "text-indexed-stored-tokenised-atomic")) + ":\"KEYONE\"", null, null);
        assertEquals(1, results.length());
        results.close();

        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "\\@"
                + escapeQName(QName.createQName(TEST_NAMESPACE, "text-indexed-stored-tokenised-nonatomic")) + ":\"KEYTWO\"", null, null);
        assertEquals(0, results.length());
        results.close();

        // Do index

        LuceneIndexerImpl indexer = LuceneIndexerImpl.getUpdateIndexer(rootNodeRef.getStoreRef(), "delta" + System.currentTimeMillis() + "_" + (new Random().nextInt()), indexerAndSearcher);
        indexer.setNodeService(nodeService);
        indexer.setLuceneIndexLock(luceneIndexLock);
        indexer.setDictionaryService(dictionaryService);
        indexer.setLuceneFullTextSearchIndexer(luceneFTS);
        indexer.setContentService(contentService);
        indexer.updateFullTextSearch(1000);
        indexer.prepare();
        indexer.commit();
        
        searcher = LuceneSearcherImpl.getSearcher(rootNodeRef.getStoreRef(), indexerAndSearcher);
        searcher.setNamespacePrefixResolver(getNamespacePrefixReolsver("namespace"));
        searcher.setDictionaryService(dictionaryService);

        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "\\@" + escapeQName(QName.createQName(TEST_NAMESPACE, "text-indexed-stored-tokenised-atomic"))
                + ":\"keyone\"", null, null);
        assertEquals(1, results.length());
        results.close();

        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "\\@"
                + escapeQName(QName.createQName(TEST_NAMESPACE, "text-indexed-stored-tokenised-nonatomic")) + ":\"keytwo\"", null, null);
        assertEquals(1, results.length());
        results.close();

        runBaseTests();
        luceneFTS.resume();
        tx.rollback();
    }

    public void testWaitForIndex() throws Exception
    {
        UserTransaction tx = transactionService.getUserTransaction();
        tx.begin();
        luceneFTS.pause();
        buildBaseIndex();
        runBaseTests();

        LuceneSearcherImpl searcher = LuceneSearcherImpl.getSearcher(rootNodeRef.getStoreRef(), indexerAndSearcher);
        searcher.setNodeService(nodeService);
        searcher.setDictionaryService(dictionaryService);
        searcher.setNamespacePrefixResolver(getNamespacePrefixReolsver("namespace"));

        ResultSet results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "\\@"
                + escapeQName(QName.createQName(TEST_NAMESPACE, "text-indexed-stored-tokenised-atomic")) + ":\"KEYONE\"", null, null);
        assertEquals(1, results.length());
        results.close();

        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "\\@"
                + escapeQName(QName.createQName(TEST_NAMESPACE, "text-indexed-stored-tokenised-nonatomic")) + ":\"KEYTWO\"", null, null);
        assertEquals(0, results.length());
        results.close();

        // Do index

        searcher = LuceneSearcherImpl.getSearcher(rootNodeRef.getStoreRef(), indexerAndSearcher);
        searcher.setNodeService(nodeService);
        searcher.setDictionaryService(dictionaryService);
        searcher.setNamespacePrefixResolver(getNamespacePrefixReolsver("namespace"));

        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "\\@" + escapeQName(QName.createQName(TEST_NAMESPACE, "text-indexed-stored-tokenised-atomic"))
                + ":\"keyone\"", null, null);
        assertEquals(1, results.length());
        results.close();

        luceneFTS.resume();
        //luceneFTS.requiresIndex(rootNodeRef.getStoreRef());
        //luceneFTS.index();
        //luceneFTS.index();
        //luceneFTS.index();
        

        Thread.sleep(35000);

        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "\\@"
                + escapeQName(QName.createQName(TEST_NAMESPACE, "text-indexed-stored-tokenised-nonatomic")) + ":\"keytwo\"", null, null);
        assertEquals(1, results.length());
        results.close();
        
        runBaseTests();
        tx.rollback();
    }

    private String escapeQName(QName qname)
    {
        return LuceneQueryParser.escape(qname.toString());
    }

    public void testForKev() throws Exception
    {
        UserTransaction tx = transactionService.getUserTransaction();
        tx.begin();
        luceneFTS.pause();
        buildBaseIndex();
        runBaseTests();

        LuceneSearcherImpl searcher = LuceneSearcherImpl.getSearcher(rootNodeRef.getStoreRef(), indexerAndSearcher);
        searcher.setNamespacePrefixResolver(getNamespacePrefixReolsver("namespace"));
        searcher.setNodeService(nodeService);
        searcher.setDictionaryService(dictionaryService);

        ResultSet results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PARENT:\"" + rootNodeRef.getId() + "\"", null, null);
        assertEquals(5, results.length());
        results.close();

        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "+PARENT:\"" + rootNodeRef.getId() + "\" +QNAME:\"one\"", null, null);
        assertEquals(1, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "( +TYPE:\"{http://www.alfresco.org/model/content/1.0}linkfile\" +@\\{http\\://www.alfresco.org/model/content/1.0\\}name:\"content woof\") OR  TEXT:\"content\"", null, null);
          
        luceneFTS.resume();
        tx.rollback();
    }

    
    public void testIssueAR47() throws Exception
    {
        // This bug arose from repeated deletes and adds creating empty index segments.
        // Two segements each containing one deletyed entry were merged together producing a single empty entry.
        // This seemed to be bad for lucene - I am not sure why
        
        // So we add something, add and delete someting repeatedly and then check we can still do the search.

        // Running in autocommit against the index
        UserTransaction tx = transactionService.getUserTransaction();
        tx.begin();
        ChildAssociationRef testFind = nodeService.createNode(rootNodeRef, ContentModel.ASSOC_CHILDREN, QName.createQName("{namespace}testFind"), testSuperType);
        tx.commit();
        
        LuceneSearcherImpl searcher = LuceneSearcherImpl.getSearcher(rootNodeRef.getStoreRef(), indexerAndSearcher);
        searcher.setNodeService(nodeService);
        searcher.setDictionaryService(dictionaryService);
        searcher.setNamespacePrefixResolver(getNamespacePrefixReolsver("namespace"));
        searcher.setQueryRegister(queryRegisterComponent);
        
        ResultSet results = searcher.query(rootNodeRef.getStoreRef(),  "lucene", "QNAME:\"namespace:testFind\"");
        assertEquals(1, results.length());
        results.close();
        
        for(int i = 0; i < 100; i++)
        {
           UserTransaction tx1 = transactionService.getUserTransaction();
           tx1.begin();
           ChildAssociationRef test = nodeService.createNode(rootNodeRef, ContentModel.ASSOC_CHILDREN, QName.createQName("{namespace}test"), testSuperType);
           tx1.commit();
           
           UserTransaction tx2 = transactionService.getUserTransaction();
           tx2.begin();
           nodeService.deleteNode(test.getChildRef());
           tx2.commit();
        }
        
        UserTransaction tx3 = transactionService.getUserTransaction();
        tx3.begin();
        results = searcher.query(rootNodeRef.getStoreRef(),  "lucene", "QNAME:\"namespace:testFind\"");
        assertEquals(1, results.length());
        results.close();
        tx3.commit();
    }
    
    // Ignore the following test until implementation is completed
    
    public void testReadAgainstDelta() throws Exception
    {
        UserTransaction tx = transactionService.getUserTransaction();
        tx.begin();
        luceneFTS.pause();
        buildBaseIndex();
        runBaseTests();
        tx.commit();
        
        // Delete
        
        tx = transactionService.getUserTransaction();
        tx.begin();
        
        runBaseTests();
        
        serviceRegistry.getNodeService().deleteNode(n1);
       

        SearchParameters sp = new SearchParameters();
        sp.addStore(rootNodeRef.getStoreRef());
        sp.setQuery("lucene", "PATH:\"//.\"");
        sp.excludeDataInTheCurrentTransaction(false);
        ResultSet results = serviceRegistry.getSearchService().query(sp);
        assertEquals(5, results.length());
        results.close();
        
        sp = new SearchParameters();
        sp.addStore(rootNodeRef.getStoreRef());
        sp.setQuery("lucene", "PATH:\"//.\"");
        sp.excludeDataInTheCurrentTransaction(true);
        results = serviceRegistry.getSearchService().query(sp);
        assertEquals(15, results.length());
        results.close();
        
        tx.rollback();
        
        sp = new SearchParameters();
        sp.addStore(rootNodeRef.getStoreRef());
        sp.setQuery("lucene", "PATH:\"//.\"");
        sp.addSort("ID", true);
        sp.excludeDataInTheCurrentTransaction(false);
        results = serviceRegistry.getSearchService().query(sp);
        assertEquals(15, results.length());
        results.close();
        
        // Create
        
        tx = transactionService.getUserTransaction();
        tx.begin();
        
        runBaseTests();
        
        assertEquals(5, serviceRegistry.getNodeService().getChildAssocs(rootNodeRef).size());
        serviceRegistry.getNodeService().createNode(rootNodeRef, ContentModel.ASSOC_CHILDREN, QName.createQName("{namespace}texas"), testSuperType).getChildRef();
        assertEquals(6, serviceRegistry.getNodeService().getChildAssocs(rootNodeRef).size());
        
        sp = new SearchParameters();
        sp.addStore(rootNodeRef.getStoreRef());
        sp.setQuery("lucene", "PATH:\"//.\"");
        sp.excludeDataInTheCurrentTransaction(false);
        results = serviceRegistry.getSearchService().query(sp);
        assertEquals(16, results.length());
        results.close();
        
        tx.rollback();
        
        sp = new SearchParameters();
        sp.addStore(rootNodeRef.getStoreRef());
        sp.setQuery("lucene", "PATH:\"//.\"");
        sp.addSort("ID", true);
        sp.excludeDataInTheCurrentTransaction(false);
        results = serviceRegistry.getSearchService().query(sp);
        assertEquals(15, results.length());
        results.close();
        
        // update property
        
        tx = transactionService.getUserTransaction();
        tx.begin();
        
        runBaseTests();
        
        sp = new SearchParameters();
        sp.addStore(rootNodeRef.getStoreRef());
        sp.setQuery("lucene", "\\@\\{namespace\\}property\\-1:\"valueone\"");
        sp.addSort("ID", true);
        sp.excludeDataInTheCurrentTransaction(false);
        results = serviceRegistry.getSearchService().query(sp);
      
        assertEquals(2, results.length());
        results.close();
     
        nodeService.setProperty(n1, QName.createQName("{namespace}property-1"), "Different");
        
        sp = new SearchParameters();
        sp.addStore(rootNodeRef.getStoreRef());
        sp.setQuery("lucene", "\\@\\{namespace\\}property\\-1:\"valueone\"");
        sp.addSort("ID", true);
        sp.excludeDataInTheCurrentTransaction(false);
        results = serviceRegistry.getSearchService().query(sp);
      
        assertEquals(1, results.length());
        results.close();
        
        tx.rollback();
        
        sp = new SearchParameters();
        sp.addStore(rootNodeRef.getStoreRef());
        sp.setQuery("lucene", "\\@\\{namespace\\}property\\-1:\"valueone\"");
        sp.excludeDataInTheCurrentTransaction(false);
        sp.addSort("ID", true);
        results = serviceRegistry.getSearchService().query(sp);
      
        assertEquals(2, results.length());
        results.close();
        
        // Add and delete
        

        tx = transactionService.getUserTransaction();
        tx.begin();
        
        runBaseTests();
        
        serviceRegistry.getNodeService().deleteNode(n1);
        serviceRegistry.getNodeService().createNode(rootNodeRef, ContentModel.ASSOC_CHILDREN, QName.createQName("{namespace}texas"), testSuperType).getChildRef();
         

        sp = new SearchParameters();
        sp.addStore(rootNodeRef.getStoreRef());
        sp.setQuery("lucene", "PATH:\"//.\"");
        sp.excludeDataInTheCurrentTransaction(false);
        results = serviceRegistry.getSearchService().query(sp);
        assertEquals(6, results.length());
        results.close();
        
        sp = new SearchParameters();
        sp.addStore(rootNodeRef.getStoreRef());
        sp.setQuery("lucene", "PATH:\"//.\"");
        sp.excludeDataInTheCurrentTransaction(true);
        results = serviceRegistry.getSearchService().query(sp);
        assertEquals(15, results.length());
        results.close();
        
        tx.rollback();
        
        sp = new SearchParameters();
        sp.addStore(rootNodeRef.getStoreRef());
        sp.setQuery("lucene", "PATH:\"//.\"");
        sp.addSort("ID", true);
        sp.excludeDataInTheCurrentTransaction(false);
        results = serviceRegistry.getSearchService().query(sp);
        assertEquals(15, results.length());
        results.close();
        
    }
    
    private void runPerformanceTest(double time, boolean clear)
    {
        LuceneIndexerImpl indexer = LuceneIndexerImpl.getUpdateIndexer(rootNodeRef.getStoreRef(), "delta" + System.currentTimeMillis() + "_" + (new Random().nextInt()), indexerAndSearcher);
        indexer.setNodeService(nodeService);
        indexer.setLuceneIndexLock(luceneIndexLock);
        indexer.setDictionaryService(dictionaryService);
        indexer.setLuceneFullTextSearchIndexer(luceneFTS);
        indexer.setContentService(contentService);
        if (clear)
        {
            //indexer.clearIndex();
        }
        indexer.createNode(new ChildAssociationRef(null, null, null, rootNodeRef));

        long startTime = System.currentTimeMillis();
        int count = 0;
        for (int i = 0; i < 10000000; i++)
        {
            if (i % 10 == 0)
            {
                if (System.currentTimeMillis() - startTime > time)
                {
                    count = i;
                    break;
                }
            }

            QName qname = QName.createQName("{namespace}a_" + i);
            NodeRef ref = nodeService.createNode(rootNodeRef, ASSOC_TYPE_QNAME, qname, ContentModel.TYPE_CONTAINER).getChildRef();
            indexer.createNode(new ChildAssociationRef(ASSOC_TYPE_QNAME, rootNodeRef, qname, ref));

        }
        indexer.commit();
        float delta = ((System.currentTimeMillis() - startTime) / 1000.0f);
        //System.out.println("\tCreated " + count + " in " + delta + "   = " + (count / delta));
    }

    private NamespacePrefixResolver getNamespacePrefixReolsver(String defaultURI)
    {
        DynamicNamespacePrefixResolver nspr = new DynamicNamespacePrefixResolver(null);
        nspr.addDynamicNamespace(NamespaceService.ALFRESCO_PREFIX, NamespaceService.ALFRESCO_URI);
        nspr.addDynamicNamespace(NamespaceService.CONTENT_MODEL_PREFIX, NamespaceService.CONTENT_MODEL_1_0_URI);
        nspr.addDynamicNamespace("namespace", "namespace");
        nspr.addDynamicNamespace("test", TEST_NAMESPACE);
        nspr.addDynamicNamespace(NamespaceService.DEFAULT_PREFIX, defaultURI);
        return nspr;
    }
    
    public static void main(String[] args) throws Exception
    {
        // String guid = GUID.generate();
        // System.out.println("GUID is " + guid + " length is " +
        // guid.length());
        LuceneTest test = new LuceneTest();
        test.setUp();
        //test.testForKev();
        //test.testDeleteContainer();
       
        //test.testReadAgainstDelta();
        
        
        NodeRef targetNode = test.rootNodeRef;
        Path path = test.serviceRegistry.getNodeService().getPath(targetNode);

        SearchParameters sp = new SearchParameters();
        sp.addStore(test.rootNodeRef.getStoreRef());
        sp.setQuery("lucene", "PATH:\"" + path + "//." + "\"");        
        ResultSet results = test.serviceRegistry.getSearchService().query(sp);
        
        results.close();

        
    }
}
