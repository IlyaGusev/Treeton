/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.dict;

import treeton.core.*;
import treeton.core.TString;
import treeton.core.applier.ApplierException;
import treeton.core.config.context.resources.ResourceInstantiationException;
import treeton.core.config.context.treenotations.TreenotationsContext;
import treeton.core.fsm.CharFSMImpl;
import treeton.core.fsm.PackedByteFSM;
import treeton.core.model.TreetonModelException;
import treeton.core.model.TrnType;
import treeton.core.model.TrnTypeUtils;
import treeton.core.model.dclimpl.TrnTypeStorageDclImpl;
import treeton.core.util.*;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Iterator;

public class PackedByteDictionary implements Dictionary {
    private static BlackBoard localBoard = TreetonFactory.newBlackBoard(100, false);
    TrnType type;
    UriTable uriTable = new UriTable();
    Recoder lemmaRecoder;
    PackedByteFSM fsm;
    int intmapDim;
    int attrDim;
    int lengthDim;
    int lemmaDim;
    Object[] mappers;
    Object[] attrs;
    byte[] data;

    public byte[] getByteRepresentation(TreenotationsContext context) throws ApplierException {
        char[] trnTypes;
        try {
            trnTypes = TrnTypeUtils.getFullCharRepresentation(context.getTypes());
        } catch (TreetonModelException e) {
            throw new ApplierException("TreetonModel problem", e);
        }
        int size = 4 + trnTypes.length * 2;

        byte[] uriBytes = uriTable.getByteRepresentation();
        size += uriBytes.length;

        size += 4;

        size += lemmaRecoder.getByteSize();
        size += fsm.getByteSize();

        size += 4;
        size += 4;
        size += 4;
        size += 4;

        size += 4;
        for (Object mapper : mappers) {
            size += ((IntMapper) mapper).getByteSize();
        }

        size += 4;
        for (Object attr : attrs) {
            size++; //for marker
            if (attr instanceof TString) {
                size += ((TString) attr).getByteSize();
            } else if (attr instanceof Integer) {
                size += 4;
            }
        }

        size += 4 + data.length;

        ByteBuffer buf = ByteBuffer.allocate(size);

        buf.putInt(trnTypes.length);
        sut.appendCharsToByteBuffer(buf, trnTypes);

        buf.put(uriBytes);

        try {
            buf.putInt(type.getIndex());
        } catch (TreetonModelException e) {
            throw new ApplierException("TreetonModel problem", e);
        }

        lemmaRecoder.appendSelf(buf);
        fsm.appendSelf(buf);

        buf.putInt(intmapDim);
        buf.putInt(attrDim);
        buf.putInt(lengthDim);
        buf.putInt(lemmaDim);


        buf.putInt(mappers.length);
        for (Object mapper1 : mappers) {
            ((IntMapper) mapper1).appendSelf(buf);
        }

        buf.putInt(attrs.length);
        for (Object attr1 : attrs) {
            if (attr1 instanceof TString) {
                buf.put((byte) 'S');
                ((TString) attr1).appendSelf(buf);
            } else if (attr1 instanceof Integer) {
                buf.put((byte) 'I');
                buf.putInt((Integer) attr1);
            }
        }

        buf.putInt(data.length);
        buf.put(data);
        return buf.array();
    }

