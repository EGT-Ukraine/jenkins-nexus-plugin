package com.egt;

import com.egt.nexus.NexusClient;
import hudson.Extension;
import hudson.Launcher;
import hudson.XmlFile;
import hudson.model.*;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.Secret;
import jenkins.model.TransientActionFactory;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.Ancestor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class JenkinsNexusPlugin extends BuildWrapper {

    private String nexusUrl;
    private String nexusUser;
    private Secret nexusPassword;
    private String nexusSpace;

    private static final String TAG_SUFFIX = "_tag";
    private static final String ENV_NAME = "PROJECT_DATA";
    private static final String TAGS_SEPARATOR = ";";
    private static final char PROJECT_EMPTY_ITEM = ' ';
    private static final long CACHE_UPDATE_SECONDS_INTERVAL = 5;

    private static final Map<String, List<String>> projectNexusImages = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, String>> projectNexusTags = new ConcurrentHashMap<>();
    private static final Map<String, LocalTime> projectUpdateTime = new ConcurrentHashMap<>();

    private static final Logger LOGGER = Logger.getLogger(JenkinsNexusPlugin.class.getName());


    @DataBoundConstructor
    public JenkinsNexusPlugin(String nexusUrl, String nexusUser, Secret nexusPassword, String nexusSpace) {
        this.nexusUrl = nexusUrl;
        this.nexusUser = nexusUser;
        this.nexusPassword = nexusPassword;
        this.nexusSpace = nexusSpace;
    }

    public String getNexusUrl() {
        return nexusUrl;
    }

    public String getNexusUser() {
        return nexusUser;
    }

    public Secret getNexusPassword() {
        return nexusPassword;
    }

    public String getNexusSpace() {
        return nexusSpace;
    }

    private static void updateImagesAndTagsFromNexus(String projectName, String url, String user, String password, String space) {
        NexusClient nexusClient = new NexusClient(url, user, password);
        projectNexusImages.put(projectName, nexusClient.getImageNamesBySpaceName(space));

        if (projectNexusImages.size() == 0) {
            return;
        }

        Map<String, String> tags = new HashMap<>();
        for (String imageName : projectNexusImages.get(projectName)) {
            tags.put(String.format("%s%s", imageName, TAG_SUFFIX), String.join(TAGS_SEPARATOR, nexusClient.getImageTags(space, imageName)));
        }
        projectNexusTags.put(projectName, tags);
    }

    private static void updateProjectProperties(AbstractProject project) throws IOException {
        if (projectNexusTags.size() == 0) {
            return;
        }

        project.removeProperty(ParametersDefinitionProperty.class);
        project.addProperty(new ProjectChoiceParameters(PROJECT_EMPTY_ITEM, TAGS_SEPARATOR).createParams(projectNexusTags.get(project.getName())));
    }

    private static FreeStyleProject readProjectConfig(Project project) throws IOException {
        XmlFile cfg = project.getConfigFile();
        if (cfg.exists()) {
            return (FreeStyleProject) cfg.read();
        }

        return null;
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, hudson.model.BuildListener listener) {
        if (build.getBuildVariables().size() == 0) {
            listener.getLogger().println("ENVs are not configured");
        } else {
            listener.getLogger().print("-----\nENVs:\n");
            for (Object variable : build.getBuildVariables().entrySet()) {
                listener.getLogger().println(((Map.Entry<String, String>) variable).getKey() + ":" + ((Map.Entry<String, String>) variable).getValue());
            }
        }
        listener.getLogger().println("-----");

        return new Environment() {
            @Override
            public boolean tearDown(AbstractBuild build, hudson.model.BuildListener listener) throws IOException, InterruptedException {
                return super.tearDown(build, listener);
            }
        };
    }

    @Override
    public void makeBuildVariables(AbstractBuild build, Map<String, String> variables) {
        List<String> pluginVars = new ArrayList<>();
        Set<Map.Entry<String, String>> variablesCopy = new HashSet<>(variables.entrySet());
        for (Map.Entry<String, String> entry : variablesCopy) {
            if (entry.getKey().endsWith(TAG_SUFFIX)) {
                if (!entry.getValue().equals(Character.toString(PROJECT_EMPTY_ITEM))) {
                    pluginVars.add(String.format("%s:%s", entry.getKey().replace(TAG_SUFFIX, ""), entry.getValue()));
                }
                variables.remove(entry.getKey());
            }
        }

        variables.put(ENV_NAME, String.format("[%s]", String.join(", ", pluginVars)));
        super.makeBuildVariables(build, variables);
    }

    @Extension
    public static class PluginDescriptor extends BuildWrapperDescriptor {

        public PluginDescriptor() {
            super(JenkinsNexusPlugin.class);
            load();
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Setup Nexus' registry images";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            super.save();
            return super.configure(req, formData);
        }

        // create `build with parameters`
        @Override
        public BuildWrapper newInstance(@CheckForNull StaplerRequest req, @Nonnull JSONObject formData) throws FormException {
            if (req != null) {
                List<Ancestor> ancestors = req.getAncestors();
                for (Ancestor ancestor : ancestors) {
                    Object object = ancestor.getObject();
                    if (object instanceof AbstractProject<?, ?>) {
                        AbstractProject<?, ?> project = (AbstractProject<?, ?>) object;
                        try {
                            updateImagesAndTagsFromNexus(project.getName(), formData.getString("nexusUrl"),
                                    formData.getString("nexusUser"), formData.getString("nexusPassword"), formData.getString("nexusSpace"));
                            updateProjectProperties(project);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }
            }

            return super.newInstance(req, formData);
        }
    }

    @Extension
    public static class ProjectTransientActionFactory extends TransientActionFactory<Project> {

        private JenkinsNexusPlugin plugin;

        @Override
        public Class<Project> type() {
            return Project.class;
        }

        @Nonnull
        @Override
        public Collection<? extends Action> createFor(@Nonnull Project project) {
            LocalTime updatedTime = projectUpdateTime.get(project.getName());
            if (updatedTime != null && LocalTime.now().isBefore(updatedTime.plusSeconds(CACHE_UPDATE_SECONDS_INTERVAL))) {
                return project.getActions();
            }

            projectUpdateTime.put(project.getName(), LocalTime.now());
            try {
                plugin = readProjectConfig(project).getBuildWrappersList().get(JenkinsNexusPlugin.class);
                updateImagesAndTagsFromNexus(project.getName(), plugin.getNexusUrl(), plugin.getNexusUser(), plugin.getNexusPassword().getPlainText(), plugin.getNexusSpace());
            } catch (IOException | NullPointerException e) {
                LOGGER.warning(e.getMessage());
                return project.getActions();
            }

            LOGGER.info(String.format("IMAGES: %s", projectNexusImages.get(project.getName()).toString()));

            try {
                updateProjectProperties(project);
            } catch (IOException e) {
                LOGGER.warning(e.getMessage());
            }

            return project.getActions();
        }
    }
}
