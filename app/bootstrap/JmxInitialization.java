package bootstrap;


import com.google.common.collect.ImmutableMap;
import play.Logger;
import play.Play;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Map;

//@OnApplicationStart
public class JmxInitialization extends Job {
    private static JMXConnectorServer jmxServer = null;

    public void doJob() {
        if (jmxServer != null) {
            return;
        }

        int port = Integer.valueOf(System.getProperty("jmx.port", "8280"));

        String accessFile = System.getProperty("com.sun.management.jmxremote.access.file");
        String passwordFile = System.getProperty("com.sun.management.jmxremote.password.file");
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        if (accessFile == null || passwordFile == null) {
            if (Play.mode == Play.Mode.PROD) {
                Logger.error("Cannot start JMX: access.file and password.file are not defined!");
            } else {
                Logger.debug("Cannot start JMX: access.file and password.file are not defined.");
            }
            return;
        }

        Map<String, String> jmxParameters = ImmutableMap.of("jmx.remote.x.access.file", accessFile,
                                                            "jmx.remote.x.password.file", passwordFile);
        Logger.debug("Jmx Parameters: %s", jmxParameters);
        try {
            LocateRegistry.createRegistry(port);
            JMXServiceURL url = new JMXServiceURL(String.format(
                    "service:jmx:rmi://127.0.0.1:%s/jndi/rmi://127.0.0.1:%s/jmxrmi",
                    port + 100, port));
            jmxServer = JMXConnectorServerFactory.newJMXConnectorServer(url, jmxParameters, mbs);
            jmxServer.start();
            Logger.debug("JMX Server started successfully at port %s", port);
        } catch (RemoteException e) {
            Logger.error(e, "Could not start JMX server.");
        } catch (MalformedURLException e) {
            Logger.error(e, "Malformed URL: This should never happen.");
        } catch (IOException e) {
            Logger.error(e, "Could not start JMX server.");
        }

    }
}