    public int readInFromBytes(TreenotationsContext context, byte[] arr, int from, ProgressListener plistener) throws ApplierException {
        try {
            if (plistener != null) {
                plistener.progressStarted();
                plistener.statusStringChanged("Считываются данные морфологических словарей");
            }

            int size = sut.getIntegerFromBytes(arr, from);
            from += 4;
            char[] trnTypes = sut.getCharArrayFromBytes(arr, from, size);
            TrnTypeStorageDclImpl storage = new TrnTypeStorageDclImpl();
            storage.readInFromChars(trnTypes, 0);
            from += 2 * size;

            from = uriTable.readInFromBytes(arr, from);

            TrnType oldTp = storage.get(sut.getIntegerFromBytes(arr, from));
            from += 4;

            type = context.getType(oldTp.getName());
            if (type == null) {
                throw new ApplierException("type " + oldTp.getName() + " undefined in current context");
            }

            lemmaRecoder = TreetonFactory.newRecoder();
            from = lemmaRecoder.readInFromBytes(arr, from);
            fsm = new PackedByteFSM();
            from = fsm.readInFromBytes(arr, from);

            intmapDim = sut.getIntegerFromBytes(arr, from);
            from += 4;
            attrDim = sut.getIntegerFromBytes(arr, from);
            from += 4;
            lengthDim = sut.getIntegerFromBytes(arr, from);
            from += 4;
            lemmaDim = sut.getIntegerFromBytes(arr, from);
            from += 4;

            size = sut.getIntegerFromBytes(arr, from);
            from += 4;
            IntMapper[] oldMappers = new IntMapper[size];
            mappers = new Object[size];

            for (int i = 0; i < oldMappers.length; i++) {
                oldMappers[i] = IntMapperStorage.getInstance().getIntMapper(arr, from);
                from += oldMappers[i].getByteSize();
            }

            size = sut.getIntegerFromBytes(arr, from);
            from += 4;
            attrs = new Object[size];

            for (int i = 0; i < size; i++) {
                byte l = arr[from++];
                if (l == 'S') {
                    from = ((TString) (attrs[i] = TreetonFactory.newTString())).readInFromBytes(arr, from);
                } else if (l == 'I') {
                    attrs[i] = sut.getIntegerFromBytes(arr, from);
                    from += 4;
                }
            }

            size = sut.getIntegerFromBytes(arr, from);
            from += 4;
            data = new byte[size];
            System.arraycopy(arr, from, data, 0, size);
            from += size;

            //StringBuffer lemmaBuf = new StringBuffer(128);
            byte[] tarr = new byte[20];

            int curEntryPlace;
            int curEntryNumb;
            int curNumEntries;

            curEntryPlace = 0;
            if (plistener != null) {
                plistener.progressFinished();
                plistener.progressStarted();
                plistener.statusStringChanged("Подготавливаются данные морфологических словарей...");
            }

            int delta = (data.length / 100) + 1;
            int closestPoint = 0;


            while (curEntryPlace < data.length) {
                if (plistener != null) {
                    if (curEntryPlace >= closestPoint) {
                        plistener.progressValueChanged((double) curEntryPlace / data.length);
                        closestPoint += delta;
                    }
                }
                curNumEntries = sut.getFromBytes(data, curEntryPlace, lengthDim);
                curEntryPlace += lengthDim;

                size = sut.getFromBytes(data, curEntryPlace, lemmaDim);
                curEntryPlace += lemmaDim;
                curEntryPlace += size;

                /*lemmaBuf.setLength(0);
               for (int i=0;i<size;i++) {
                   lemmaBuf.append(lemmaRecoder.getSymbolByNumber(data[curEntryPlace++]));
               } */

                curEntryNumb = 0;
                while (curEntryNumb < curNumEntries) {
                    curEntryPlace += 4;
                    //curEntryPlace+=4;
                    int mapperIndex = sut.getFromBytes(data, curEntryPlace, intmapDim);
                    curEntryPlace += intmapDim;
                    if (mapperIndex == -1) {
                        curEntryNumb++;
                        continue;
                    }
                    IntMapper oldMapper = oldMappers[mapperIndex];
                    int l = oldMapper.getMaxValue() + 1;
                    for (int i = 0; i < l; i++) {
                        localBoard.put(type.getFeatureIndex(oldTp.getFeatureNameByIndex(oldMapper.getKey(i))), null);
                    }

                    IntMapper mapper = IntMapperStorage.getInstance().getIntMapper(localBoard);
                    mappers[mapperIndex] = mapper;
                    l = oldMapper.getMaxValue() + 1;

                    if (tarr.length < l * attrDim) {
                        tarr = new byte[l * attrDim];
                    }

                    int cur = curEntryPlace;
                    for (int i = 0; i < l; i++) {
                        int oldK = oldMapper.getKey(i);
                        int k = type.getFeatureIndex(oldTp.getFeatureNameByIndex(oldK));
                        int p = mapper.get(k);
                        System.arraycopy(data, cur, tarr, p * attrDim, attrDim);
                        cur += attrDim;
                    }
                    System.arraycopy(tarr, 0, data, curEntryPlace, l * attrDim);
                    curEntryPlace += l * attrDim;
                    curEntryNumb++;
                }
            }

            if (plistener != null) {
                plistener.progressFinished();
                plistener.progressStarted();
                plistener.statusStringChanged("Строится автомат для русской морфологии");
            }

            if (plistener != null) {
                plistener.statusStringChanged("");
                plistener.progressFinished();
            }
            return from;
        } catch (TreetonModelException e) {
            throw new ApplierException("Problem with model!!!");
        }
    }

