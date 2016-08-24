/*
 * nfms4redd Portal Interface - http://nfms4redd.org/
 *
 * (C) 2012, FAO Forestry Department (http://www.fao.org/forestry/)
 *
 * This application is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation;
 * version 3.0 of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
package org.fao.unredd.portal;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;

import org.fao.unredd.jwebclientAnalyzer.PluginDescriptor;

import net.sf.json.JSONObject;

public interface Config {

	public static final String PROPERTY_CLIENT_MODULES = "client.modules";
	public static final String PROPERTY_MAP_CENTER = "map.centerLonLat";
	public static final String PROPERTY_LANGUAGES = "languages";

	File getDir();

	Properties getProperties();

	/**
	 * Returns an array of <code>Map&lt;String, String&gt;</code>. For each
	 * element of the array, a {@link Map} is returned containing two
	 * keys/values: <code>code</code> (for language code) and <code>name</code>
	 * (for language name).
	 * 
	 * @return The array of languages or null if no language configuration is
	 *         found
	 */
	Map<String, String>[] getLanguages();

	ResourceBundle getMessages(Locale locale) throws ConfigurationException;

	/**
	 * Returns the property as an array or null if the property does not exist
	 * 
	 * @param property
	 * @return
	 */
	String[] getPropertyAsArray(String property);

	/**
	 * @return The language defined as default in the configuration or null if
	 *         no language is defined in the configuration
	 */
	String getDefaultLang();

	/**
	 * Plugin configuration provided by the list of
	 * {@link ModuleConfigurationProvider}. Only configuration for the active
	 * plugins are provided. Any disabled plugins won't be contained in the key
	 * set of the returning map.
	 * 
	 * @param locale
	 * @param request
	 * @return
	 */
	Map<PluginDescriptor, JSONObject> getPluginConfig(Locale locale,
			HttpServletRequest request);

	/**
	 * Add providers to modify the behavior of
	 * {@link #getPluginConfiguration(HttpServletRequest)} and
	 * 
	 * @param provider
	 */
	void addModuleConfigurationProvider(ModuleConfigurationProvider provider);

	/**
	 * Gets the plugin descriptor with the given name. If there is more than one
	 * plugin with the same name (which shouldn't happen) it returns one of them
	 * arbitrarily.
	 * 
	 * @param name
	 *            The name of the plugin.
	 * @return The plugin descriptor.
	 */
	PluginDescriptor getPlugin(String name);
}
