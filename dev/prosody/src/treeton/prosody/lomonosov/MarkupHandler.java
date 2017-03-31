/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.lomonosov;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import treeton.core.*;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.model.TrnTypeStorage;
import treeton.core.scape.ParseException;
import treeton.core.scape.trnmapper.TrnMapperRule;
import treeton.core.scape.trnmapper.TrnMapperRuleStorage;

import java.io.IOException;
import java.util.*;

public class MarkupHandler extends DefaultHandler {
    private TrnMapperRuleStorage trnMapperRuleStorage;

    private Logger logger = Logger.getLogger(MarkupHandler.class);
    private StringBuffer text = new StringBuffer();
    private TreenotationStorage tStorage;
    private BlackBoard localBoard = TreetonFactory.newBlackBoard(100,false);
    private List<Map<String,String>> glossList = new ArrayList<Map<String, String>>();

    private TrnType atomicType;
    private TrnType sentenceType;
    private TrnType tokenType;
    private TrnType glossType;
    private int kindFeature;
    private TString punctuationValue = TreetonFactory.newTString("punctuation");

    private Token beforeLastWordStart;
    private Token beforeLastSentenceStart;

    public MarkupHandler(TreenotationStorage tStorage, String mapperPath) {
        this.tStorage = tStorage;
        TrnTypeStorage types = tStorage.getTypes();
        try {
            atomicType = types.get("Atom");
            tokenType = types.get("Token");
            kindFeature = tokenType.getFeatureIndex("kind");
            sentenceType = types.get("Sentence");
            glossType = types.get("gloss");
        } catch (TreetonModelException e) {
            logger.error(e);
        }

        if (mapperPath != null) {
            trnMapperRuleStorage = new TrnMapperRuleStorage(types,types);
            try {
                trnMapperRuleStorage.readInFromFile(mapperPath);
            } catch (ParseException e) {
                trnMapperRuleStorage = null;
                logger.error("Unable to read in mapping rules. Working without mapping rules.",e);
            } catch (IOException e) {
                logger.error("Unable to read in mapping rules. Working without mapping rules.",e);
                logger.error("Unable to read in mapping rules. Working without mapping rules.",e);
            }
        }
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes);

