package com.xmlcalabash.extensions;

import com.xmlcalabash.core.XMLCalabash;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.XProcURIResolver;
import net.sf.saxon.s9api.*;
import org.nineml.coffeefilter.InvisibleXml;
import org.nineml.coffeefilter.InvisibleXmlDocument;
import org.nineml.coffeefilter.InvisibleXmlParser;
import org.nineml.coffeefilter.ParserOptions;
import org.nineml.coffeegrinder.trees.Arborist;
import org.nineml.coffeesacks.XPathAxe;
import org.nineml.coffeesacks.XmlForest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 8, 2008
 * Time: 7:44:07 AM
 * To change this template use File | Settings | File Templates.
 */

@XMLCalabash(
        name = "cx:invisible-xml",
        type = "{http://xmlcalabash.com/ns/extensions}invisible-xml")

public class CoffeePress extends DefaultStep {
    private static final String library_xpl = "http://xmlcalabash.com/extension/steps/invisible-xml.xpl";
    private static final String library_uri = "/com/xmlcalabash/extensions/coffeepress/library.xpl";
    private static final QName _default_log_level = new QName("", "default-log-level");
    private static final QName _log_levels = new QName("", "log-levels");
    private static final QName _disable_pragmas = new QName("", "disable-pragmas");
    private static final QName _disablePragmas = new QName("", "disablePragmas");
    private static final QName _enable_pragmas = new QName("", "enable-pragmas");
    private static final QName _enablePragmas = new QName("", "enablePragmas");
    private static final QName _disable_states = new QName("", "disable-states");
    private static final QName _enable_states = new QName("", "enable-states");
    private static final QName _fail_on_error = new QName("", "fail-on-error");
    private static final QName _ixml = new QName("", "ixml");

    private String input = null;
    private final Map<QName, String> params = new HashMap<>();
    private ReadablePipe grammarPipe = null;
    private ReadablePipe source = null;
    private WritablePipe result = null;