    private void pack(FeaturesValueCollector intmapCollector, FeaturesValueCollector attrCollector, int maxOmonN, int maxLemmaLength, int maxIndex, Object[] entries, CharFSMImpl fsm) {
        intmapDim = intmapCollector.size() <= 127 ? 1 : intmapCollector.size() <= 32767 ? 2 : 4;
        attrDim = attrCollector.size() <= 127 ? 1 : attrCollector.size() <= 32767 ? 2 : 4;
        lengthDim = maxOmonN <= 127 ? 1 : maxOmonN <= 32767 ? 2 : 4;
        lemmaDim = maxLemmaLength <= 127 ? 1 : maxLemmaLength <= 32767 ? 2 : 4;
        int nBytes = 0;
        for (int i = 0; i <= maxIndex; i++) {
            Object[] arr = (Object[]) entries[i];
            String lemma = (String) arr[0];
            nBytes += lengthDim;
            nBytes += lemmaDim;
            nBytes += lemma.length();
            for (int j = 1; j < arr.length; j++) {
                nBytes += 4;
                nBytes += intmapDim;
                nBytes += attrDim * ((InputEntry) arr[j]).getAttrs().size();
            }
        }
        data = new byte[nBytes];
        int curPos = 0;
        lemmaRecoder = TreetonFactory.getRecoder("lemma");
        for (int i = 0; i <= maxIndex; i++) {
            Object[] arr = (Object[]) entries[i];
            String lemma = (String) arr[0];
            fsm.changeValue(lemma, curPos);
            sut.putInBytes(data, curPos, arr.length - 1, lengthDim);
            curPos += lengthDim;
            sut.putInBytes(data, curPos, lemma.length(), lemmaDim);
            curPos += lemmaDim;
            for (int j = 0; j < lemma.length(); j++) {
                data[curPos++] = (byte) lemmaRecoder.getSymbolNumber(lemma.charAt(j));
            }
            for (int j = 1; j < arr.length; j++) {
                InputEntry e = (InputEntry) arr[j];
                sut.putIntegerInBytes(data, curPos, uriTable.addURI(e.getUri()));
                curPos += 4;

                IntMapper mapper = ((IntFeatureMapImpl) e.getAttrs()).getIntMapper();
                if (mapper != null) {
                    sut.putInBytes(data, curPos, intmapCollector.getValueIndex(mapper), intmapDim);
                } else {
                    sut.putInBytes(data, curPos, intmapCollector.getValueIndex(-1), intmapDim);
                }
                curPos += intmapDim;

                Iterator it = e.getAttrs().valueIterator();
                while (it.hasNext()) {
                    Object o = it.next();
                    sut.putInBytes(data, curPos, attrCollector.getValueIndex(o), attrDim);
                    curPos += attrDim;
                }
            }
        }
        mappers = intmapCollector.toArray();
        attrs = attrCollector.toArray();
        this.fsm = new PackedByteFSM(fsm);
    }

    public void buildFromIterator(TrnType tp, HandlingErrorIterator<StringEntryPair> entriesIterator, ProgressListener plistener, int size) throws ResourceInstantiationException, IOException {
        type = tp;
        FeaturesValueCollector attrCollector = new FeaturesValueCollector();
        FeaturesValueCollector intmapCollector = new FeaturesValueCollector();
        CharFSMImpl fsm = (CharFSMImpl) TreetonFactory.newCharFSM();
        Object[] entries = new Object[103000];


        int maxOmonN = 1;
        int maxIndex = -1;
        int maxLemmaLength = 0;

        int counter = 0;
        if (plistener != null) {
            plistener.progressStarted();
            plistener.statusStringChanged("Подготавливаются данные морфологических словарей...");
        }

        int delta = (size / 100) + 1;
        while (entriesIterator.hasNext()) {
            if (plistener != null) {
                if (counter++ % delta == 0) {
                    plistener.progressValueChanged((double) counter / size);
                }
            }
            StringEntryPair pair = entriesIterator.next();
            String lemma = pair.getString();
            if (lemma.length() > maxLemmaLength)
                maxLemmaLength = lemma.length();

            InputEntry e = pair.getEntry();
            IntMapper mapper = ((IntFeatureMapImpl) e.getAttrs()).getIntMapper();
            if (mapper != null) {
                intmapCollector.getValueFor(mapper);
            }
            Iterator<NumeratedObject> it = e.getAttrs().numeratedObjectIterator();
            while (it.hasNext()) {
                NumeratedObject no = it.next();
                e.getAttrs().put(no.n, attrCollector.getValueFor(no.o));
            }
            int index = fsm.addString(lemma);
            if (index > maxIndex) {
                maxIndex = index;
            }

            if (index >= entries.length) {
                Object[] tarr = new Object[(int) Math.max(index + 1, entries.length * 1.5)];
                System.arraycopy(entries, 0, tarr, 0, entries.length);
                entries = tarr;
            }
            Object[] arr = (Object[]) entries[index];
            if (arr == null) {
                arr = new Object[2];
                entries[index] = arr;
                arr[0] = lemma;
                arr[1] = e;
            } else {
                Object[] tarr = new Object[arr.length + 1];
                System.arraycopy(arr, 0, tarr, 0, arr.length);
                tarr[tarr.length - 1] = e;
                entries[index] = tarr;

                if (tarr.length - 1 > maxOmonN) {
                    maxOmonN = tarr.length;
                }
            }
        }
        if (plistener != null) {
            plistener.statusStringChanged("");
            plistener.progressFinished();
            plistener.progressStarted();
            plistener.statusStringChanged("Строится автомат для русской морфологии");
        }

        if (plistener != null) {
            plistener.progressFinished();
            plistener.progressStarted();
            plistener.statusStringChanged("Пакуются данные морфологических словарей");
        }

        pack(intmapCollector, attrCollector, maxOmonN, maxLemmaLength, maxIndex, entries, fsm);
        if (plistener != null) {
            plistener.statusStringChanged("");
            plistener.progressFinished();
        }
    }

