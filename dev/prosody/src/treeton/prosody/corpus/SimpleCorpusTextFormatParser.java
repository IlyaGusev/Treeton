/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.corpus;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SimpleCorpusTextFormatParser {
    public SimpleCorpusTextFormatParser(File inputFile) throws IOException {
        FileInputStream fis = new FileInputStream( inputFile );
        inputStreamReader = new InputStreamReader( fis, "UTF-8" );
        currentProperties = new HashMap<String, String>();
        currentNoindexZones = new ArrayList<SimpleSpan>();
        currentFragmentText = new StringBuffer(5000);
        lastTagText = new StringBuffer(100);

        int c = (char) inputStreamReader.read();
        while( c != -1 ) {
            if( '<' == c ) {
                readTag();
                break;
            }
            c = inputStreamReader.read();
        }
    }

    private Reader inputStreamReader;
    private Map<String,String> currentProperties;
    private StringBuffer currentFragmentText;
    private StringBuffer lastTagText;
    private int currentNoindexZoneStart = -1;

    class SimpleSpan {
        SimpleSpan(int start, int end) {
            this.start = start;
            this.end = end;
        }

        int start;
        int end;
    }

    private Collection<SimpleSpan> currentNoindexZones;

    boolean nextFragment() throws CorpusException, IOException {
        currentProperties.clear();
        currentNoindexZones.clear();
        currentFragmentText.setLength(0);
        currentFragmentText.append('\r');
        currentFragmentText.append('\n');

        if( lastTagText.length() == 0 ) {
            return false;
        }

        if( lastTagText.indexOf("=") == -1 ) {
            throw new CorpusException("Wrong format. First tag must contain '=' symbol");
        }

        parseTag();

        boolean headerIsComplete = false;

        int c = skipEndOfLines();

        while( c != -1 ) {
            if( c == '<' ) {
                readTag();

                if( headerIsComplete && lastTagText.indexOf("=") >= 0 ) {
                    break;
                }

                if( lastTagText.indexOf("=") >= 0 ) {
                    if( headerIsComplete ) {
                        break;
                    }

                    parseTag();
                    c = skipEndOfLines();
                } else {
                    headerIsComplete = true;
                    parseTag();
                    c = inputStreamReader.read();
                }
            } else {
                if( c != ' ' && c !='\n' && c != '\r') {
                    headerIsComplete = true;
                }
                currentFragmentText.append( (char)c );
                c = inputStreamReader.read();
            }
        }

        return true;
    }

    void readTag() throws IOException {
        assert lastTagText.length() == 0;
        int c = inputStreamReader.read();

        while( c != -1 && c != '>' ) {
            lastTagText.append( (char)c );
            c = inputStreamReader.read();
        }
    }

    void parseTag() throws CorpusException {
        int equalsPlace = lastTagText.indexOf("=");
        if( equalsPlace != -1 ) {
            String attrName = lastTagText.substring( 0, equalsPlace ).trim();
            String attrValue = lastTagText.substring( equalsPlace + 1).trim();

            if( currentProperties.containsKey(attrName) ) {
                throw new CorpusException( "Duplicate attribute mention detected: " + attrName );
            }

            currentProperties.put( attrName, attrValue );
        } else {
            String tagName = lastTagText.toString().trim();

            if( "noindex".equals( tagName ) ) {
                assert currentNoindexZoneStart == -1;
                currentNoindexZoneStart = currentFragmentText.length();
            } else if( "/noindex".equals( tagName ) ) {
                currentNoindexZones.add( new SimpleSpan( currentNoindexZoneStart, currentFragmentText.length()));
                currentNoindexZoneStart = -1;
            } else {
                throw new CorpusException( "Unknown tag detected: " + tagName );
            }
        }

        lastTagText.setLength(0);
    }

    int skipEndOfLines() throws IOException {
        int c = inputStreamReader.read();

        while( c == '\n' || c == '\r' ) {
            c = inputStreamReader.read();
        }
        return c;
    }

    Map<String,String> getProperties() {
        return currentProperties;
    }

    public Collection<SimpleSpan> getNoindexZones() {
        return currentNoindexZones;
    }

    public String getText() {
        return currentFragmentText.toString();
    }
}