    public CoffeePress(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        if ("grammar".equals(port)) {
            grammarPipe = pipe;
        } else {
            source = pipe;
        }
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void setParameter(QName name, RuntimeValue value) {
        params.put(name, value.getString());
    }

    public void reset() {
        result.resetWriter();
    }

    public void run() throws SaxonApiException {
        super.run();

        boolean failOnError = getOption(_fail_on_error, true);

        ParserOptions options = makeParserOptions();

        XdmNode grammarNode = grammarPipe.read();
        XdmNode sourceNode = source.read();

        XdmNode rootElement = grammarNode;
        if (rootElement != null && rootElement.getNodeKind() == XdmNodeKind.DOCUMENT) {
            XdmSequenceIterator<XdmNode> iter = grammarNode.axisIterator(Axis.CHILD);
            while (iter.hasNext()) {
                rootElement = iter.next();
                if (rootElement.getNodeKind() == XdmNodeKind.ELEMENT) {
                    break;
                }
            }
        }

        InvisibleXml invisibleXml = new InvisibleXml(options);
        final InvisibleXmlParser parser;
        if (rootElement == null) {
            parser = invisibleXml.getParser();
        } else {
            if (rootElement.getNodeKind() == XdmNodeKind.ELEMENT && _ixml.equals(rootElement.getNodeName())) {
                // Hack.
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Serializer serializer = runtime.getProcessor().newSerializer(baos);
                serializer.setOutputProperty(Serializer.Property.METHOD, "xml");
                serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes");
                serializer.serializeNode(rootElement);
                ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                try {
                    parser = invisibleXml.getParserFromVxml(bais, grammarNode.getBaseURI().toString());
                } catch (IOException ex) {
                    // this can't happen from a byte array input stream...
                    throw new XProcException(ex);
                }
            } else {
                parser = invisibleXml.getParserFromIxml(grammarNode.getStringValue());
            }
        }


        input = sourceNode.getStringValue();

        if (parser.constructed()) {
            InvisibleXmlDocument doc = parser.parse(sourceNode.getStringValue());

            if (doc.succeeded()) {
                writeTreeResult(parser, doc);
            } else {
                if (failOnError) {
                    throw new XProcException("Failed to parse input");
                }
                writeFailureResult(doc);
            }
        } else {
            InvisibleXmlDocument doc = parser.getFailedParse();
            if (doc.getResult().succeeded()) {
                // Apparently we failed before we even tried to parse the grammar...
                throw new XProcException("Failed to parse Invisible XML grammar");
            }
            writeFailureResult(doc);
        }
   }

   private void writeFailureResult(InvisibleXmlDocument doc) throws SaxonApiException {
       ByteArrayInputStream bais = new ByteArrayInputStream(doc.getTree().getBytes(StandardCharsets.UTF_8));
       SAXSource source = new SAXSource(new InputSource(bais));
       DocumentBuilder builder = runtime.getProcessor().newDocumentBuilder();
       result.write(builder.build(source));
   }

   private void writeTreeResult(InvisibleXmlParser parser, InvisibleXmlDocument doc) throws SaxonApiException {
        final XmlForest forest;
        try {
            forest = new XmlForest(runtime.getProcessor(), doc);
        } catch (SAXException ex) {
            throw new XProcException(ex);
        }

       XPathAxe axe = new XPathAxe(runtime.getProcessor(), parser, forest, input);
       Arborist arborist = doc.getResult().getArborist(axe);
       DocumentBuilder builder = runtime.getProcessor().newDocumentBuilder();
       BuildingContentHandler handler = builder.newBuildingContentHandler();
       arborist.getTree(doc.getAdapter(handler));
       result.write(handler.getDocumentNode());
   }

   private ParserOptions makeParserOptions() {
       Set<String> booleanOptions = new HashSet<>(Arrays.asList(
               "ignore-trailing-whitespace", "ignoreTrailingWhitespace",
               "allow-undefined-symbols", "allowUndefinedSymbols",
               "allow-unreachable-symbols", "allowUnreachableSymbols",
               "allow-unproductive-symbols", "allowUnproductiveSymobls",
               "allow-multiple-definitions", "allowMultipleDefinitions",
               "show-marks", "showMarks",
               "show-bnf-nonterminals", "showBnfNonterminals",
               "suppressAmbiguousState",
               "suppressPrefixState",
               "strict-ambiguity", "strictAmbiguity",
               "ignore-bom",
               "normalize-line-endings",
               "mark-ambiguities",
               "pedantic"
       ));

       Set<String> stringOptions = new HashSet<>(Arrays.asList(
               "parser-type", "parser",
               "priority-style",
               "disable-pragmas",
               "enable-pragmas",
               "disable-states",
               "enable-states",
               "default-log-level",
               "log-levels",
               "start-symbol",
               "format",
               "type"
       ));

       ParserOptions parserOptions = new ParserOptions();
       if (params.containsKey(_default_log_level)) {
           parserOptions.getLogger().setDefaultLogLevel(params.get(_default_log_level));
       }
       if (params.containsKey(_log_levels)) {
           parserOptions.getLogger().setLogLevels(params.get(_log_levels));
       }

       for (QName name : params.keySet()) {
           // This test works for both Saxon 11 and Saxon 12.
           String clarkName = name.getClarkName();
           if (clarkName.startsWith("{}") || !clarkName.startsWith("{")) {
               String key = name.getLocalName();

               final String value = params.get(name);
               final boolean bool;

               if (booleanOptions.contains(key)) {
                   if ("true".equals(value) || "yes".equals(value) || "1".equals(value)) {
                       bool = true;
                   } else if ("false".equals(value) || "no".equals(value) || "0".equals(value)) {
                       bool = false;
                   } else {
                       parserOptions.getLogger().warn("XMLCalabash", "Ignoring invalid option value: %s=%s", key, value);
                       continue;
                   }
               } else {
                   bool = false; // irrelevant but makes the compiler happy
                   if (!stringOptions.contains(key)) {
                       parserOptions.getLogger().warn("XMLCalabash", "Ignoring invalid option: %s=%s", key, value);
                       continue;
                   }
               }

               switch (key) {
                   case "ignore-trailing-whitespace":
                   case "ignoreTrailingWhitespace":
                       parserOptions.setIgnoreTrailingWhitespace(bool);
                       break;
                   case "allow-undefined-symbols":
                   case "allowUndefinedSymbols":
                       parserOptions.setAllowUndefinedSymbols(bool);
                       break;
                   case "allow-unreachable-symbols":
                   case "allowUnreachableSymbols":
                       parserOptions.setAllowUnreachableSymbols(bool);
                       break;
                   case "allow-unproductive-symbols":
                   case "allowUnproductiveSymbols":
                       parserOptions.setAllowUnproductiveSymbols(bool);
                       break;
                   case "allow-multiple-definitions":
                   case "allowMultipleDefinitions":
                       parserOptions.setAllowMultipleDefinitions(bool);
                       break;
                   case "show-marks":
                   case "showMarks":
                       parserOptions.setShowMarks(bool);
                       break;
                   case "show-bnf-nonterminals":
                   case "showBnfNonterminals":
                       parserOptions.setShowBnfNonterminals(bool);
                       break;
                   case "suppressAmbiguousState":
                       if (bool) {
                           parserOptions.suppressState("ambiguous");
                       } else {
                           parserOptions.exposeState("ambiguous");
                       }
                       break;
                   case "suppressPrefixState":
                       if (bool) {
                           parserOptions.suppressState("prefix");
                       } else {
                           parserOptions.exposeState("prefix");
                       }
                       break;
                   case "strict-ambiguity":
                   case "strictAmbiguity":
                       parserOptions.setStrictAmbiguity(bool);
                       break;
                   case "ignore-bom":
                       parserOptions.setIgnoreBOM(bool);
                       break;
                   case "normalize-line-endings":
                       parserOptions.setNormalizeLineEndings(bool);
                       break;
                   case "mark-ambiguities":
                       parserOptions.setMarkAmbiguities(bool);
                       break;
                   case "pedantic":
                       parserOptions.setPedantic(bool);
                       break;
                   case "parser-type":
                   case "parser":
                       parserOptions.setParserType(value);
                       break;
                   case "priority-style":
                       parserOptions.setPriorityStyle(value);
                       break;
                   case "start-symbol":
                       parserOptions.setStartSymbol(value);
                       break;
                   case "disable-pragmas":
                   case "disablePragmas":
                       break; // see below
                   case "enable-pragmas":
                   case "enablePragmas":
                       break; // see below
                   case "disable-states":
                   case "enable-states":
                       break; // see below
                   case "default-log-level":
                       break; // see above
                   case "log-levels":
                       break; // see above
                   case "format":
                       if (!"xml".equals(value)) {
                           parserOptions.getLogger().warn("XMLCalabash", "Ignoring non-XML format option: %s", value);
                       }
                       break;
                   case "type":
                       Set<String> types = new HashSet<>(Arrays.asList("ixml", "xml", "vxml"));
                       if (!types.contains(value)) {
                           parserOptions.getLogger().warn("XMLCalabash", "Ignoring invalid input format: %s", value);
                       }

                       break;
                   default:
                       parserOptions.getLogger().warn("XMLCalabash", "Ignoring unexpected option: %s", key);
               }

           }
       }

       // Disable first,
       String value = params.getOrDefault(_disable_pragmas, params.getOrDefault(_disablePragmas, null));
       if (value != null) {
           for (String name : value.split(",\\s*")) {
               parserOptions.disablePragma(name.trim());
           }
       }
       // then enable
       value = params.getOrDefault(_enable_pragmas, params.getOrDefault(_enablePragmas, null));
       if (value != null) {
           for (String name : value.split(",\\s*")) {
               parserOptions.enablePragma(name.trim());
           }
       }

       // Disable first,
       value = params.getOrDefault(_disable_states, null);
       if (value != null) {
           for (String name : value.split(",\\s*")) {
               parserOptions.suppressState(name.trim());
           }
       }
       // then enable
       value = params.getOrDefault(_enable_states, null);
       if (value != null) {
           for (String name : value.split(",\\s*")) {
               parserOptions.exposeState(name.trim());
           }
       }

       return parserOptions;
   }

    public static void configureStep(XProcRuntime runtime) {
        XProcURIResolver resolver = runtime.getResolver();
        URIResolver uriResolver = resolver.getUnderlyingURIResolver();
        URIResolver myResolver = new StepResolver(uriResolver);
        resolver.setUnderlyingURIResolver(myResolver);
    }

    private static class StepResolver implements URIResolver {
        Logger logger = LoggerFactory.getLogger(CoffeePress.class);
        URIResolver nextResolver = null;

        public StepResolver(URIResolver next) {
            nextResolver = next;
        }

        @Override
        public Source resolve(String href, String base) throws TransformerException {
            try {
                URI baseURI = new URI(base);
                URI xpl = baseURI.resolve(href);
                if (library_xpl.equals(xpl.toASCIIString())) {
                    URL url = CoffeePress.class.getResource(library_uri);
                    logger.debug("Reading library.xpl for cx:ditaa from " + url);
                    InputStream s = CoffeePress.class.getResourceAsStream(library_uri);
                    if (s != null) {
                        SAXSource source = new SAXSource(new InputSource(s));
                        return source;
                    } else {
                        logger.info("Failed to read " + library_uri + " for cx:ditaa");
                    }
                }
            } catch (URISyntaxException e) {
                // nevermind
            }

            if (nextResolver != null) {
                return nextResolver.resolve(href, base);
            } else {
                return null;
            }
        }
    }
}