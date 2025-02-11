package io.jenkins.plugins.jfrog;

import hudson.EnvVars;
import io.jenkins.plugins.jfrog.actions.JFrogCliConfigEncryption;
import org.jfrog.build.client.ProxyConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static io.jenkins.plugins.jfrog.CliEnvConfigurator.*;
import static org.junit.Assert.*;


/**
 * @author yahavi
 **/
public class CliEnvConfiguratorTest {
    ProxyConfiguration proxyConfiguration;
    EnvVars envVars;

    @Before
    public void setUp() {
        envVars = new EnvVars();
        envVars.put("JOB_NAME", "buildName");
        envVars.put("BUILD_NUMBER", "1");
        envVars.put("BUILD_URL", "https://acme.jenkins.io");
    }

    @Test
    public void configureCliEnvBasicTest() {
        invokeConfigureCliEnv("a/b/c", new JFrogCliConfigEncryption(envVars));
        assertEnv(envVars, JFROG_CLI_BUILD_NAME, "buildName");
        assertEnv(envVars, JFROG_CLI_BUILD_NUMBER, "1");
        assertEnv(envVars, JFROG_CLI_BUILD_URL, "https://acme.jenkins.io");
        assertEnv(envVars, JFROG_CLI_HOME_DIR, "a/b/c");
    }

    @Test
    public void configEncryptionTest() {
        JFrogCliConfigEncryption configEncryption = new JFrogCliConfigEncryption(envVars);
        assertTrue(configEncryption.shouldEncrypt());
        assertEquals(32, configEncryption.getKey().length());

        invokeConfigureCliEnv("a/b/c", configEncryption);
        assertEnv(envVars, JFROG_CLI_ENCRYPTION_KEY, configEncryption.getKey());
    }

    @Test
    public void configEncryptionWithHomeDirTest() {
        // Config JFROG_CLI_HOME_DIR to disable key encryption
        envVars.put(JFROG_CLI_HOME_DIR, "/a/b/c");
        JFrogCliConfigEncryption configEncryption = new JFrogCliConfigEncryption(envVars);
        invokeConfigureCliEnv("", configEncryption);

        assertFalse(configEncryption.shouldEncrypt());
        assertFalse(envVars.containsKey(JFROG_CLI_ENCRYPTION_KEY));
    }

    void assertEnv(EnvVars envVars, String key, String expectedValue) {
        assertEquals(expectedValue, envVars.get(key));
    }

    void invokeConfigureCliEnv() {
        this.invokeConfigureCliEnv("", new JFrogCliConfigEncryption(envVars));
    }

    void invokeConfigureCliEnv(String jfrogHomeTempDir, JFrogCliConfigEncryption configEncryption) {
        try (MockedStatic<Utils> mockController = Mockito.mockStatic(Utils.class)) {
            mockController.when(Utils::createProxyConfiguration).thenReturn(proxyConfiguration);
            configureCliEnv(envVars, jfrogHomeTempDir, configEncryption);
        }
    }
}
