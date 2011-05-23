package jobs;


import controllers.Demo;
import play.Logger;
import play.jobs.Every;
import play.jobs.Job;


@Every("5min")
public class CleanOldStreams extends Job {
    @Override
    public void doJob() throws Exception {
        Demo.reviewFetcher.cleanOldBusinesses("5min");
    }
}
