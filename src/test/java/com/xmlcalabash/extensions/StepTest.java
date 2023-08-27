package com.xmlcalabash.extensions;

import com.xmlcalabash.core.XProcConfiguration;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.runtime.XPipeline;
import net.sf.saxon.s9api.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.fail;

public class StepTest {
    private static Processor saxon;
    private static XProcConfiguration config;

    @BeforeAll
    public static void setup() {
        saxon = new Processor(false);
        config = new XProcConfiguration(saxon);
    }

    private String runPipeline(XdmNode pipedoc) throws SaxonApiException {
        XProcRuntime runtime = new XProcRuntime(config);
        XPipeline pipeline = runtime.use(pipedoc);
        pipeline.run();

        ReadablePipe output = pipeline.readFrom("result");
        XdmNode result = output.read();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Serializer serializer = saxon.newSerializer(baos);
        serializer.setOutputProperty(Serializer.Property.METHOD, "xml");
        serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes");
        serializer.serializeNode(result);
        return baos.toString(StandardCharsets.UTF_8);
    }

    @Test
    public void testInlineIXML() {
        try {
            DocumentBuilder builder = saxon.newDocumentBuilder();
            XdmNode pipedoc = builder.build(new File("src/test/resources/inline-ixml.xpl"));
            String xml = runPipeline(pipedoc);
            Assertions.assertEquals("<text><sentence><word>this</word><word>is</word><word>a</word><word>test</word></sentence></text>", xml);
        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
        }
    }

    @Test
    public void testIXML() {
        try {
            DocumentBuilder builder = saxon.newDocumentBuilder();
            XdmNode pipedoc = builder.build(new File("src/test/resources/file-ixml.xpl"));
            String xml = runPipeline(pipedoc);
            Assertions.assertEquals("<text><sentence><word>this</word><word>is</word><word>a</word><word>test</word></sentence></text>", xml);
        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
        }
    }

    @Test
    public void testVXML() {
        try {
            DocumentBuilder builder = saxon.newDocumentBuilder();
            XdmNode pipedoc = builder.build(new File("src/test/resources/file-vxml.xpl"));
            String xml = runPipeline(pipedoc);
            Assertions.assertEquals("<text><sentence><word>this</word><word>is</word><word>a</word><word>test</word></sentence></text>", xml);
        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
        }
    }

    @Test
    public void testFailOnError() {
        try {
            DocumentBuilder builder = saxon.newDocumentBuilder();
            XdmNode pipedoc = builder.build(new File("src/test/resources/fail-on-error.xpl"));
            String xml = runPipeline(pipedoc);
            fail();
        } catch (Exception ex) {
            Assertions.assertTrue(ex.getMessage().contains("Failed to parse input"));
        }
    }

    @Test
    public void testPassOnError() {
        try {
            DocumentBuilder builder = saxon.newDocumentBuilder();
            XdmNode pipedoc = builder.build(new File("src/test/resources/pass-on-error.xpl"));
            String xml = runPipeline(pipedoc);
            Assertions.assertTrue(xml.startsWith("<failed "));
        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
        }
    }

    @Test
    public void testNoGrammar() {
        try {
            DocumentBuilder builder = saxon.newDocumentBuilder();
            XdmNode pipedoc = builder.build(new File("src/test/resources/no-grammar.xpl"));
            String xml = runPipeline(pipedoc);
            Assertions.assertTrue(xml.startsWith("<ixml>"));
        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
        }
    }

}
