/*******************************************************************************
 * Crafter Studio Web-content authoring solution
 *     Copyright (C) 2007-2013 Crafter Software Corporation.
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.craftercms.studio.impl.v1.service.site;

import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.xml.XMLSerializer;
import org.apache.commons.lang.StringUtils;
import org.craftercms.studio.api.v1.constant.CStudioConstants;
import org.craftercms.studio.api.v1.log.Logger;
import org.craftercms.studio.api.v1.log.LoggerFactory;
import org.craftercms.studio.api.v1.service.ConfigurableServiceBase;
import org.craftercms.studio.api.v1.service.configuration.DeploymentEndpointConfig;
import org.craftercms.studio.api.v1.service.configuration.ServicesConfig;
import org.craftercms.studio.api.v1.service.configuration.SiteEnvironmentConfig;
import org.craftercms.studio.api.v1.service.content.ContentService;
import org.craftercms.studio.api.v1.service.site.SiteConfigNotFoundException;
import org.craftercms.studio.api.v1.service.site.SiteService;
import org.craftercms.studio.api.v1.to.*;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Note: consider renaming
 * A site in Crafter Studio is currently the name for a WEM project being managed.  
 * This service provides access to site configuration
 * @author russdanner
 */
public class SiteServiceImpl extends ConfigurableServiceBase implements SiteService {

	private final static Logger logger = LoggerFactory.getLogger(SiteServiceImpl.class);

