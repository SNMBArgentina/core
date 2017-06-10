package org.geoladris;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.sf.json.JSONObject;

public class PluginDirsAnalyzerTest {
  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  @Test
  public void checkTest1() {
    Set<PluginDescriptor> plugins = getPluginDescriptors("test1");
    assertEquals(1, plugins.size());

    List<String> modules = new ArrayList<>();
    JSONObject defaultConf = new JSONObject();
    for (PluginDescriptor plugin : plugins) {
      modules.addAll(plugin.getModules());
      defaultConf.putAll(plugin.getConfiguration());
    }

    checkList(modules, "module1", "module2");
    JSONObject layout = defaultConf.getJSONObject("layout");
    JSONObject legend = defaultConf.getJSONObject("legend");

    assertEquals("29px", layout.getString("banner-size"));
    assertEquals(true, legend.getBoolean("show-title"));
  }

  @Test
  public void scanNoJavaPlugins() {
    Set<PluginDescriptor> plugins = getPluginDescriptors("testNoJava");
    assertEquals(3, plugins.size());
    for (PluginDescriptor plugin : plugins) {
      if (plugin.getName().equals("plugin1")) {
        checkList(plugin.getModules(), "plugin1/a", "plugin1/b");
      } else if (plugin.getName().equals("plugin2")) {
        checkList(plugin.getModules(), "plugin2/c");
      } else if (plugin.getName().equals("javaplugin")) {
        checkList(plugin.getModules(), "javaplugin/module-java");
      }
    }
  }

  @Test
  public void scanJavaNonRootModules() {
    String name = "testJavaNonRootModules";
    Set<PluginDescriptor> plugins = getPluginDescriptors(name);
    checkList(plugins.iterator().next().getModules(), name + "/module1");
  }

  @Test
  public void checkPluginDescriptors() {
    Set<PluginDescriptor> plugins = getPluginDescriptors("test1");
    assertEquals(1, plugins.size());
    PluginDescriptor plugin = plugins.iterator().next();
    checkList(plugin.getModules(), "module1", "module2");
    Set<String> modules = plugin.getModules();
    JSONObject defaultConf = plugin.getConfiguration();
    assertEquals(2, modules.size());
    assertTrue(modules.contains("module1"));
    assertTrue(modules.contains("module2"));
    assertTrue(defaultConf.containsKey("layout"));
    assertTrue(defaultConf.containsKey("legend"));
    assertFalse(defaultConf.containsKey("layer-list"));
  }

  @Test
  public void testResourcesInSubdirectories() {
    Set<PluginDescriptor> plugins = getPluginDescriptors("testSubdirectories");
    assertEquals(1, plugins.size());
    PluginDescriptor plugin = plugins.iterator().next();
    checkList(plugin.getModules(), "subdirs/lib/module");
  }

  @Test
  public void reuseAnalyzer() throws Exception {
    File p1 = tmp.newFolder("p1");
    IOUtils.write("{}", new FileOutputStream(new File(p1, "p1-conf.json")));

    PluginDirsAnalyzer analyzer = new PluginDirsAnalyzer(tmp.getRoot());
    Set<PluginDescriptor> plugins = analyzer.getPluginDescriptors();
    assertEquals(1, plugins.size());
    assertEquals("p1", plugins.iterator().next().getName());

    File p2 = tmp.newFolder("p2");
    IOUtils.write("{}", new FileOutputStream(new File(p2, "p2-conf.json")));

    analyzer.reload();
    plugins = analyzer.getPluginDescriptors();
    assertEquals(2, plugins.size());
    for (PluginDescriptor plugin : plugins) {
      assertTrue(plugin.getName().equals("p1") || plugin.getName().equals("p2"));
    }
  }

  @Test
  public void testNoPluginDescriptor() {
    Set<PluginDescriptor> plugins = getPluginDescriptors("testNoPluginDescriptor");
    assertEquals(1, plugins.size());
    checkList(plugins.iterator().next().getModules(), "plugin/module");
  }

  private PluginDirsAnalyzer getAnalyzer(String dir) {
    final String root = "src/test/resources/" + dir;
    return new PluginDirsAnalyzer(new File(root, "WEB-INF/classes"), new File(root, "plugins"));
  }

  private Set<PluginDescriptor> getPluginDescriptors(String dir) {
    return getAnalyzer(dir).getPluginDescriptors();
  }

  private void checkList(Collection<String> result, String... testEntries) {
    for (String entry : testEntries) {
      assertTrue(entry + " not in " + result, result.contains(entry));
    }
    assertEquals(result.size(), testEntries.length);
  }
}
