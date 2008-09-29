package org.basex.api.xqj;

import static org.basex.Text.*;
import static org.basex.util.Token.*;
import java.io.IOException;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.basex.BaseX;
import org.basex.build.Builder;
import org.basex.build.Parser;
import org.basex.core.ProgressException;
import org.basex.io.IO;
import org.basex.util.Token;
import org.basex.util.TokenBuilder;

/**
 * This class parses an XML document via a conventional SAX parser.
 * Would be the easiest solution, but some large file cannot be parsed
 * with the default parser.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-08, ISC License
 * @author Christian Gruen
 */
public final class XMLStreamWrapper extends Parser {
  /** Element counter. */
  int nodes;
  /** XML stream reader. */
  XMLStreamReader reader;

  /**
   * Constructor.
   * @param sr stream reader
   */
  public XMLStreamWrapper(final XMLStreamReader sr) {
    super(IO.DUMMY);
    reader = sr;
  }

  @Override
  public void parse(final Builder builder) throws IOException {
    try {
      builder.startDoc(token(io.name()));
      while(reader.hasNext()) {
        final int kind = reader.next();
        switch(kind) {
          case XMLStreamConstants.START_ELEMENT:
            final int as = reader.getAttributeCount();
            byte[][] atts = null;
            if(as != 0) {
              atts = new byte[as << 1][];
              for(int a = 0; a < as; a++) {
                atts[a << 1] = Token.token(reader.getAttributeLocalName(a));
                atts[(a << 1) + 1] = Token.token(reader.getAttributeValue(a));
              }
            }
            builder.startElem(Token.token(reader.getLocalName()), atts);
            nodes++;
            break;
          case XMLStreamConstants.END_ELEMENT:
            builder.endElem(Token.token(reader.getLocalName()));
            break;
          case XMLStreamConstants.CHARACTERS:
            builder.text(new TokenBuilder(reader.getText()), false);
            nodes++;
            break;
          case XMLStreamConstants.PROCESSING_INSTRUCTION:
            builder.pi(new TokenBuilder(reader.getPITarget() + ' ' +
                reader.getPIData()));
            nodes++;
            break;
          case XMLStreamConstants.COMMENT:
            builder.comment(new TokenBuilder(reader.getText()));
            nodes++;
            break;
          case XMLStreamConstants.END_DOCUMENT:
            break;
          default:
            throw new IOException("Unknown node kind " + kind);
        }
      }
      builder.endDoc();
    } catch(final ProgressException ex) {
      throw ex;
    } catch(final IOException ex) {
      ex.printStackTrace();
      throw ex;
    } catch(final XMLStreamException ex) {
      final IOException ioe = new IOException(ex.getMessage());
      ioe.setStackTrace(ex.getStackTrace());
      throw ioe;
    }
  }

  @Override
  public String head() {
    return PROGCREATE;
  }

  @Override
  public String det() {
    return BaseX.info(NODESPARSED, io.name(), nodes);
  }

  @Override
  public double percent() {
    return (nodes / 1000000d) % 1;
  }
}
