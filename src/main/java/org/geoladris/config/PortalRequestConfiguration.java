package org.geoladris.config;

import java.io.File;

/**
 * Instance that provides services using the current request and portal configuration
 * 
 * @author fergonco
 */
public interface PortalRequestConfiguration {

  String localize(String template);

  File getConfigDir();

  /**
   * Whether the portal is configured to cache the configuration or not
   * 
   * @return
   */
  boolean usingCache();

}
