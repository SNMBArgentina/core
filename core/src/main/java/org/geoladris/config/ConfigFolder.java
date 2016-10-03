package org.geoladris.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;
import org.geoladris.ConfigurationException;

public class ConfigFolder {
  private static final Logger logger = Logger.getLogger(ConfigFolder.class);
  private File dir = null;
  private String rootPath;
  private String configInitParameter;
  private String contextPath;

  public ConfigFolder(String contextPath, String rootPath, String configInitParameter) {
    this.contextPath = contextPath;
    this.rootPath = rootPath;
    this.configInitParameter = configInitParameter;
  }

  public File getFilePath() {
    if (dir == null) {
      String defaultDir = rootPath + File.separator + "WEB-INF" + File.separator + "default_config";

      // Get the portal config dir property from Java system properties
      String portalConfigDir = System.getenv("PORTAL_CONFIG_DIR");
      if (portalConfigDir == null || portalConfigDir.trim().length() == 0) {
        portalConfigDir = System.getProperty("PORTAL_CONFIG_DIR");
      }

      // If not set in the system properties, get it from the Servlet
      // context parameters (web.xml)
      if (portalConfigDir == null)
        portalConfigDir = configInitParameter;

      // Otherwise:
      if (portalConfigDir == null) {
        // if not set already, use the default portal config dir
        logger.warn("PORTAL_CONFIG_DIR property not found. Using default config.");
        dir = new File(defaultDir);
      } else {
        // if set but not existing, use the default portal config dir
        dir = new File(portalConfigDir, contextPath);
        if (!dir.exists()) {
          logger.warn("PORTAL_CONFIG_DIR is set to " + dir.getAbsolutePath()
              + ", but it doesn't exist. Using default config.");
          dir = new File(defaultDir);
        }
      }

      logger.info("============================================================================");
      logger.info("PORTAL_CONFIG_DIR: " + dir.getAbsolutePath());
      logger.info("============================================================================");
    }

    return dir;
  }

  private File getPortalPropertiesFile() {
    return new File(getFilePath() + "/portal.properties");
  }

  private File getTranslationFolder() {
    return new File(getFilePath(), "messages");
  }

  public Properties getProperties() {
    File file = getPortalPropertiesFile();
    logger.debug("Reading portal properties file " + file);
    Properties properties = new Properties();
    try {
      properties.load(new FileInputStream(file));
    } catch (FileNotFoundException e) {
      logger.warn("Missing portal.properties file");
    } catch (IOException e) {
      logger.error("Error reading portal.properties file", e);
    }

    return properties;
  }

  public ResourceBundle getMessages(Locale locale) {
    URLClassLoader urlClassLoader;
    try {
      urlClassLoader = new URLClassLoader(new URL[] {getTranslationFolder().toURI().toURL()});
    } catch (MalformedURLException e) {
      logger.error("Something is wrong with the configuration directory", e);
      throw new ConfigurationException(e);
    }
    return ResourceBundle.getBundle("messages", locale, urlClassLoader);
  }

}