	@Override
	public void register() {
		this._servicesManager.registerService(SiteService.class, this);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.craftercms.cstudio.alfresco.service.impl.ConfigurableServiceBase#
	 * getConfiguration(java.lang.String)
	 */
	protected TimeStamped getConfiguration(String key) {
		// key is not being used here
		return sitesConfig;
	}

	@Override
	protected void removeConfiguration(String key) {
		if (!StringUtils.isEmpty(key)) {
			sitesConfig = null;
		}
	}

	/*
      * (non-Javadoc)
      *
      * @seeorg.craftercms.cstudio.alfresco.service.impl.ConfigurableServiceBase#
      * loadConfiguration(java.lang.String)
      */
	@SuppressWarnings("unchecked")
	protected void loadConfiguration(String key) {
		String configLocation = _configPath.replaceFirst(CStudioConstants.PATTERN_SITE, key)
				.replaceFirst(CStudioConstants.PATTERN_ENVIRONMENT, environment);
		configLocation = configLocation + "/" + _configFileName;
		Document document = null;
		try {
			document = contentService.getContentAsDocument(configLocation);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		if (document != null) {
			Element root = document.getRootElement();
			SitesConfigTO config = new SitesConfigTO();
			Map<String, String> sitesMenu = loadMap(root.selectNodes("sites-menu/menu-item"));
			config.setSitesMenu(sitesMenu);
			Map<String, String> siteTypes = loadMap(root.selectNodes("site-types/site-type"));
			config.setSiteTypes(siteTypes);
			Map<String, String> repositoryTypes = loadMap(root.selectNodes("repository-types/repository-type"));
			config.setRepositoryTypes(repositoryTypes);
			config.setSitesLocation(root.valueOf("sites-location"));
			config.setLastUpdated(new Date());
			sitesConfig = config;
		}
	}

	/**
	 * create a map from the given list of nodes
	 *
	 * @param nodes
	 * @return a map of key and value pairs
	 */
	protected Map<String, String> loadMap(List<Node> nodes) {
		if (nodes != null && nodes.size() > 0) {
			Map<String, String> mapping = new HashMap<String, String>();
			for (Node node : nodes) {
				String key = node.valueOf("@key");
				String value = node.getText();
				mapping.put(key, value);
			}
			return mapping;
		} else {
			return new HashMap<String, String>(0);
		}
	}

	/**
	 * given a site ID return the configuration as a document
	 * This method allows extensions to add additional properties to the configuration that
	 * are not made available through the site configuration object
	 * @param site the name of the site
	 * @return a Document containing the entire site configuration
	 */
	public Document getSiteConfiguration(String site) 
	throws SiteConfigNotFoundException {
		return _siteServiceDAL.getSiteConfiguration(site);
	}

	@Override
	public JSON getConfiguration(String site, String path, boolean applyEnv) {
		//
		String configPath = "";
		if (StringUtils.isEmpty(site)) {
			configPath = this.configRoot + path;
		} else {
			if (applyEnv) {
				configPath = this.environmentConfigPath.replaceAll(CStudioConstants.PATTERN_SITE, site).replaceAll(
						CStudioConstants.PATTERN_ENVIRONMENT, environment)
						+ path;
			} else {
				configPath = this.sitesConfigPath + "/" + site + path;
			}
		}
		logger.debug("[SITESERVICE] loading configuration at " + configPath);
		String configContent = contentService.getContentAsString(configPath);

		JSON response = null;

		if (configContent != null) {
			configContent = configContent.replaceAll("\\n([\\s]+)?+", "");
			configContent = configContent.replaceAll("<!--(.*?)-->", "");
			XMLSerializer xmlSerializer = new XMLSerializer();
			response = xmlSerializer.read(configContent);
		} else {
			response = new JSONObject();
		}
		return response;
	}

	/**
	 * check if any of site configuration is updated and reload it
	 */
	protected void checkForUpdates() {
		if (sitesMappings == null) {
			loadSitesMappings();
		} else {
			if (this.sitesMappings != null) {
				for (String site : this.sitesMappings.keySet()) {
					if (site != null) {
						if (servicesConfig.isUpdated(site)) {
							logger.debug("[SITESERVICE] " + site + " configuration is updated. reloading it.");

							if (servicesConfig.siteExists(site) && this.sitesMappings.containsKey(site)) {
								SiteTO siteConfig = this.sitesMappings.get(site);
								loadSiteConfig(site, siteConfig);
							} else {
								this.sitesMappings.remove(site);
							}
						}
						if (environmentConfig.isUpdated(site)) {
							logger.debug("[SITESERVICE] " + site
									+ " environment configuration is updated. reloading it.");

							if (environmentConfig.exists(site) && this.sitesMappings.containsKey(site)) {
								SiteTO siteConfig = this.sitesMappings.get(site);
								loadSiteEnvironmentConfig(site, siteConfig);
							} else {
								this.sitesMappings.remove(site);
							}
						}
						if (deploymentEndpointConfig.isUpdated(site)) {
							logger.debug("[SITESERVICE] " + site
									+ " environment configuration is updated. reloading it.");
						}
						if (deploymentEndpointConfig.exists(site) && this.sitesMappings.containsKey(site)) {
							SiteTO siteConfig = this.sitesMappings.get(site);
							loadSiteDeploymentConfig(site, siteConfig);
						}
					}
				}
			}
		}
	}


	/**
	 * load sites mappings
	 */
	protected void loadSitesMappings() {
		Map<String, SiteTO> sitesMapping = new HashMap<>();
		ContentItemTO sitesTree = contentService.getContentItemTree(sitesConfigPath, 1);
		if (sitesTree != null && sitesTree.children.size() > 0) {
			for (ContentItemTO siteItem : sitesTree.children) {
				String site = siteItem.getName();
				logger.debug("[SITESERVICE] loading site configuration for " + site);

				SiteTO siteConfig = new SiteTO();
				siteConfig.setSite(site);
				siteConfig.setEnvironment(this.environment);
				this.loadSiteConfig(site, siteConfig);
				this.loadSiteEnvironmentConfig(site, siteConfig);
				this.loadSiteDeploymentConfig(site, siteConfig);
				sitesMapping.put(site, siteConfig);
			}
			if (this.sitesMappings != null) {
				this.sitesMappings.clear();
				this.sitesMappings = null;
			}
			this.sitesMappings = sitesMapping;
		} else {
			logger.warn("[SITESERVICE] no sites found at : " + this.sitesConfigPath);
		}
	}

	/**
	 * load site configuration info (not environment specific)
	 *
	 * @param site
	 * @param siteConfig
	 */
	protected void loadSiteConfig(String site, SiteTO siteConfig) {
		// get site configuration
		siteConfig.setWebProject(servicesConfig.getWemProject(site));
		siteConfig.setRepositoryRootPath("/wem-projects/" + site + "/" + site + "/work-area");
	}

	/***
	 * load site environment specific info
	 *
	 * @param site
	 * @param siteConfig
	 */
	protected void loadSiteEnvironmentConfig(String site, SiteTO siteConfig) {
		// get environment specific configuration
		logger.debug("Loading site environment configuration for " + site + "; Environemnt: " + environment);
		EnvironmentConfigTO environmentConfigTO = environmentConfig.getEnvironmentConfig(site);
		if (environmentConfig == null) {
			logger.error("Environment configuration for site " + site + " does not exist.");
			return;
		}
		siteConfig.setLiveUrl(environmentConfigTO.getLiveServerUrl());
		siteConfig.setAuthoringUrl(environmentConfigTO.getAuthoringServerUrl());
		siteConfig.setAuthoringUrlPattern(environmentConfigTO.getAuthoringServerUrlPattern());
		siteConfig.setPreviewUrl(environmentConfigTO.getPreviewServerUrl());
		siteConfig.setPreviewUrlPattern(environmentConfigTO.getPreviewServerUrlPattern());
		siteConfig.setAdminEmail(environmentConfigTO.getAdminEmailAddress());
		siteConfig.setCookieDomain(environmentConfigTO.getCookieDomain());
		siteConfig.setOpenSiteDropdown(environmentConfigTO.getOpenDropdown());
		siteConfig.setFormServerUrl(environmentConfigTO.getFormServerUrlPattern());
		siteConfig.setPublishingChannelGroupConfigs(environmentConfigTO.getPublishingChannelGroupConfigs());
	}

	/***
	 * load site environment specific info
	 *
	 * @param site
	 * @param siteConfig
	 */
	protected void loadSiteDeploymentConfig(String site, SiteTO siteConfig) {
		// get environment specific configuration
		logger.debug("Loading deployment configuration for " + site + "; Environment: " + environment);
		DeploymentConfigTO deploymentConfig = deploymentEndpointConfig.getSiteDeploymentConfig(site);
		if (deploymentConfig == null) {
			logger.error("Deployment configuration for site " + site + " does not exist.");
			return;
		}
		siteConfig.setDeploymentEndpointConfigs(deploymentConfig.getEndpointMapping());
	}

	/** getter site service dal */
	public SiteServiceDAL getSiteService() { return _siteServiceDAL; }
	/** setter site service dal */
	public void setSiteServiceDAL(SiteServiceDAL service) { _siteServiceDAL = service; }

	public ServicesConfig getServicesConfig() { return servicesConfig; }
	public void setServicesConfig(ServicesConfig servicesConfig) { this.servicesConfig = servicesConfig; }

	public ContentService getContentService() { return contentService; }
	public void setContentService(ContentService contentService) { this.contentService = contentService; }

	public String getSitesConfigPath() { return sitesConfigPath; }
	public void setSitesConfigPath(String sitesConfigPath) { this.sitesConfigPath = sitesConfigPath; }

	public String getEnvironment() { return environment; }
	public void setEnvironment(String environment) { this.environment = environment; }

	public SiteEnvironmentConfig getEnvironmentConfig() { return environmentConfig; }
	public void setEnvironmentConfig(SiteEnvironmentConfig environmentConfig) { this.environmentConfig = environmentConfig; }

	public DeploymentEndpointConfig getDeploymentEndpointConfig() { return deploymentEndpointConfig; }
	public void setDeploymentEndpointConfig(DeploymentEndpointConfig deploymentEndpointConfig) { this.deploymentEndpointConfig = deploymentEndpointConfig; }

	public String getConfigRoot() { return configRoot; }
	public void setConfigRoot(String configRoot) { this.configRoot = configRoot; }

	public String getEnvironmentConfigPath() { return environmentConfigPath; }
	public void setEnvironmentConfigPath(String environmentConfigPath) { this.environmentConfigPath = environmentConfigPath; }

	protected SiteServiceDAL _siteServiceDAL;
	protected ServicesConfig servicesConfig;
	protected ContentService contentService;
	protected String sitesConfigPath;
	protected String environment;
	protected SiteEnvironmentConfig environmentConfig;
	protected DeploymentEndpointConfig deploymentEndpointConfig;
	protected String configRoot = null;
	protected String environmentConfigPath = null;

	/**
	 * a map of site key and site information
	 */
	protected Map<String, SiteTO> sitesMappings;

	protected SitesConfigTO sitesConfig = null;
}