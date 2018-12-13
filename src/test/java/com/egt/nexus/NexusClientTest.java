package com.egt.nexus;

import com.github.kristofa.test.http.Method;
import com.github.kristofa.test.http.MockHttpServer;
import com.github.kristofa.test.http.SimpleHttpResponseProvider;
import org.apache.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static junit.framework.Assert.assertNotNull;

public class NexusClientTest {

    private static final int PORT = 51234;
    private static final String baseUrl = String.format("http://localhost:%d/", PORT);
    private static MockHttpServer server;
    private static SimpleHttpResponseProvider responseProvider;


    @BeforeClass
    public static void startHttpServer() throws IOException {
        responseProvider = new SimpleHttpResponseProvider();
        server = new MockHttpServer(PORT, responseProvider);
        server.start();
    }

    @AfterClass
    public static void stopHttpServer() throws IOException {
        server.stop();
    }

    @Test
    public void getDockerRegistry() throws MalformedURLException {
        responseProvider.expect(Method.GET, "/repository/docker-registry/v2/_catalog").respondWith(HttpStatus.SC_OK, APPLICATION_JSON, "{\"repositories\": [\"space/store\",\"space/group-1/server\"]}");
        NexusClient nexusClient = new NexusClient(baseUrl, "", "");
        DockerRegistry repos = nexusClient.getDockerRegistry();
        assertNotNull(repos);
    }

    @Test
    public void getImageMetadata() {
        responseProvider.expect(Method.GET, "/service/rest/beta/search?docker.imageName=space/store").respondWith(HttpStatus.SC_OK, APPLICATION_JSON, "{ \"items\": [{ \"id\": \"ZG9ja2VyLXJlZ2lzdHJ5OmE0OTc5Y2NmNzJkYzcxMjgyOTVkMDc0Y2M0NWViMDZk\", \"repository\": \"docker-registry\", \"format\": \"docker\", \"group\": null, \"name\": \"space/store\", \"version\": \"18615\", \"assets\": [{ \"downloadUrl\": \"http://host/repository/docker-registry/v2/space/store/manifests/18615\", \"path\": \"v2/space/store/manifests/18615\", \"id\": \"ZG9ja2VyLXJlZ2lzdHJ5OjcxYWZlYTU0MGUyM2RkZTU4ZWI4M2JiYTg2YmM3M2Rh\", \"repository\": \"docker-registry\", \"format\": \"docker\", \"checksum\": { \"sha1\": \"b98d62cf1c6f4e5ee1bad3a646b1b10324c9fffd\", \"sha256\": \"16ac2b8725d2e4f4e761e8089ceef66f944a6d30f3bbe5872171a45a0dd06ce7\" } }]}]}");
        NexusClient nexusClient = new NexusClient(baseUrl, "", "");
        ImageMetadata imageMetadata = nexusClient.getImageMetadata("space/store");
        assertNotNull(imageMetadata);
    }

    @Test
    public void getImageNamesBySpaceName() {
        responseProvider.expect(Method.GET, "/repository/docker-registry/v2/_catalog").respondWith(HttpStatus.SC_OK, APPLICATION_JSON, "{\"repositories\": [\"space/store\",\"space/group-1/server\"]}");
        NexusClient nexusClient = new NexusClient(baseUrl, "", "");
        List<String> imageNames = nexusClient.getImageNamesBySpaceName("space");
        assertNotNull(imageNames);
    }

    @Test
    public void getImageTags() {
        responseProvider.expect(Method.GET, "/service/rest/beta/search?docker.imageName=space/store").respondWith(HttpStatus.SC_OK, APPLICATION_JSON, "{ \"items\": [{ \"id\": \"ZG9ja2VyLXJlZ2lzdHJ5OmE0OTc5Y2NmNzJkYzcxMjgyOTVkMDc0Y2M0NWViMDZk\", \"repository\": \"docker-registry\", \"format\": \"docker\", \"group\": null, \"name\": \"space/store\", \"version\": \"18615\", \"assets\": [{ \"downloadUrl\": \"http://host/repository/docker-registry/v2/space/store/manifests/18615\", \"path\": \"v2/space/store/manifests/18615\", \"id\": \"ZG9ja2VyLXJlZ2lzdHJ5OjcxYWZlYTU0MGUyM2RkZTU4ZWI4M2JiYTg2YmM3M2Rh\", \"repository\": \"docker-registry\", \"format\": \"docker\", \"checksum\": { \"sha1\": \"b98d62cf1c6f4e5ee1bad3a646b1b10324c9fffd\", \"sha256\": \"16ac2b8725d2e4f4e761e8089ceef66f944a6d30f3bbe5872171a45a0dd06ce7\" } }]}]}");
        NexusClient nexusClient = new NexusClient(baseUrl, "", "");
        List<String> imageNames = nexusClient.getImageTags("space", "store");
        assertNotNull(imageNames);
    }
}