        try {
            if (qName.equals("se")) {
                beforeLastSentenceStart = tStorage.lastToken();
            } else if (qName.equals("c") || qName.equals("w")) {
                beforeLastWordStart = tStorage.lastToken();
            } else if (qName.equals("gloss")) {
                String lem = attributes.getValue("lem");
                String lex = attributes.getValue("lex");
                String gram = attributes.getValue("gram");
                String flex = attributes.getValue("flex");

                List<Map<String,String>> lexVariants = new ArrayList<Map<String, String>>();
                parseAttrs(lex,lexVariants);
                Set<Map<String,String>> variants;
                if (gram != null) {
                    List<Map<String,String>> gramVariants = new ArrayList<Map<String, String>>();
                    parseAttrs(gram,gramVariants);

                    variants = multiply(lexVariants,gramVariants);
                } else {
                    variants = new HashSet<Map<String, String>>(lexVariants);
                }

                for (Map<String, String> variant : variants) {
                    variant.put("lem",lem);
                    variant.put("flex",flex);
                }

                glossList.addAll(variants);
            }
        } catch (Exception e) {
            logger.warn("Error when trying to process " + qName, e);
        }

    }

    private Set<Map<String, String>> multiply(List<Map<String, String>> vars1, List<Map<String, String>> vars2) {
        Set<Map<String,String>> result = new HashSet<Map<String, String>>();

        for (Map<String, String> map1 : vars1) {
            for (Map<String, String> map2 : vars2) {
                Map<String,String> nmap = new HashMap<String, String>(map1);
                nmap.putAll(map2);
                result.add(nmap);
            }
        }

        return result;
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        super.endElement(uri, localName, qName);
        try {
            if (qName.equals("se")) {
                Token s = beforeLastSentenceStart == null ? tStorage.firstToken() : beforeLastSentenceStart.getNextToken();
                Token e = tStorage.lastToken();
                tStorage.add(TreetonFactory.newTreenotation(s,e,sentenceType));
            } else if (qName.equals("br")||qName.equals("br ")) {
                text.append(" ");
                tStorage.addToken(1,1,atomicType,localBoard," ");
            } else if (qName.equals("c")) {
                Token s = beforeLastWordStart == null ? tStorage.firstToken() : beforeLastWordStart.getNextToken();
                while (s != null) {
                    if (s.getText().trim().length()>0)
                        break;
                    s = s.getNextToken();
                }

                if (s!=null) {
                    Token e = tStorage.lastToken();
                    while (e != null) {
                        if (e.getText().trim().length()>0)
                            break;
                        e = e.getPreviousToken();
                    }

                    if (e != null) {
                        localBoard.put(kindFeature,punctuationValue);
                        tStorage.add(TreetonFactory.newTreenotation(s,e,tokenType,localBoard));
                    }
                }
            } else if (qName.equals("w")) {
                Token s = beforeLastWordStart == null ? tStorage.firstToken() : beforeLastWordStart.getNextToken();
                Token e = tStorage.lastToken();
                for (Map<String,String> map : glossList) {
                    for (Map.Entry<String,String> entry : map.entrySet()) {
                        String key = entry.getKey().replace('-','_');
                        int feature = glossType.getFeatureIndex(key);
                        if (feature < 0) {
                            throw new MarkupHandlerException("Undefined feature "+key+"(type "+glossType.getName()+")");
                        }
                        localBoard.put(feature,glossType,entry.getValue());
                    }
                    Treenotation trn = TreetonFactory.newTreenotation(s, e, glossType, localBoard);

                    if (trnMapperRuleStorage != null) {
                        TrnMapperRule mapperRule = trnMapperRuleStorage.getRule(trn);
                        if (mapperRule != null) {
                            mapperRule.bind(trn);
                            mapperRule.assign(localBoard);
                            trn.removeAll();
                            if (!mapperRule.getType().equals(trn.getType())) {
                                tStorage.changeType(trn,mapperRule.getType());
                            }
                            trn.put(localBoard);
                        }
                    }

                    tStorage.add(trn);
                }

                glossList.clear();
            } else if (qName.equals("gloss")) {
            }
        } catch (Exception e) {
            logger.warn("Error when trying to process " + qName, e);
        } finally {
            localBoard.clean();
        }
    }

    public void characters(char ch[], int start, int length) throws SAXException {
        String s = new String(ch, start, length);
        text.append(s);
        int i=0;
        while (i<s.length() && s.charAt(i)==' ') i++;

        if (i>0) {
            tStorage.addToken(i,1,atomicType,localBoard,s.substring(0,i));
            s = s.substring(i);
        }

        if (s.length()>0) {
            i=s.length()-1;
            while (i>=0 && s.charAt(i)==' ') i--;

            tStorage.addToken(i+1,1,atomicType,localBoard,s.substring(0,i+1));
            if (i < s.length() - 1) {
                s = s.substring(i+1);
                tStorage.addToken(s.length(),1,atomicType,localBoard,s);
            }
        }

    }

    private void parseAttrs(String s, List<Map<String,String>> variants) {
        StringTokenizer tok = new StringTokenizer(s,"¦");
        while (tok.hasMoreTokens()) {
            String variant = tok.nextToken();
            StringTokenizer tok1 = new StringTokenizer(variant,",");
            List<Map<String,String>> attrs = new ArrayList<Map<String, String>>();
            while (tok1.hasMoreTokens()) {
                String pair = tok1.nextToken().trim();
                int idx = pair.indexOf(":");
                if (idx>=0) {
                    String key = pair.substring(0,idx).trim();
                    String value = pair.substring(idx+1).trim();

                    List<String> values = new ArrayList<String>();

                    StringTokenizer tok2 = new StringTokenizer(value,"/");
                    while (tok2.hasMoreTokens()) {
                        values.add(tok2.nextToken().trim());
                    }

                    if (attrs.size()==0) {
                        attrs.add(new HashMap<String, String>());
                    }

                    String first = values.get(0);

                    for (Map<String, String> attr : attrs) {
                        attr.put(key,first);
                    }

                    int sz = attrs.size();

                    for (int i=1;i<values.size();i++) {
                        for (int j=0;j<sz;j++) {
                            Map<String, String> nmap = new HashMap<String, String>(attrs.get(j));
                            nmap.put(key,values.get(i));
                            attrs.add(nmap);
                        }
                    }
                } else {
                    logger.warn("Found unary attr: "+pair);
                }
            }
            variants.addAll(attrs);
        }
    }
}
