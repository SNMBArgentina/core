package org.geoladris.config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.geoladris.Geoladris;
import org.geoladris.PluginDescriptor;
import org.geoladris.PluginDescriptorFileReader;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import net.sf.json.JSONObject;

public class AbstractConfigTest {
  @Mock
  private HttpServletRequest request;
  @Mock
  private ServletContext context;
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testConfigurationProvidersMerge() throws Exception {
    Set<PluginDescriptor> plugins = new HashSet<>();
    PluginDescriptor plugin1 = new PluginDescriptor("1", true);
    plugins.add(plugin1);

    JSONObject conf1 = JSONObject.fromObject("{ module : { a : 1, b : 2 }}");
    JSONObject conf2 = JSONObject.fromObject("{ module : { a : 10, c : 3 }}");

    ModuleConfigurationProvider provider1 = mock(ModuleConfigurationProvider.class);
    when(provider1.getPluginConfig(any(PortalRequestConfiguration.class),
        any(HttpServletRequest.class))).thenReturn(Collections.singletonMap("1", conf1));
    ModuleConfigurationProvider provider2 = mock(ModuleConfigurationProvider.class);
    when(provider2.getPluginConfig(any(PortalRequestConfiguration.class),
        any(HttpServletRequest.class))).thenReturn(Collections.singletonMap("1", conf2));

    when(context.getAttribute(Geoladris.ATTR_CONFIG_PROVIDERS))
        .thenReturn(Arrays.asList(provider1, provider2));
    Config config = new FilesConfig(folder.getRoot(), context, request, plugins, false, -1);

    PluginDescriptor[] c = config.getPluginConfig(Locale.getDefault());

    JSONObject pluginConf = c[0].getConfiguration().getJSONObject("module");

    assertTrue(pluginConf.has("a") && pluginConf.has("b") && pluginConf.has("c"));
    assertEquals(3, pluginConf.get("c"));
    assertEquals(2, pluginConf.get("b"));
    // Providers should be applied in order
    assertEquals(10, pluginConf.get("a"));
  }

  @Test
  public void testCache() throws Exception {
    String defaultLang = "es";
    Locale locale = new Locale(defaultLang);
    Properties firstProperties = new Properties();
    firstProperties.put("languages", "{\"es\": \"Espa\u00f1ol\"}");
    firstProperties.put("languages.default", defaultLang);
    Config config =
        buildConfigReadOnceAndChangeFolderConfig(true, -1, defaultLang, locale, firstProperties);

    // Check we still have the same values
    assertTrue(config.getDefaultLang().equals(defaultLang));
    assertTrue(config.getLanguages()[0].get("code").equals("es"));
    assertTrue(config.getProperties().equals(firstProperties));
  }

  @Test
  public void testNoCache() throws Exception {
    String defaultLang = "es";
    Locale locale = new Locale(defaultLang);
    Properties firstProperties = new Properties();
    firstProperties.put("languages", "{\"es\": \"Espa\u00f1ol\"}");
    firstProperties.put("languages.default", defaultLang);
    Config config =
        buildConfigReadOnceAndChangeFolderConfig(false, -1, defaultLang, locale, firstProperties);

    assertFalse(config.getDefaultLang().equals(defaultLang));
    assertFalse(config.getLanguages()[0].get("code").equals("es"));
    assertFalse(config.getProperties() == firstProperties);
  }

  @Test
  public void testCacheTimeout() throws Exception {
    String defaultLang = "es";

    Properties firstProperties = new Properties();
    firstProperties.put("languages", "{\"es\": \"Espa\u00f1ol\"}");
    firstProperties.put("languages.default", defaultLang);

    int cacheTimeout = 1;
    Config config = buildConfigReadOnceAndChangeFolderConfig(true, cacheTimeout, defaultLang,
        new Locale(defaultLang), firstProperties);

    // Check we still have the same values
    assertTrue(config.getDefaultLang().equals(defaultLang));
    assertTrue(config.getLanguages()[0].get("code").equals("es"));
    assertTrue(config.getProperties().equals(firstProperties));

    Thread.sleep(cacheTimeout * 1500);

    // Check values changed
    assertFalse(config.getDefaultLang().equals(defaultLang));
    assertFalse(config.getLanguages()[0].get("code").equals("es"));
    assertFalse(config.getProperties() == firstProperties);
  }

