/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.ru.prosody;

import org.apache.log4j.Logger;

import java.util.Map;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.io.*;

public class MeterRecognizerFileImpl implements MeterRecognizer {
    private static final Logger logger = Logger.getLogger(MeterRecognizerFileImpl.class);

    Map<String,MeterType> knownStrings = new HashMap<String, MeterType>();

    public MeterRecognizerFileImpl(File inputFile, String delims) throws IOException {
        FileReader _reader = new FileReader(inputFile);
        BufferedReader reader = new BufferedReader(_reader);

        String s;
        while ((s = reader.readLine()) != null) {
            StringTokenizer tokenizer = new StringTokenizer(s,delims);
            String key = tokenizer.nextToken();
            String value = tokenizer.nextToken().trim();
            MeterType type = null;

            if (value.startsWith("я")) {
                type = MeterType.iambus;
            } else if (value.startsWith("х")) {
                type = MeterType.trocheus;
            } else if (value.startsWith("д")) {
                type = MeterType.dactilus;
            } else if (value.startsWith("ф")) {
                type = MeterType.amphibracheus;
            } else if (value.startsWith("н")) {
                type = MeterType.anapaestus;
            } else {
                logger.warn("Unable to parse meter definition: "+value);
            }

            if (type != null) {
                knownStrings.put(key,type);
            }
        }

        reader.close();
        _reader.close();
    }

    public MeterType recognize(String s) {
        return knownStrings.get(s);
    }
}