    public Iterator<String> lemmaIterator() {
        return new InternalEntryIterator();
    }

    public ResultSet findInDict(String s) {
        return new InternalResultSet(s);
    }

    public TrnType getType() {
        return type;
    }

    public class StaticEntry {
        int recordId;
        IntMapper mapper;
        int dataOffset;
        int neighbourOffset;

        public void readIn(int curEntryPlace) {
            dataOffset = curEntryPlace;
            recordId = sut.getIntegerFromBytes(data, dataOffset);
            dataOffset += 4;
            int i = sut.getFromBytes(data, dataOffset, intmapDim);
            if (i == -1) {
                mapper = null;
            } else {
                mapper = (IntMapper) mappers[i];
            }
            dataOffset += intmapDim;
            if (mapper != null) {
                neighbourOffset = dataOffset + attrDim * (mapper.getMaxValue() + 1);
            } else {
                neighbourOffset = dataOffset;
            }
        }

        Object get(int feature) {
            if (mapper == null)
                return null;
            int idx = mapper.get(feature);
            if (idx < 0)
                return null;
            return attrs[sut.getFromBytes(data, dataOffset + idx * attrDim, attrDim)];
        }
    }

    private class InternalEntryIterator implements Iterator<String> {
        StringBuffer lemmaBuf = new StringBuffer(0);
        private int curEntryPlace = -1;
        private int curEntryNumb = -1;
        private StaticEntry currentEntry = new StaticEntry();

        private InternalEntryIterator() {
            curEntryPlace = 0;
        }

        public boolean hasNext() {
            return curEntryPlace < data.length;
        }

        public String next() {
            int curNumEntries = sut.getFromBytes(data, curEntryPlace, lengthDim);
            curEntryPlace += lengthDim;

            int size = sut.getFromBytes(data, curEntryPlace, lemmaDim);
            curEntryPlace += lemmaDim;

            lemmaBuf.setLength(0);
            for (int i = 0; i < size; i++) {
                lemmaBuf.append(lemmaRecoder.getSymbolByNumber(data[curEntryPlace++]));
            }

            curEntryNumb = 0;

            while (curEntryNumb < curNumEntries) {
                currentEntry.readIn(curEntryPlace);
                shiftToNext();
            }

            return lemmaBuf.toString();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        private void shiftToNext() {
            curEntryPlace = currentEntry.neighbourOffset;
            curEntryNumb++;
        }
    }

    class InternalResultSet implements ResultSet {
        private int curEntryPlace = -1;
        private int curEntryNumb = -1;
        private int curNumEntries = -1;
        private StaticEntry currentEntry = new StaticEntry();

        InternalResultSet(String s) {
            curEntryPlace = fsm.get(s);
            if (curEntryPlace != -1) {
                curNumEntries = sut.getFromBytes(data, curEntryPlace, lengthDim);
                curEntryPlace += lengthDim;
                curEntryPlace += lemmaDim + sut.getFromBytes(data, curEntryPlace, lemmaDim);
                curEntryNumb = 0;
            }
        }

        public boolean next() {
            if (curEntryPlace == -1)
                return false;
            if (curEntryNumb < curNumEntries) {
                currentEntry.readIn(curEntryPlace);
                shiftToNext();
                return true;
            }
            return false;
        }

        public URI getRecordUri() {
            return uriTable.get(currentEntry.recordId);
        }

        public Object getValueOf(String keyName) {
            try {
                return currentEntry.get(type.getFeatureIndex(keyName));
            } catch (TreetonModelException e) {
                return null;
            }
        }

        public Object getValueOf(int key) {
            return currentEntry.get(key);
        }

        public TrnType getType() {
            return type;
        }

        public IntMapper getMapper() {
            return currentEntry.mapper;
        }


        private void shiftToNext() {
            curEntryPlace = currentEntry.neighbourOffset;
            curEntryNumb++;
        }
    }
}