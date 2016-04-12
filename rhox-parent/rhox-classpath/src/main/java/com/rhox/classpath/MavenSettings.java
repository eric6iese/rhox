/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rhox.classpath;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilder;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.eclipse.aether.repository.AuthenticationSelector;
import org.eclipse.aether.repository.MirrorSelector;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.ConservativeAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;

/**
 * Reads the MavenSettings of the System and allows queries on the 'effective'
 * settings created out of the global and the user-specific file.<br/>
 * Currently it is not possible to also use a project-specific file, but this
 * functionality might be added as well as soon as it is defined.<br/>
 * Please note that the only reason to ever use the maven settings is to get the
 * registered network connections and the like - it just 'hijacks' the default
 * maven config to avoid even more configuration just for some simple scripts.
 */
class MavenSettings {

    private static final File USER_MAVEN_HOME = new File(System.getProperty("user.home"), ".m2");
    private static final File DEFAULT_LOCAL_REPOSITORY = new File(USER_MAVEN_HOME, "repository");
    private static final RemoteRepository DEFAULT_REMOTE_REPOSITORY = new RemoteRepository.Builder("maven-central", "default", "http://repo1.maven.org/maven2/").build();

    public static MavenSettings getDefault() {
        String envMavenHome = System.getenv("M2_HOME");
        File userSettings = new File(USER_MAVEN_HOME, "settings.xml");
        File globalSettings = new File(System.getProperty("maven.home", envMavenHome != null ? envMavenHome : ""), "conf/settings.xml");

        SettingsBuildingRequest settingsBuildingRequest = new DefaultSettingsBuildingRequest();
        settingsBuildingRequest.setSystemProperties(System.getProperties());
        settingsBuildingRequest.setUserSettingsFile(userSettings);
        settingsBuildingRequest.setGlobalSettingsFile(globalSettings);

        DefaultSettingsBuilderFactory mvnSettingBuilderFactory = new DefaultSettingsBuilderFactory();
        DefaultSettingsBuilder settingsBuilder = mvnSettingBuilderFactory.newInstance();
        Settings settings;
        try {
            SettingsBuildingResult settingsBuildingResult = settingsBuilder.build(settingsBuildingRequest);
            settings = settingsBuildingResult.getEffectiveSettings();
        } catch (SettingsBuildingException unexpected) {
            throw new IllegalStateException(unexpected);
        }
        return new MavenSettings(settings);
    }

    private final Settings settings;

    private MavenSettings(Settings settings) {
        this.settings = settings;
    }

    public List<RemoteRepository> getRemoteRepositories() {
        Map<String, Profile> profiles = settings.getProfilesAsMap();
        List<RemoteRepository> remotes = new ArrayList<>();
        for (String profileName : settings.getActiveProfiles()) {
            Profile profile = profiles.get(profileName);
            List<Repository> repositories = profile.getRepositories();
            for (Repository repo : repositories) {
                remotes.add(new RemoteRepository.Builder(repo.getId(), "default", repo.getUrl()).build());
            }
        }
        if (remotes.isEmpty()) {
            remotes.add(new RemoteRepository.Builder(DEFAULT_REMOTE_REPOSITORY).build());
        }
        return Collections.unmodifiableList(remotes);
    }

    public File getLocalRepository() {
        if (settings.getLocalRepository() != null) {
            return new File(settings.getLocalRepository());
        }
        return new File(new File(System.getProperty("user.home"), ".m2"), "repository");
    }

    public ProxySelector getProxySelector() {
        DefaultProxySelector selector = new DefaultProxySelector();
        settings.getProxies().stream().
                filter(org.apache.maven.settings.Proxy::isActive).
                forEach(proxy -> {
                    AuthenticationBuilder auth = new AuthenticationBuilder();
                    auth.addUsername(proxy.getUsername()).addPassword(proxy.getPassword());
                    selector.add(new org.eclipse.aether.repository.Proxy(proxy.getProtocol(), proxy.getHost(),
                            proxy.getPort(), auth.build()),
                            proxy.getNonProxyHosts());
                });
        return selector;
    }

    public MirrorSelector getMirrorSelector() {
        DefaultMirrorSelector selector = new DefaultMirrorSelector();
        for (org.apache.maven.settings.Mirror mirror : settings.getMirrors()) {
            selector.add(String.valueOf(mirror.getId()), mirror.getUrl(), mirror.getLayout(), false,
                    mirror.getMirrorOf(), mirror.getMirrorOfLayouts());
        }
        return selector;
    }

    public AuthenticationSelector getAuthSelector() {
        DefaultAuthenticationSelector selector = new DefaultAuthenticationSelector();
        for (Server server : settings.getServers()) {
            AuthenticationBuilder auth = new AuthenticationBuilder();
            auth.addUsername(server.getUsername()).addPassword(server.getPassword());
            auth.addPrivateKey(server.getPrivateKey(), server.getPassphrase());
            selector.add(server.getId(), auth.build());
        }
        return new ConservativeAuthenticationSelector(selector);
    }

    public boolean isOffline() {
        return settings.isOffline();
    }

    public DependencyConfiguration toConfig() {
        return new DependencyConfiguration(getLocalRepository(), getRemoteRepositories(), getProxySelector(), getMirrorSelector(), getAuthSelector(), isOffline());
    }
}
