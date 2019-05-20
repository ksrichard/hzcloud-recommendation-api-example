package hu.klavorar.recommendationapi.config;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.spi.impl.discovery.HazelcastCloudDiscovery;
import com.hazelcast.client.spi.properties.ClientProperty;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class HazelcastConfig {

    @Autowired
    private HazelcastConfigProperties hazelcastConfigProperties;

    @Bean
    public HazelcastInstance hazelcastInstance() {
        ClientConfig config = new ClientConfig();
        config.setGroupConfig(new GroupConfig(
                hazelcastConfigProperties.getClusterGroupName(),
                hazelcastConfigProperties.getClusterGroupPassword()));
        config.setProperty("hazelcast.client.statistics.enabled","true");
        config.setProperty(ClientProperty.HAZELCAST_CLOUD_DISCOVERY_TOKEN.getName(), hazelcastConfigProperties.getDiscoveryToken());
        return HazelcastClient.newHazelcastClient(config);
    }

}

