/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.vfny.geoserver.global.GeoserverDataDirectory;
import org.w3c.dom.Document;

public class StyleEditPageTest extends GeoServerWicketTestSupport {
    
    StyleInfo buildingsStyle;

    @Before
    public void setUp() throws Exception {
        login();
        buildingsStyle = getCatalog().getStyleByName(MockData.BUILDINGS.getLocalPart());
        if(buildingsStyle == null) {
            // undo the rename performed in one of the test methods
            StyleInfo si = getCatalog().getStyleByName("BuildingsNew");
            if(si != null) {
                si.setName(MockData.BUILDINGS.getLocalPart());
                getCatalog().save(si);
            }
            buildingsStyle = getCatalog().getStyleByName(MockData.BUILDINGS.getLocalPart());
        }
        StyleEditPage edit = new StyleEditPage(buildingsStyle);
        tester.startPage(edit);
    }

    @Test
    public void testLoad() throws Exception {
        tester.assertRenderedPage(StyleEditPage.class);
        tester.assertNoErrorMessage();
        
        tester.assertComponent("form:name", TextField.class);
        tester.assertComponent("form:SLD:editorContainer:editor", TextArea.class);
        
        tester.assertModelValue("form:name", "Buildings");

        File styleFile = GeoserverDataDirectory.findStyleFile( buildingsStyle.getFilename() );
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document d1 = db.parse( new FileInputStream(styleFile) );

        //GEOS-3257, actually drag into xml and compare with xmlunit to avoid 
        // line ending problems
        String xml = tester.getComponentFromLastRenderedPage("form:SLD").getDefaultModelObjectAsString();
        xml = xml.replaceAll("&lt;","<").replaceAll("&gt;",">").replaceAll("&quot;", "\"");
        Document d2 = db.parse( new ByteArrayInputStream(xml
            .getBytes()));

        assertXMLEqual(d1, d2);
    }
    
    @Test
    public void testMissingName() throws Exception {
        FormTester form = tester.newFormTester("form");
        form.setValue("name", "");
        form.submit();
        
        tester.assertRenderedPage(StyleEditPage.class);
        tester.assertErrorMessages(new String[] {"Field 'Name' is required."});
    }

    @Test
    public void testChangeName() throws Exception {
        FormTester form = tester.newFormTester("form");
        form.setValue("name", "BuildingsNew");
        form.submit();
        
        assertNull(getCatalog().getStyleByName("Buildings"));
        assertNotNull(getCatalog().getStyleByName("BuildingsNew"));
    }
}
