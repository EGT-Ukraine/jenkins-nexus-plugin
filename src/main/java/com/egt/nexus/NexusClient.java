package com.egt.nexus;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NexusClient {

    private static final int CONNECTION_TIMEOUT = 30000;
    private static final int READ_TIMEOUT = 30000;

    private WebResource resource;

    public NexusClient(String url, String username, String password) {
        this.resource = createWebResource(url, username, password);
    }

    private WebResource createWebResource(String url, String username, String password) {
        Client client = Client.create(new DefaultClientConfig());
        client.setConnectTimeout(CONNECTION_TIMEOUT);
        client.setReadTimeout(READ_TIMEOUT);
        if (!username.isEmpty() && !password.isEmpty()) {
            client.addFilter(new HTTPBasicAuthFilter(username, password));
        }
        return client.resource(url);
    }

    public DockerRegistry getDockerRegistry() {
        // TODO: check http status code
        return resource.path("repository").path("docker-registry").path("v2").path("_catalog").accept(MediaType.APPLICATION_JSON).get(DockerRegistry.class);
    }

    // TODO: test should be prepared
    public List<String> getSpaces() {
        List<String> result = new ArrayList<>();

        for (String repo : this.getDockerRegistry().getRepositories()) {
            String space = repo.split("/")[0];
            if (space.equals("")) {
                continue;
            }

            result.add(space);
        }

        return result;
    }


    public ImageMetadata getImageMetadata(String repository) {
        // TODO: check http status code
        return resource.path("service").path("rest").path("beta").path("search").queryParam("docker.imageName", repository).accept(MediaType.APPLICATION_JSON).get(ImageMetadata.class);
    }

    public List<String> getImageNamesBySpaceName(String spaceName) {
        if (spaceName.trim().isEmpty()) {
            return null;
        }

        spaceName = spaceName + "/";

        List<String> result = new ArrayList<>();
        for (String repoName : this.getDockerRegistry().getRepositories()) {
            if (!repoName.startsWith(spaceName)) {
                continue;
            }

            result.add(repoName.substring(spaceName.length()));
        }

        Collections.sort(result);

        return result;
    }


    public List<String> getImageTags(String spaceName, String imageName) {
        if (spaceName.trim().isEmpty() || imageName.trim().isEmpty()) {
            return null;
        }

        List<String> result = new ArrayList<>();
        for (MetadataItem metadata : this.getImageMetadata(spaceName + "/" + imageName).getItems()) {
            result.add(metadata.getVersion());
        }

        Collections.sort(result);

        return result;
    }
}
