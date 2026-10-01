/*-
 * #%L
 * I18n Manager AppJars - Demo
 * %%
 * Copyright (C) 2023 - 2026 AppJars
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.appjars.i18nmanager.demo.views;

import com.appjars.i18nmanager.model.LanguageDto;
import com.appjars.i18nmanager.service.LanguageService;
import com.appjars.i18nmanager.flow.view.TranslationItemCrudView;
import com.vaadin.flow.router.RouteConfiguration;

/** Routes of the appjar views that need building, shared by the landing page and the layout. */
final class DemoRoutes {

  private DemoRoutes() {}

  /**
   * URL of the translations view for the default language. The view requires a language route
   * parameter, so it is opened for the default language, mirroring how the languages view links to
   * it.
   */
  static String translationsForDefaultLanguage(LanguageService languageService) {
    LanguageDto language = languageService.getDefaultLanguage();
    String region = language.getLanguageRegion();
    String langParameter = (region == null || region.isEmpty())
        ? language.getLanguageKey()
        : language.getLanguageKey() + "_" + region;
    return RouteConfiguration.forSessionScope()
        .getUrl(TranslationItemCrudView.class, langParameter);
  }
}
