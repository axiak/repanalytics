package util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.reflect.TypeToken;
import models.businesses.Review;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizer;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.ObjectStream;
import play.Logger;
import play.Play;
import play.libs.F;
import service.ReviewSource;
import twitter4j.Twitter;

import javax.annotation.Nonnull;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class NaturalLanguages {
    private static TokenizerModel model = null;
    private static DoccatModel categorizationModel = null;
    private static DoccatModel twitterCategorizationModel = null;


    private static void initializeTokenizeModel() {
        if (model == null) {
            try {
                InputStream modelIn = new FileInputStream(new File(new File(Play.applicationPath, "dat"), "en-token.bin").getAbsolutePath());
                model = new TokenizerModel(modelIn);
            } catch (IOException e) {
                Logger.error(e, "Could not open tokenizer model");
            }
        }
    }

    private static void initializeCategorizationModel() {
        if (categorizationModel == null) {
            try {
                InputStream modelIn = new FileInputStream(new File(new File(Play.applicationPath, "dat"), "en-classifier.bin").getAbsolutePath());
                categorizationModel = new DoccatModel(modelIn);
            } catch (IOException e) {
                Logger.error(e, "Could not open classifier model");
            }
        }
        if (twitterCategorizationModel == null) {
            try {
                InputStream modelIn = new FileInputStream(new File(new File(Play.applicationPath, "dat"), "en-classifier-twitter.bin").getAbsolutePath());
                twitterCategorizationModel = new DoccatModel(modelIn);
            } catch (IOException e) {
                Logger.error(e, "Could not open classifier model");
            }
        }
    }

    public static String[] tokenizeString(String input) {
        initializeTokenizeModel();
        Tokenizer tokenizer = new TokenizerME(model);
        return tokenizer.tokenize(input);
    }

    public static void reviewSentiment(@Nonnull Review review) {
        if (review.text == null) {
            return;
        }
        initializeCategorizationModel();
        DocumentCategorizer categorizer;
        if (review.source == ReviewSource.TWITTER) {
            categorizer = new DocumentCategorizerME(twitterCategorizationModel);
        } else {
            categorizer = new DocumentCategorizerME(categorizationModel);
        }
        double[] categorization = categorizer.categorize(tokenizeString(review.text));
        int sentimentRating = Integer.valueOf(categorizer.getBestCategory(categorization));
        if (review.rating == null) {
            review.rating = sentimentRating;
        }

        int i = 0;
        double average = 0, denominator = 0;
        for (double dimensionalValue : categorization) {
            double weight = Math.pow(dimensionalValue, 10);
            average += weight * Integer.valueOf(categorizer.getCategory(i++));
            denominator += weight;
        }
        average /= (denominator * 2.5);
        review.sentiment = average - 1;
    }

    public static void trainClassifier(File inputFile, File outputFile) {
        assert(inputFile.exists());
        StringBuilder b = new StringBuilder();
        try {
            BufferedReader bis = new BufferedReader(new FileReader(inputFile));
            String line;
            while ((line = bis.readLine()) != null) {
                b.append(line);
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
        Gson g = new GsonBuilder().registerTypeAdapter(F.Tuple.class, new TupleInstanceCreator()).create();
        List<F.Tuple<Integer, String>> data = g.fromJson(b.toString(), new TypeToken<List<F.Tuple < Integer, String >>> (){}.getType());
        Logger.info("Data: %s", b.toString());
        List<DocumentSample> samples = new ArrayList<DocumentSample>();
        for (F.Tuple<Integer, String> datum : data) {
            samples.add(new DocumentSample(String.valueOf(datum._1), tokenizeString(datum._2)));
        }
        final Iterator<DocumentSample> doc = samples.iterator();

        DoccatModel model = null;
        try {
            model = DocumentCategorizerME.train("en", new ObjectStream<DocumentSample>() {
                @Override
                public DocumentSample read() throws IOException {
                    try {
                        return doc.next();
                    } catch (NoSuchElementException e) {
                        return null;
                    }
                }

                @Override public void reset() throws IOException, UnsupportedOperationException {}

                @Override public void close() throws IOException {}
            });
        } catch (IOException e) {
        }
        BufferedOutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(outputFile));
            assert model != null;
            model.serialize(out);
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private static class TupleInstanceCreator implements InstanceCreator<F.Tuple<?, ?>> {
        @Override
        @SuppressWarnings("unchecked")
        public F.Tuple<?, ?> createInstance(Type type) {
            return new F.Tuple(null, null);
        }
    }
}
