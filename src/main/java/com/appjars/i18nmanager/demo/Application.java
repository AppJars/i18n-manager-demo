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

import com.appjars.i18nmanager.I18nManagerAutoConfiguration;
import com.appjars.i18nmanager.demo.views.MainLayout;
import com.appjars.i18nmanager.flow.util.RouteConfigurer;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.spring.annotation.EnableVaadin;
import com.vaadin.flow.theme.lumo.Lumo;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * The entry point of the Spring Boot application.
 *
 * <p>Use the @PWA annotation to make the application installable on phones, tablets and some desktop
 * browsers.
 */
@SuppressWarnings("serial")
@SpringBootApplication
@ComponentScan(basePackageClasses = I18nManagerAutoConfiguration.class)
@EnableVaadin({"com.appjars.i18nmanager.flow", "com.appjars.i18nmanager.demo"})
@EnableJpaRepositories(basePackageClasses = I18nManagerAutoConfiguration.class)
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet(Lumo.UTILITY_STYLESHEET)
@StyleSheet("styles.css")
@PWA(
    name = "I18n Manager Demo",
    shortName = "I18n Manager Demo",
    offlineResources = {"icons/icon.png"})
public class Application extends SpringBootServletInitializer implements AppShellConfigurator {

  @Autowired RouteConfigurer routeConfigurer;

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @PostConstruct
  public void configure() {
    routeConfigurer.setViewsRouterLayout(MainLayout.class);
  }

  /** {@link PWA} covers the installed app icon; the browser tab icon needs this declaration. */
  @Override
  public void configurePage(AppShellSettings settings) {
    settings.addFavIcon("icon", "icons/icon.png", "180x180");
  }
}
