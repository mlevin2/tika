package org.apache.tika.parser.image.xmp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;

import org.apache.jempbox.xmp.XMPMetadata;
import org.apache.jempbox.xmp.XMPSchemaDublinCore;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.DublinCore;
import org.apache.tika.metadata.Metadata;
import org.xml.sax.InputSource;

public class JempboxExtractor {

    private XMPPacketScanner scanner = new XMPPacketScanner();
    
    private Metadata metadata;
    
    // The XMP spec says it must be unicode, but for most file formats it specifies "must be encoded in UTF-8"
    private static final String DEFAULT_XMP_CHARSET = "UTF-8";

    public JempboxExtractor(Metadata metadata) {
        this.metadata = metadata;
    }
    
    public void parse(InputStream file)
            throws IOException, TikaException {
    	
        ByteArrayOutputStream xmpraw = new ByteArrayOutputStream();
        boolean found = scanner.parse(file, xmpraw);
        file.close();
        if (!found) {
            return;
        }
        
        Reader decoded = new InputStreamReader(new ByteArrayInputStream(xmpraw.toByteArray()), DEFAULT_XMP_CHARSET);
        XMPMetadata xmp = XMPMetadata.load(new InputSource(decoded));
        
        XMPSchemaDublinCore dc = xmp.getDublinCoreSchema();
        if (dc != null) {
            if (dc.getTitle() != null) {
                metadata.set(DublinCore.TITLE, dc.getTitle());
            }
            if (dc.getDescription() != null) {
                metadata.set(DublinCore.DESCRIPTION, dc.getDescription());
            }
            if (dc.getCreators() != null && dc.getCreators().size() > 0) {
                metadata.set(DublinCore.CREATOR, joinCreators(dc.getCreators()));
            }
            if (dc.getSubjects() != null && dc.getSubjects().size() > 0) {
                Iterator<String> keywords = dc.getSubjects().iterator();
                while (keywords.hasNext()) {
                    metadata.add(DublinCore.SUBJECT, keywords.next());
                }
                // TODO should we set KEYWORDS too?
                // All tested photo managers set the same in Iptc.Application2.Keywords and Xmp.dc.subject
            }
        }
        
    }

    protected String joinCreators(List<String> creators) {
        if (creators == null || creators.size() == 0) {
            return "";
        }
        if (creators.size() == 1) {
            return creators.get(0);
        }
        StringBuffer c = new StringBuffer();
        for (String s : creators) {
            c.append(", ").append(s);
        }
        return c.substring(2);
    }
}