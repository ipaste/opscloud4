package com.baiyi.opscloud.datasource.kubernetes.client.provider;

import com.baiyi.opscloud.common.datasource.KubernetesConfig;
import com.baiyi.opscloud.datasource.kubernetes.client.KubeClient;
import com.baiyi.opscloud.datasource.kubernetes.client.provider.eks.AmazonEksHelper;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URISyntaxException;

/**
 * @Author baiyi
 * @Date 2022/9/14 09:37
 * @Version 1.0
 */
@Slf4j
@Component
public class AmazonEksProvider {

    private static AmazonEksHelper amazonEksHelper;

    @Autowired
    public void setAmazonEksHelper(AmazonEksHelper amazonEksHelper) {
        AmazonEksProvider.amazonEksHelper = amazonEksHelper;
    }


    /**
     * 按供应商构建 client
     *
     * @param kubernetes
     * @return
     */
    public static KubernetesClient buildWithProvider(KubernetesConfig.Kubernetes kubernetes) {
        try {
            String token = amazonEksHelper.generateEksToken(kubernetes.getAmazonEks());
            return build(kubernetes.getAmazonEks().getUrl(), token);
        } catch (URISyntaxException e) {
        }
        return null;
    }

    private static KubernetesClient build(String url, String token) {
        io.fabric8.kubernetes.client.Config config = new ConfigBuilder()
                .withMasterUrl(url)
                .withOauthToken(token)
                .withTrustCerts(true)
                .withWebsocketTimeout(KubeClient.Config.WEBSOCKET_TIMEOUT)
                .withConnectionTimeout(KubeClient.Config.CONNECTION_TIMEOUT)
                .withRequestTimeout(KubeClient.Config.REQUEST_TIMEOUT)
                .withWatchReconnectInterval(60000)
                .build();
        // 6.x
        // return new KubernetesClientBuilder().withConfig(config).build();
        return new DefaultKubernetesClient(config);

    }

}
