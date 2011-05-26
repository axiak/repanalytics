package bootstrap;


import com.google.common.collect.ImmutableMap;
import play.Logger;
import play.Play;
import play.jobs.Job;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class JmxInitialization extends Job<Void> {
    private static JMXConnectorServer jmxServer = null;
    private static AtomicBoolean alreadyComputing = new AtomicBoolean(false);

    public void doJob() {
        if (jmxServer == null && !alreadyComputing.getAndSet(true)) {
            initializeJmxServer();
        }
    }

    private void initializeJmxServer() {
        final int port = Integer.valueOf(System.getProperty("jmx.port", "8280"));

        final String accessFile = System.getProperty("com.sun.management.jmxremote.access.file",
                                               Play.getFile("conf" + File.separator + "jmxremote.access").getAbsolutePath());
        final String passwordFile = System.getProperty("com.sun.management.jmxremote.password.file",
                                                 Play.getFile("conf" + File.separator + "jmxremote.password").getAbsolutePath());
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        final Map<String, String> jmxParameters = ImmutableMap.of("jmx.remote.x.access.file", accessFile,
                                                            "jmx.remote.x.password.file", passwordFile);
        try {
            LocateRegistry.createRegistry(port);
            final JMXServiceURL url = new JMXServiceURL(String.format(
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
