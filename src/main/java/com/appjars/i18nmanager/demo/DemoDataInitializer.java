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
package com.appjars.i18nmanager.demo;

import com.appjars.i18nmanager.model.LanguageDto;
import com.appjars.i18nmanager.service.I18nKeyService;
import com.appjars.i18nmanager.service.LanguageService;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds the default (English) language with the texts of the landing page, so a first-run evaluator
 * finds the translation editor populated instead of empty — and can edit a row and see the change on
 * the landing right away. Every other key the application uses is left out on purpose: loading them
 * is what the <em>Scan missing keys</em> action demonstrates.
 */
@Component
class DemoDataInitializer implements ApplicationRunner {

  /** The landing page's own texts: {@code HomeView} reads exactly these keys. */
  private static final String SEEDED_KEY_PREFIX = "appjars.i18nmanager.demo.home.";

  private static final String BUNDLE_RESOURCE = "/messages_en.properties";

  /** The seeded texts take no arguments; MessageFormat is applied only to resolve the escaping. */
  private static final Object[] NO_ARGUMENTS = new Object[0];

  private final LanguageService languageService;
  private final I18nKeyService keyService;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  DemoDataInitializer(LanguageService languageService, I18nKeyService keyService) {
    this.languageService = languageService;
    this.keyService = keyService;
  }

  @Override
  public void run(ApplicationArguments args) {
    try {
      // Creates and persists English as the default language when the database is empty.
      LanguageDto english = languageService.getDefaultLanguage();

      // Anything already stored — a previous run, or keys the evaluator loaded themselves — is
      // left untouched, so the demo can be restarted without undoing their work.
      if (!keyService.findAll().isEmpty()) {
        return;
      }

      Properties seed = landingPageTexts();
      List<String> skipped = keyService.persistAll(seed, english, false);
      logger.info("Seeded {} English translations for the landing page", seed.size() - skipped.size());
      if (!skipped.isEmpty()) {
        logger.warn("Skipped seed keys: {}", skipped);
      }
    } catch (Exception e) {
      // A failed seed must not keep the demo from starting: the views still work, just empty.
      logger.error("Could not seed the demo translations", e);
    }
  }

  /** Reads the English texts of the landing page from the demo's own resource bundle. */
  private Properties landingPageTexts() throws IOException {
    Properties bundle = new Properties();
    try (InputStream stream = getClass().getResourceAsStream(BUNDLE_RESOURCE)) {
      if (stream == null) {
        throw new IOException(BUNDLE_RESOURCE + " not found on the classpath");
      }
      try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
        bundle.load(reader);
      }
    }

    Properties texts = new Properties();
    bundle.stringPropertyNames().stream().filter(key -> key.startsWith(SEEDED_KEY_PREFIX))
        .forEach(key -> texts.setProperty(key, render(bundle.getProperty(key))));
    return texts;
  }

  /**
   * Renders a bundle value into the literal text to store. Bundle values are MessageFormat
   * patterns — a literal apostrophe is written doubled — while translations read back from the
   * database are served verbatim, so the pattern has to be resolved before it is seeded or the
   * escaping itself would show up on screen.
   */
  private static String render(String pattern) {
    return new MessageFormat(pattern, Locale.ENGLISH).format(NO_ARGUMENTS);
  }
}
