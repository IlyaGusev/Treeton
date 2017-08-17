/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.corpus;

import treeton.prosody.StressDescription;
import treeton.prosody.VerseProcessingUtilities;

import java.io.*;
import java.util.*;

public class SimpleCorpusTextFormatParser {
    class Chunk {
        Chunk(String text, boolean noIndex) {
            this.text = text;
            this.stressDescription = null;
            this.noIndex = noIndex;
        }

        String getText() {
            return text;
        }

        void setText(String text) {
            this.text = text;
        }

        public StressDescription getStressDescription() {
            return stressDescription;
        }

        void setStressDescription(StressDescription stressDescription) {
            this.stressDescription = stressDescription;
        }

        boolean isNoIndex() {
            return noIndex;
        }

        private String text;
        private StressDescription stressDescription;
        private final boolean noIndex;
    }

    SimpleCorpusTextFormatParser(File inputFile) throws IOException {
        FileInputStream fis = new FileInputStream( inputFile );
        inputStreamReader = new InputStreamReader( fis, "UTF-8" );
        currentProperties = new HashMap<>();
        currentChunkText = new StringBuffer(5000);
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
    private List<Chunk> chunks = new ArrayList<>();
    private StringBuffer wholeFragmentText = new StringBuffer();
    private StringBuffer currentChunkText;
    private StringBuffer lastTagText;
    private boolean insideNoindexZone = false;


    boolean nextFragment() throws CorpusException, IOException {
        currentProperties.clear();
        chunks.clear();
        Chunk firstChunk = new Chunk("\r\n", true);
        chunks.add(firstChunk);
        currentChunkText.setLength(0);
        wholeFragmentText.setLength(0);

        if (lastTagText.length() == 0) {
            return false;
        }

        if (lastTagText.indexOf("=") == -1) {
            throw new CorpusException("Wrong format. First tag must contain '=' symbol");
        }

        parseTag();

        boolean headerIsComplete = false;

        int c = skipEndOfLines();

        while (c != -1) {
            if (c == '<') {
                readTag();

                if (headerIsComplete && lastTagText.indexOf("=") >= 0) {
                    break;
                }

                if (lastTagText.indexOf("=") >= 0) {
                    parseTag();
                    c = skipEndOfLines();
                } else {
                    headerIsComplete = true;
                    parseTag();
                    c = inputStreamReader.read();
                }
            } else {
                if (c != ' ' && c != '\n' && c != '\r') {
                    headerIsComplete = true;
                }

                if (c == '|' && !insideNoindexZone) {
                    currentChunkText.append(' ');
                    c = inputStreamReader.read();

                    while (c == ' ' || c == '\r') {
                        c = inputStreamReader.read();
                    }
                    if (c == '\n') {
                        currentChunkText.append((char) c);
                        c = inputStreamReader.read();
                    }
                } else {
                    currentChunkText.append((char) c);
                    if (c == '\n' && !insideNoindexZone) {
                        chunks.add(new Chunk(currentChunkText.toString(), false));
                        currentChunkText.setLength(0);
                    }
                    c = inputStreamReader.read();
                }
            }
        }

        ArrayList<String> formattedStrings = new ArrayList<>();
        for (Chunk chunk : chunks) {
            if (!chunk.isNoIndex()) {
                formattedStrings.add(chunk.getText());
            }
        }
        ArrayList<String> plainStrings = new ArrayList<>();
        ArrayList<StressDescription> stressDescriptions = new ArrayList<>();
        try {
            VerseProcessingUtilities.parseFormattedVerses(formattedStrings, plainStrings, stressDescriptions);
        } catch (Exception e) {
            throw new RuntimeException("Wrong verses format", e);
        }

        for (int i = 0, j=0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            if (!chunk.isNoIndex()) {
                chunk.setStressDescription(stressDescriptions.get(j));
                chunk.setText(plainStrings.get(j++));
            }
            wholeFragmentText.append(chunk.getText());
        }

        return true;
    }

    private void readTag() throws IOException {
        assert lastTagText.length() == 0;
        int c = inputStreamReader.read();

        while( c != -1 && c != '>' ) {
            lastTagText.append( (char)c );
            c = inputStreamReader.read();
        }
    }

    private void parseTag() throws CorpusException {
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
                assert !insideNoindexZone;
                if(currentChunkText.length() > 0) {
                    chunks.add(new Chunk(currentChunkText.toString(), false));
                    currentChunkText.setLength(0);
                }
                insideNoindexZone = true;
            } else if( "/noindex".equals( tagName ) ) {
                if(currentChunkText.length() > 0) {
                    chunks.add(new Chunk(currentChunkText.toString(), true));
                    currentChunkText.setLength(0);
                }
                insideNoindexZone = false;
            } else {
                throw new CorpusException( "Unknown tag detected: " + tagName );
            }
        }

        lastTagText.setLength(0);
    }

    private int skipEndOfLines() throws IOException {
        int c = inputStreamReader.read();

        while( c == '\n' || c == '\r' ) {
            c = inputStreamReader.read();
        }
        return c;
    }

    Map<String,String> getProperties() {
        return currentProperties;
    }

    List<Chunk> getChunks() {
        return chunks;
    }

    public String getWholeFragmentText() {
        return wholeFragmentText.toString();
    }
}