  private Config buildConfigReadOnceAndChangeFolderConfig(boolean useCache, int cacheTimeout,
      String defaultLang, Locale locale, Properties firstProperties) throws IOException {
    File portalProperties = new File(folder.getRoot(), "portal.properties");
    firstProperties.store(new FileOutputStream(portalProperties), "");

    Config config =
        new FilesConfig(folder.getRoot(), context, request, null, useCache, cacheTimeout);

    assertTrue(config.getDefaultLang().equals(defaultLang));
    assertTrue(config.getLanguages()[0].get("code").equals("es"));
    assertTrue(config.getProperties().equals(firstProperties));

    Properties secondProperties = new Properties();
    secondProperties.put("languages", "{\"fr\": \"Frances\"}");
    secondProperties.put("languages.default", "fr");
    secondProperties.store(new FileOutputStream(portalProperties), "");
    return config;
  }

  @Test
  public void testPluginConfigurationCached() throws Exception {
    readPluginConfigurationTwice(true, true, 1);
  }

  @Test
  public void testPluginConfigurationCacheIgnoredIfProviderCannotBeCached() throws Exception {
    readPluginConfigurationTwice(true, false, 2);
  }

  @Test
  public void testPluginConfigurationCacheIgnoredIfCacheDisabled() throws Exception {
    readPluginConfigurationTwice(false, true, 2);
  }

  private void readPluginConfigurationTwice(boolean useCache, boolean canBeCached, int numCalls)
      throws IOException {
    // Install configuration provider
    ModuleConfigurationProvider provider = mock(ModuleConfigurationProvider.class);
    when(provider.canBeCached()).thenReturn(canBeCached);

    Config config = new FilesConfig(null, context, request,
        Collections.<PluginDescriptor>emptySet(), useCache, -1);
    when(context.getAttribute(Geoladris.ATTR_CONFIG_PROVIDERS)).thenReturn(Arrays.asList(provider));

    // Call twice
    config.getPluginConfig(Locale.getDefault());
    config.getPluginConfig(Locale.getDefault());

    // Check num calls
    verify(provider, times(numCalls)).getPluginConfig(any(PortalRequestConfiguration.class),
        any(HttpServletRequest.class));
  }

  @Test
  public void testNoConfigurationFolder() throws Exception {
    File portalProperties = new File(folder.getRoot(), "portal.properties");
    new Properties().store(new FileOutputStream(portalProperties), "");

    Config config = new FilesConfig(folder.getRoot(), context, request,
        Collections.<PluginDescriptor>emptySet(), false, -1);
    when(context.getAttribute(Geoladris.ATTR_CONFIG_PROVIDERS)).thenReturn(Arrays.asList());
    assertNotNull(config.getDir());
    assertNotNull(config.getPluginConfig(Locale.getDefault()));
    assertNotNull(config.getProperties());
    assertNotNull(config.getMessages(Locale.getDefault()));
    assertNotNull(config.getDefaultLang());
  }

  @Test
  public void testFailingConfigurationProvider() throws Exception {
    Config config = new FilesConfig(mock(File.class), context, request,
        Collections.<PluginDescriptor>emptySet(), false, -1);
    ModuleConfigurationProvider provider = mock(ModuleConfigurationProvider.class);
    when(provider.getPluginConfig(any(PortalRequestConfiguration.class),
        any(HttpServletRequest.class))).thenThrow(new IOException("mock"));
    when(context.getAttribute(Geoladris.ATTR_CONFIG_PROVIDERS)).thenReturn(Arrays.asList(provider));
    assertNotNull(config.getPluginConfig(Locale.getDefault()));
  }

