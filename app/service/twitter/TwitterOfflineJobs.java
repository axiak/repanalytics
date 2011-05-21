package service.twitter;


import com.google.gson.GsonBuilder;
import models.businesses.Business;
import models.businesses.BusinessChain;
import models.businesses.Review;
import org.apache.commons.lang.math.RandomUtils;
import play.Logger;
import play.Play;
import play.jobs.Job;
import util.NaturalLanguages;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class TwitterOfflineJobs extends Job {
    @Override
    public void doJob() throws Exception {
        //getTwitterData();
        trainTwitterClassifier();
    }

    private void trainTwitterClassifier() {
        File datFile = new File(Play.applicationPath, "dat");
        NaturalLanguages.trainClassifier(new File(datFile, "twitter_info_annotated.json"),
                                         new File(datFile, "en-classifier-twitter.bin"));
    }

    private void getTwitterData() {
        List<Review> reviews = new LinkedList<Review>();
        TwitterService service = new TwitterService();
        List<BusinessChain> chains = BusinessChain.all().fetch();
        Collections.shuffle(chains);
        for (BusinessChain chain : chains.subList(0, 15)) {
            Business business = new Business();
            business.name = chain.name;
            business.id = RandomUtils.nextLong();
            Logger.info("Getting info for %s", chain.name);
            reviews.addAll(service.getReviews(business));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(new File(new File(Play.applicationPath, "dat"), "twitter_info.json"));
            fos.write(new GsonBuilder().create().toJson(reviews).getBytes());
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }
    }
}
