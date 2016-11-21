//package org.geoladris.config;
//
//import static junit.framework.Assert.assertEquals;
//import static junit.framework.Assert.assertNotNull;
//import static junit.framework.Assert.assertTrue;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.HashSet;
//import java.util.Set;
//
//import org.geoladris.PluginDescriptor;
//import org.geoladris.PluginDescriptorFileReader;
//import org.geoladris.config.PluginDescriptors;
//import org.junit.Test;
//
//import net.sf.json.JSONObject;
//
//public class PluginDescriptorsTest {
//  private PluginDescriptorFileReader reader = new PluginDescriptorFileReader();
//
//  @Test
//  public void doesNotReturnDisabledPlugins() throws Exception {
//    PluginDescriptors pluginDescriptors =
//        new PluginDescriptors(Collections.<PluginDescriptor>emptySet());
//    pluginDescriptors.merge("p1", JSONObject
//        .fromObject("{_enabled : false, " + "m1 : { a : 1, b : 2}, m2 : { c : 1, d : 2}}"));
//    pluginDescriptors.merge("p2", JSONObject.fromObject("{_enabled : true, m3 : { a : 1, b : 2}}"));
//
//    PluginDescriptor[] descriptors = pluginDescriptors.getEnabled();
//
//    assertEquals(1, descriptors.length);
//    JSONObject pluginConf = descriptors[0].getConfiguration();
//    assertEquals(1, pluginConf.keySet().size());
//    assertEquals("m3", pluginConf.keySet().iterator().next());
//  }
//
//  @Test
//  public void pluginsEnabledByDefault() throws Exception {
//    PluginDescriptors pluginDescriptors =
//        new PluginDescriptors(Collections.<PluginDescriptor>emptySet());
//
//    pluginDescriptors.merge("p1",
//        JSONObject.fromObject("{ m1 : { a : 1, b : 2}, m2 : { c : 1, d : 2}}"));
//
//    PluginDescriptor[] descriptors = pluginDescriptors.getEnabled();
//    assertEquals(1, descriptors.length);
//  }
//
//  @Test
//  public void defaultConfigurationMergedByDefault() throws Exception {
//    checkConfigurationMerge("{ _enabled : true, m1 : { a : 10}}");
//  }
//
//  @Test
//  public void mergesDefaultConfIfSpecified() throws Exception {
//    checkConfigurationMerge("{ _override : false," + " _enabled : true, m1 : { a : 10}}");
//  }
//
//  private void checkConfigurationMerge(String pluginConfiguration) {
//    Set<PluginDescriptor> plugins = new HashSet<PluginDescriptor>();
//    PluginDescriptor pluginDescriptor =
//        reader.read("{default-conf:{ m1 : { a : 1, b : 2}, m2 : { c : 1, d : 2}}}", "p1");
//    plugins.add(pluginDescriptor);
//    PluginDescriptors pluginDescriptors = new PluginDescriptors(plugins);
//    pluginDescriptors.merge("p1", JSONObject.fromObject(pluginConfiguration));
//
//    JSONObject resultConfiguration = pluginDescriptors.getEnabled()[0].getConfiguration();
//    assertEquals(2, resultConfiguration.keySet().size());
//    assertEquals(10, resultConfiguration.getJSONObject("m1").get("a"));
//    assertEquals(2, resultConfiguration.getJSONObject("m1").get("b"));
//    assertEquals(1, resultConfiguration.getJSONObject("m2").get("c"));
//    assertEquals(2, resultConfiguration.getJSONObject("m2").get("d"));
//  }
//
//  @Test
//  public void overridesDefaultConfIfSpecified() throws Exception {
//    Set<PluginDescriptor> plugins =
//        createPluginSet("{default-conf:{ m1 : { a : 1, b : 2}, m2 : { c : 1, d : 2}}}", "p1");
//    PluginDescriptors pluginDescriptors = new PluginDescriptors(plugins);
//    pluginDescriptors.merge("p1",
//        JSONObject.fromObject("{ _override : true," + " _enabled : true, m1 : { a : 10}}"));
//
//    JSONObject resultConfiguration = pluginDescriptors.getEnabled()[0].getConfiguration();
//    assertEquals(1, resultConfiguration.keySet().size());
//    JSONObject m1Configuration = resultConfiguration.getJSONObject("m1");
//    assertEquals(1, m1Configuration.keySet().size());
//    assertEquals(10, m1Configuration.get("a"));
//  }
//
//  private Set<PluginDescriptor> createPluginSet(String pluginDefaultConfiguration,
//      String pluginName) {
//    Set<PluginDescriptor> plugins = new HashSet<PluginDescriptor>();
//    PluginDescriptor pluginDescriptor = reader.read(pluginDefaultConfiguration, pluginName);
//    plugins.add(pluginDescriptor);
//    return plugins;
//  }
//
//  @Test
//  public void doesNotReturnPseudomodules() throws Exception {
//    Set<PluginDescriptor> plugins = createPluginSet(
//        "{ installInRoot: false, default-conf:{m1 : { a : 1, b : 2}, m2 : { c : 1, d : 2}}}", "p1");
//    PluginDescriptors pluginDescriptors = new PluginDescriptors(plugins);
//
//    pluginDescriptors.merge("p1",
//        JSONObject.fromObject("{ _override : false, "//
//            + "_enabled : true, "//
//            + "m1 : { a : 1, b : 2}, "//
//            + "m2 : { c : 1, d : 2}}"));
//
//    PluginDescriptor[] descriptors = pluginDescriptors.getEnabled();
//    assertEquals(1, descriptors.length);
//    Set<?> moduleNames = descriptors[0].getConfiguration().keySet();
//    assertEquals(2, moduleNames.size());
//    assertTrue(moduleNames.contains("m1"));
//    assertTrue(moduleNames.contains("m2"));
//  }
//
//  @Test
//  public void includesPluginsFromDefaultConfIfAllEnabled() throws Exception {
//    Set<PluginDescriptor> plugins = createPluginSet("{default-conf:{ m2 : { b : 20 }}}", "p2");
//    PluginDescriptors pluginDescriptors = new PluginDescriptors(plugins);
//
//    pluginDescriptors.merge("p1", JSONObject.fromObject("{ m1 : { a : 10 }}"));
//
//    PluginDescriptor[] descriptors = pluginDescriptors.getEnabled();
//    assertEquals(2, descriptors.length);
//    int p1Index = descriptors[0].getName().equals("p1") ? 0 : 1;
//    int p2Index = 1 - p1Index;
//    assertEquals(10, descriptors[p1Index].getConfiguration().getJSONObject("m1").get("a"));
//    assertEquals(20, descriptors[p2Index].getConfiguration().getJSONObject("m2").get("b"));
//  }
//
//  @Test
//  public void qualifiedPluginModuleConfiguration() throws Exception {
//    Set<PluginDescriptor> plugins = createPluginSet("{"//
//        + " installInRoot:false,"//
//        + " default-conf:{ m2 : 0 }, "//
//        + "}", "plugin1");
//    PluginDescriptors pluginDescriptors = new PluginDescriptors(plugins);
//
//    JSONObject pluginConfig = pluginDescriptors.getEnabled()[0].getConfiguration();
//    assertTrue(pluginConfig.has("plugin1/m2"));
//  }
//
//  @Test
//  public void unqualifiedPluginModuleConfiguration() throws Exception {
//    Set<PluginDescriptor> plugins = createPluginSet("{"//
//        + " installInRoot:true,"//
//        + " default-conf:{ m2 : 0 }, "//
//        + "}", "plugin1");
//    PluginDescriptors pluginDescriptors = new PluginDescriptors(plugins);
//
//    JSONObject pluginConfig = pluginDescriptors.getEnabled()[0].getConfiguration();
//    assertTrue(pluginConfig.has("m2"));
//  }
//
//  /**
//   * Bug fix: a call to merge with a pluginName not found in the descriptors used to construct the
//   * PluginDescriptors instance causes the PluginDescriptor to have null name
//   */
//  @Test
//  public void nullNameWhenNewPluginConfigurationIsMerged() {
//    PluginDescriptors pluginDescriptors = new PluginDescriptors(createPluginSet("{"//
//        + " default-conf:{ m2 : 0 }, "//
//        + "}", "plugin1"));
//
//    pluginDescriptors.merge("plugin2", JSONObject.fromObject("{ m2 : 0 }"));
//
//    PluginDescriptor[] descriptors = pluginDescriptors.getEnabled();
//    assertEquals(2, descriptors.length);
//    ArrayList<String> pluginNames = new ArrayList<>();
//    Collections.addAll(pluginNames, "plugin1", "plugin2");
//    for (PluginDescriptor pluginDescriptor : descriptors) {
//      assertNotNull(pluginDescriptor.getName());
//      pluginNames.remove(pluginDescriptor.getName());
//    }
//    assertEquals(0, pluginNames.size());
//  }
//
//  @Test
//  public void qualifiedStyleWithSubdirectories() {
//    PluginDescriptor descriptor = new PluginDescriptor("myplugin");
//    descriptor.setInstallInRoot(false);
//    descriptor.addStylesheet("styles/subdirectory/style.css");
//    String style = descriptor.getStylesheets().iterator().next();
//    assertEquals("styles/myplugin/subdirectory/style.css", style);
//  }
//}
