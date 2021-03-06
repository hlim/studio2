/*******************************************************************************
 * Crafter Studio Web-content authoring solution
 *     Copyright (C) 2007-2016 Crafter Software Corporation.
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
package org.craftercms.studio.impl.v1.service.translation.provider.smartling;

import java.io.InputStream;
import org.craftercms.studio.impl.v1.service.translation.TranslationProvider;

/**
 * Translation provider for Smartling translation API
 * @author rdanner
 */
public class SmartlingTranslationProvider implements TranslationProvider {

	@Override
	public void translate(String sourceSite, String sourceLanguage, String targetLanguage, String filename, InputStream content) {
		System.out.println("submitting job for translation");
	}

	@Override
	public int getTranslationStatusForItem(String sourceSite, String targetLanguage, String path) {
		return 0;
	}

	@Override
	public InputStream getTranslatedContentForItem(String sourceSite, String targetLanguage, String path) {
		return null;
	}
}
