package org.example;

import org.apache.activemq.artemis.api.core.QueueConfiguration;
import org.apache.activemq.artemis.api.core.client.*;
import org.apache.activemq.artemis.core.config.CoreAddressConfiguration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.config.impl.SecurityConfiguration;
import org.apache.activemq.artemis.core.protocol.core.impl.RemotingConnectionImpl;
import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.SecuritySettingPlugin;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.apache.activemq.artemis.spi.core.security.ActiveMQJAASSecurityManager;
import org.apache.activemq.artemis.tests.util.ActiveMQTestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ArtemisTest extends ActiveMQTestBase {
    private ActiveMQServer server;

    ConfigurationImpl configuration = new ConfigurationImpl();

    @Before
    public void setUp() throws Exception {
        super.setUp();

        Map<String, Set<Role>> setSecurityRol = configuration.getSecurityRoles();
        Set<Role> roleSet1 = new HashSet<>();
        Role role1 = new Role("amq",true,true,true,true,true,true,true,true,true,true);
        roleSet1.add(role1);
        setSecurityRol.put("#",roleSet1);
        Set<Role> roleSet2 = new HashSet<>();
        Role role2 = new Role("xyz",true,true,true,true,true,true,true,true,true,true);
        roleSet2.add(role2);
        setSecurityRol.put("A.*.B",roleSet2);
        System.out.println(">> "+setSecurityRol.size());
        configuration.setPersistenceEnabled(false);
        configuration.addAcceptorConfiguration("netty","tcp://localhost:61616");
        server = createServer(configuration);
        ActiveMQJAASSecurityManager activeMQJAASSecurityManager = (ActiveMQJAASSecurityManager) server.getSecurityManager();
        SecurityConfiguration userConfig = activeMQJAASSecurityManager.getConfiguration();
        userConfig.addUser("admin","admin");
        userConfig.addRole("admin","amq");
        userConfig.addUser("sender","sender");
        userConfig.addRole("sender","xyz");
        Map<String, AddressSettings> stringAddresssettingsMap = configuration.getAddressesSettings();
        AddressSettings addressSettings = new AddressSettings();
        addressSettings.setAutoCreateAddresses(true);
        addressSettings.setAutoCreateQueues(true);
        stringAddresssettingsMap.put("#",addressSettings);
        List<CoreAddressConfiguration> coreAddressConfigurations = configuration.getAddressConfigurations();
        CoreAddressConfiguration coreAddressConfiguration = new CoreAddressConfiguration();
        coreAddressConfiguration.setName("A.C.B");
        coreAddressConfiguration.addQueueConfiguration(new QueueConfiguration("A.C.B"));
        coreAddressConfigurations.add(coreAddressConfiguration);
    }
    @Test
    public void testConsumerRole() throws Exception {
        server.start();
        ServerLocator serverLocator = ActiveMQClient.createServerLocator("tcp://localhost:61616");
        ClientSession sendSession = serverLocator.createSessionFactory().createSession("sender","sender",false,true,true,false,10);
        ClientSession recvSession = serverLocator.createSessionFactory().createSession("admin","admin",false,true,true,false,10);
        sendSession.createProducer().send("A.C.B",sendSession.createMessage(true));
        ClientConsumer clientConsumer = recvSession.createConsumer("A.C.B");
        recvSession.start();
        ClientMessage clientMessage = clientConsumer.receive(5000);
        System.out.println(">>>> "+clientMessage);
        assertTrue(clientMessage != null);
        recvSession.stop();
        recvSession.close();
        sendSession.close();
    }

    @After
    public void cleanup() throws Exception{
        server.stop();
    }
}
