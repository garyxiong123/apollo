package com.ctrip.apollo.client.loader.impl;

import com.ctrip.apollo.client.loader.ConfigServiceLocator;
import com.ctrip.apollo.client.model.ApolloRegistry;
import com.ctrip.apollo.client.util.ConfigUtil;
import com.ctrip.apollo.core.dto.ApolloConfig;
import com.ctrip.apollo.core.dto.ServiceDTO;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class RemoteConfigLoaderTest {
  private RemoteConfigLoader remoteConfigLoader;
  @Mock
  private RestTemplate restTemplate;
  @Mock
  private ConfigUtil configUtil;
  @Mock
  private ConfigServiceLocator serviceLocater;
  @Mock
  private ResponseEntity<ApolloConfig> someResponse;

  @Before
  public void setUp() {
    remoteConfigLoader = spy(new RemoteConfigLoader(restTemplate, configUtil, serviceLocater));
  }

  @Test
  public void testLoadApolloConfig() throws Exception {
    String someServerUrl = "http://someUrl";
    String someCluster = "some cluster";
    ApolloConfig apolloConfig = mock(ApolloConfig.class);
    String someAppId = "1";
    ApolloRegistry apolloRegistry = assembleSomeApolloRegistry(someAppId, "someVersion");
    ApolloConfig previousConfig = null;

    ServiceDTO someService = new ServiceDTO();
    someService.setHomepageUrl(someServerUrl);
    List<ServiceDTO> someServices = new ArrayList<>();
    someServices.add(someService);
    when(serviceLocater.getConfigServices()).thenReturn(someServices);
    when(configUtil.getCluster()).thenReturn(someCluster);
    doReturn(apolloConfig).when(remoteConfigLoader)
        .getRemoteConfig(restTemplate, someServerUrl, someCluster, apolloRegistry, previousConfig);

    ApolloConfig result = remoteConfigLoader.loadApolloConfig(apolloRegistry, previousConfig);

    assertEquals(apolloConfig, result);
  }

  @Test
  public void testGetRemoteConfig() throws Exception {
    String someAppId = "1";
    String someServerUrl = "http://someServer";
    String someClusterName = "someCluster";
    String someVersionName = "someVersion";
    ApolloConfig someApolloConfig = mock(ApolloConfig.class);
    ApolloRegistry apolloRegistry = assembleSomeApolloRegistry(someAppId, someVersionName);
    ApolloConfig previousConfig = null;

    when(someResponse.getStatusCode()).thenReturn(HttpStatus.OK);
    when(someResponse.getBody()).thenReturn(someApolloConfig);
    when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
        eq(ApolloConfig.class), anyMap())).thenReturn(someResponse);

    ApolloConfig
        result =
        remoteConfigLoader
            .getRemoteConfig(restTemplate, someServerUrl, someClusterName, apolloRegistry,
                previousConfig);

    assertEquals(someApolloConfig, result);
  }

  @Test(expected = RuntimeException.class)
  public void testGetRemoteConfigWithServerError() throws Exception {
    String someAppId = "1";
    String someServerUrl = "http://someServer";
    String someClusterName = "someCluster";
    String someVersionName = "someVersion";
    ApolloRegistry apolloRegistry = assembleSomeApolloRegistry(someAppId, someVersionName);
    ApolloConfig previousConfig = null;
    HttpStatus someErrorCode = HttpStatus.INTERNAL_SERVER_ERROR;

    when(someResponse.getStatusCode()).thenReturn(someErrorCode);
    when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
        eq(ApolloConfig.class), anyMap())).thenReturn(someResponse);

    remoteConfigLoader.getRemoteConfig(restTemplate, someServerUrl, someClusterName, apolloRegistry,
        previousConfig);
  }

  @Test
  public void testGetRemoteConfigWith304Response() throws Exception {
    String someAppId = "1";
    String someServerUrl = "http://someServer";
    String someClusterName = "someCluster";
    String someVersionName = "someVersion";
    ApolloRegistry apolloRegistry = assembleSomeApolloRegistry(someAppId, someVersionName);
    ApolloConfig previousConfig = null;

    when(someResponse.getStatusCode()).thenReturn(HttpStatus.NOT_MODIFIED);
    when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
        eq(ApolloConfig.class), anyMap())).thenReturn(someResponse);

    ApolloConfig
        result =
        remoteConfigLoader
            .getRemoteConfig(restTemplate, someServerUrl, someClusterName, apolloRegistry,
                previousConfig);

    assertNull(result);
  }

  private ApolloRegistry assembleSomeApolloRegistry(String someAppId, String someVersion) {
    ApolloRegistry someApolloRegistry = new ApolloRegistry();
    someApolloRegistry.setAppId(someAppId);
    someApolloRegistry.setVersion(someVersion);

    return someApolloRegistry;
  }
}
