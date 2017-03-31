/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.musimatix;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import treeton.core.config.BasicConfiguration;
import treeton.core.config.context.ContextConfiguration;
import treeton.core.config.context.ContextConfigurationProsodyImpl;
import treeton.core.config.context.resources.LoggerLogListener;
import treeton.core.util.LoggerProgressListener;

import java.util.*;

public class VerseProcessingExample {
    private static final Logger logger = Logger.getLogger(VerseProcessingExample.class);

    public static void main(String[] argv) throws Exception, VerseProcessor.VerseProcessorException {
        BasicConfiguration.createInstance();
        ContextConfiguration.registerConfigurationClass(ContextConfigurationProsodyImpl.class);
        ContextConfiguration.createInstance();
        Logger.getRootLogger().setLevel(Level.INFO);

        Properties props = new Properties();
        props.setProperty("metricGrammarPath",argv[0]);
        props.setProperty("stressRestrictionViolationWeight","1");
        props.setProperty("reaccentuationRestrictionViolationWeight","3");
        props.setProperty("spacePerMeter","10");
        props.setProperty("maxStressRestrictionViolations","3");
        props.setProperty("maxReaccentuationRestrictionViolations","2");
        props.setProperty("maxSyllablesPerVerse","23");
        props.setProperty("meterMultiplier","1.0");
        props.setProperty("footnessMultiplier","0.25");
        props.setProperty("footnessVarianceMultiplier","0.25");
        props.setProperty("numberOfVersesForAverageVector","-1");
        props.setProperty("fragmentSimilarityThreshold","0.8");
        props.setProperty("secondPass","true");

        VerseProcessor processor = new VerseProcessor(props);
        processor.setProgressListener(new LoggerProgressListener("Musimatix",logger));
        processor.addLogListener(new LoggerLogListener(logger));
        processor.initialize();

        /*ArrayList<String> song = new ArrayList<>();
        //song.add( "Песня группы Комунизм, исполняется Егором Летовым на альбоме Хроника пикирующего бомбардировщика");
        song.add("Мой дядя самых лучших правил");
        song.add("Когда не в шутку занемог");
        song.add("Он уважать себя заставил");
        song.add("И лучше выдумать не мог");*/

        /*String[] strings = {      "Ти моя остання любов,",
                "Моя машина, моя машина.",
                "Ти i я напилися знов,",
                "Моя єдина , смак бензина й кави...",
                "День i нiчь ,дихає час",
                "А ми з тобою живемо двое,",
                "Автомобiль буде у нас,",
                "Мое ти сонце"};*/

        String[] strings = {"Россия — священная наша держава,",
                "Россия — любимая наша страна.",
                "Могучая воля, великая слава —",
                "Твоё достоянье на все времена!",
                "",
                "Славься, Отечество наше свободное,",
                "Братских народов союз вековой,",
                "Предками данная мудрость народная!",
                "Славься, страна! Мы гордимся тобой!",
                "",
                "От южных морей до полярного края",
                "Раскинулись наши леса и поля.",
                "Одна ты на свете! Одна ты такая —",
                "Хранимая Богом родная земля!",
                "",
                "Славься, Отечество наше свободное,",
                "Братских народов союз вековой,",
                "Предками данная мудрость народная!",
                "Славься, страна! Мы гордимся тобой!",
                "",
                "Широкий простор для мечты и для жизни",
                "Грядущие нам открывают года.",
                "Нам силу даёт наша верность Отчизне.",
                "Так было, так есть и так будет всегда!",
                "",
                "Славься, Отечество наше свободное,",
                "Братских народов союз вековой,",
                "Предками данная мудрость народная!",
                "Славься, страна! Мы гордимся тобой!"
        };

        String[] mixedStrings = {
                "Россия — священная наша держава,",
                "Славься, Отечество наше свободное,",
                "Россия — любимая наша страна.",
                "Братских народов союз вековой,",
                "Могучая воля, великая слава —",
                "Предками данная мудрость народная!",
                "Твоё достоянье на все времена!",
                "Славься, страна! Мы гордимся тобой!",
                "От южных морей до полярного края",
                "Славься, Отечество наше свободное,",
                "Раскинулись наши леса и поля.",
                "Братских народов союз вековой,",
                "Одна ты на свете! Одна ты такая —",
                "Предками данная мудрость народная!",
                "Хранимая Богом родная земля!",
                "Славься, страна! Мы гордимся тобой!",
                "Широкий простор для мечты и для жизни",
                "Славься, Отечество наше свободное,",
                "Грядущие нам открывают года.",
                "Братских народов союз вековой,",
                "Нам силу даёт наша верность Отчизне.",
                "Предками данная мудрость народная!",
                "Так было, так есть и так будет всегда!",
                "Славься, страна! Мы гордимся тобой!"
        };

        String[] query1strings = {
                "Как вобла красивая щуку плотвичка",
                "Как скаты акулу и тут камбала",
                "Так кит у дельфина, карась у форели",
                "Так ёрш барабульку, так лещ пескаря",
        };

        String[] query2strings = {
                "Вобла капризная, окунь не ласковый",
                "Умных дельфинов косяк боевой",
                "Ёршики, палтусы, скаты бездомные",
                "Килька с треской и с морскою звездой!",
        };

        ArrayList<String> song = new ArrayList<>(Arrays.asList(strings));

        for( int i = 0; i < 1; i++ ) {
            Collection<VerseDescription> verseDescriptions = processor.process(song,false);

            //System.out.println(i);
            for (VerseDescription verseDescription : verseDescriptions) {
                System.out.println(verseDescription);
            }
        }

        ArrayList<String> query1 = new ArrayList<>(Arrays.asList(query1strings));
        ArrayList<String> query2 = new ArrayList<>(Arrays.asList(query2strings));
        ArrayList<String> mixedSong = new ArrayList<>(Arrays.asList(mixedStrings));

        ArrayList<VerseDescription> verseDescriptions = processor.process(song,false);
        ArrayList<VerseDescription> mixedVerseDescriptions = processor.process(mixedSong,false);
        ArrayList<VerseDescription> query1Descriptions = processor.process(query1,false);
        ArrayList<VerseDescription> query2Descriptions = processor.process(query2,false);

        Vector<Double> averageForVerse = processor.countAverage(verseDescriptions,0);
        Vector<Double> averageForMixed = processor.countAverage(mixedVerseDescriptions,0);
        Vector<Double> averageForQuery1 = processor.countAverage(query1Descriptions,0);
        Vector<Double> averageForQuery2 = processor.countAverage(query2Descriptions,0);

        System.out.println("Average vector for verse: " + processor.getHtmlFormattedMetricVector(averageForVerse));
        System.out.println("Average vector for mixed verse: " + processor.getHtmlFormattedMetricVector(averageForMixed));
        System.out.println("Average vector for query1: " + processor.getHtmlFormattedMetricVector(averageForQuery1));
        System.out.println("Average vector for query2: " + processor.getHtmlFormattedMetricVector(averageForQuery2));

        System.out.println("Simple distance between verse and query1 "+processor.countAverageDistance( averageForQuery1, averageForVerse ));
        System.out.println("Simple distance between verse and query2 "+processor.countAverageDistance( averageForQuery2, averageForVerse ));
        System.out.println("Simple distance between mixed verse and query1 "+processor.countAverageDistance( averageForQuery1, averageForMixed ));
        System.out.println("Simple distance between mixed verse and query2 "+processor.countAverageDistance( averageForQuery2, averageForMixed ));

        PreciseVerseDistanceCounter preciseVerseDistanceCounter = processor.createVerseDistanceCounter(query1Descriptions,0);
        System.out.println("Precise distance between verse and query1 "+preciseVerseDistanceCounter.countDistance( verseDescriptions, 0 ));
        System.out.println("Precise distance between mixed verse and query1 "+preciseVerseDistanceCounter.countDistance( mixedVerseDescriptions, 0 ));

        preciseVerseDistanceCounter = processor.createVerseDistanceCounter(query2Descriptions,0);
        System.out.println("Precise distance between verse and query2 "+preciseVerseDistanceCounter.countDistance( verseDescriptions, 0 ));
        System.out.println("Precise distance between mixed verse and query2 "+preciseVerseDistanceCounter.countDistance( mixedVerseDescriptions, 0 ));

        processor.deinitialize();
    }
}