  @Test
  public void testMergeDoesNotAffectDefaultPluginConfiguration() throws IOException {
    Set<PluginDescriptor> plugins = new HashSet<PluginDescriptor>();
    PluginDescriptor pluginDescriptor =
        new PluginDescriptorFileReader().read("{default-conf:{m1:true}}", "p1");
    plugins.add(pluginDescriptor);
    Config config = new FilesConfig(mock(File.class), context, request, plugins, false, -1);

    Map<String, JSONObject> mergingConfiguration1 = new HashMap<String, JSONObject>();
    mergingConfiguration1.put("p1", JSONObject.fromObject("{m2:true}"));
    Map<String, JSONObject> mergingConfiguration2 = new HashMap<String, JSONObject>();
    mergingConfiguration2.put("p1", JSONObject.fromObject("{}"));
    ModuleConfigurationProvider provider = mock(ModuleConfigurationProvider.class);
    when(provider.getPluginConfig(any(PortalRequestConfiguration.class),
        any(HttpServletRequest.class))).thenReturn(mergingConfiguration1)
            .thenReturn(mergingConfiguration2);
    when(context.getAttribute(Geoladris.ATTR_CONFIG_PROVIDERS)).thenReturn(Arrays.asList(provider));

    JSONObject configuration = config.getPluginConfig(Locale.ROOT)[0].getConfiguration();
    assertEquals(2, configuration.keySet().size());

    configuration = config.getPluginConfig(Locale.ROOT)[0].getConfiguration();
    assertEquals(1, configuration.keySet().size());
  }

  @Test
  public void updatesPlugins() {
    Set<PluginDescriptor> plugins = new HashSet<PluginDescriptor>();
    PluginDescriptor plugin = new PluginDescriptor("p1", true);
    plugin.addModule("m1");
    plugins.add(plugin);

    Config config = new FilesConfig(mock(File.class), context, request, plugins, false, -1);
    when(context.getAttribute(Geoladris.ATTR_CONFIG_PROVIDERS)).thenReturn(Collections.EMPTY_LIST);
    PluginDescriptor[] pluginConfig = config.getPluginConfig(Locale.getDefault());
    assertEquals(1, pluginConfig.length);
    assertEquals("p1", pluginConfig[0].getName());
    assertEquals(1, pluginConfig[0].getModules().size());
    assertEquals("m1", pluginConfig[0].getModules().iterator().next());

    plugins = new HashSet<PluginDescriptor>();
    plugin = new PluginDescriptor("p2", false);
    plugin.addModule("m2");
    plugins.add(plugin);

    config.setPlugins(plugins);
    pluginConfig = config.getPluginConfig(Locale.getDefault());
    assertEquals(1, pluginConfig.length);
    assertEquals("p2", pluginConfig[0].getName());
    assertEquals(1, pluginConfig[0].getModules().size());
    assertEquals("p2/m2", pluginConfig[0].getModules().iterator().next());
  }

  @Test
  public void ignoresConfigurationForNonExistingPlugins() throws IOException {
    Set<PluginDescriptor> plugins = Collections.singleton(new PluginDescriptor("p1", true));
    Config config = new FilesConfig(mock(File.class), context, request, plugins, false, -1);

    ModuleConfigurationProvider provider = mock(ModuleConfigurationProvider.class);
    Map<String, JSONObject> providerConf =
        Collections.singletonMap("another_plugin", new JSONObject());
    when(provider.getPluginConfig(any(PortalRequestConfiguration.class),
        any(HttpServletRequest.class))).thenReturn(providerConf);
    when(context.getAttribute(Geoladris.ATTR_CONFIG_PROVIDERS)).thenReturn(Arrays.asList(provider));

    PluginDescriptor[] pluginConfig = config.getPluginConfig(Locale.getDefault());
    assertEquals(1, pluginConfig.length);
  }
}