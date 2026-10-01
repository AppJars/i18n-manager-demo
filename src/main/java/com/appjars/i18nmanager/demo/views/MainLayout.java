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

import com.appjars.i18nmanager.demo.views.tour.DemoTours;
import com.appjars.i18nmanager.demo.views.tour.DemoTours.DemoTour;
import com.appjars.i18nmanager.flow.view.LanguageCrudView;
import com.appjars.i18nmanager.flow.view.TranslationItemCrudView;
import com.appjars.i18nmanager.service.LanguageService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.Arrays;
import java.util.Optional;
import org.vaadin.addons.themeselect.ThemeSelect;

// Anonymous so the public landing page (HomeView) can render inside this layout (the demo itself
// has no authentication)
@SuppressWarnings("serial")
@AnonymousAllowed
public class MainLayout extends AppLayout implements AfterNavigationObserver {

  private static final String THEME_LABEL_KEY = "appjars.i18nmanager.demo.mainlayout.theme";

  private H2 viewTitle;

  /** "This page" tour entry, only enabled while a view that has a tour is showing. */
  private MenuItem thisPageTourItem;

  /**
   * Translations entry of the drawer. Its target carries the default language, so it is refreshed
   * on every navigation instead of only when the drawer is built: the evaluator can make another
   * language the default from the languages view.
   */
  private SideNavItem translationsItem;

  private final LanguageService languageService;


  public MainLayout(LanguageService languageService) {
    this.languageService = languageService;

    setPrimarySection(Section.DRAWER);
    addDrawerContent();
    addHeaderContent();
  }

  private void addHeaderContent() {
    DrawerToggle toggle = new DrawerToggle();
    toggle.getElement().setAttribute("aria-label", "Menu toggle");

    viewTitle = new H2();
    viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

    MenuBar tourMenu = createTourMenu();
    tourMenu.getStyle().setMarginLeft("auto");

    ThemeSelect themeSelect = new ThemeSelect(getTranslation(THEME_LABEL_KEY));
    Tooltip.forComponent(themeSelect).setText(getTranslation(THEME_LABEL_KEY));
    themeSelect.addClassNames("minimal");
    themeSelect.getStyle().setPaddingRight("var(--lumo-space-m)");

    addToNavbar(true, toggle, viewTitle, tourMenu, themeSelect);
  }

  /**
   * Tour menu of the navigation bar, so a guided tour can be launched from any view and not only
   * from the landing page.
   */
  private MenuBar createTourMenu() {
    MenuBar menu = new MenuBar();
    menu.addClassName("tour-menu");
    menu.addThemeVariants(MenuBarVariant.LUMO_PRIMARY);
    menu.setOpenOnHover(true);
    SubMenu tours = menu
        .addItem(new Div(VaadinIcon.MAP_MARKER.create(), new Span(tourLabel("button"))))
        .getSubMenu();
    thisPageTourItem =
        tours.addItem(tourEntry(VaadinIcon.CROSSHAIRS, "home"), e -> startCurrentTour());
    tours.addSeparator();
    tours.addItem(tourEntry(VaadinIcon.HOME, "landing"), e -> startTour(DemoTour.HOME));
    tours.addItem(tourEntry(VaadinIcon.GLOBE, "languages"), e -> startTour(DemoTour.LANGUAGES));
    tours.addItem(tourEntry(VaadinIcon.EDIT, "translations"),
        e -> startTour(DemoTour.TRANSLATIONS));
    return menu;
  }

  /** Tour menu entry: the icon of the view the tour runs on, next to its label. */
  private Component tourEntry(VaadinIcon icon, String key) {
    Div entry = new Div(icon.create(), new Span(tourLabel(key)));
    entry.addClassName("tour-menu-item");
    return entry;
  }

  /** Reuses the tour labels of the landing page, so both menus stay in sync. */
  private String tourLabel(String key) {
    return getTranslation("appjars.i18nmanager.demo.home.tour." + key);
  }

  /**
   * Starts a tour: if its view is already showing it runs right away, otherwise it is stashed and
   * the view is navigated to, where {@link #startPendingTour()} picks it up. Every tour goes through
   * here, so they all share one host — which is what lets {@link #afterNavigation} stop whichever
   * one is running.
   */
  void startTour(DemoTour tour) {
    if (currentTour().filter(tour::equals).isPresent()) {
      DemoTours.start(tour, this, this::getTranslation);
      return;
    }
    VaadinSession.getCurrent().setAttribute(DemoTours.PENDING_TOUR_ATTRIBUTE, tour);
    getUI().ifPresent(ui -> {
      if (tour == DemoTour.TRANSLATIONS) {
        ui.navigate(DemoRoutes.translationsForDefaultLanguage(languageService));
      } else {
        ui.navigate(tourView(tour));
      }
    });
  }

  private void startCurrentTour() {
    currentTour().ifPresent(tour -> DemoTours.start(tour, this, this::getTranslation));
  }

  /** The tour of the view currently being shown, if it has one. */
  private Optional<DemoTour> currentTour() {
    if (getContent() == null) {
      return Optional.empty();
    }
    Class<?> current = getContent().getClass();
    return Arrays.stream(DemoTour.values()).filter(tour -> tourView(tour).equals(current))
        .findFirst();
  }

  /** Single source of truth mapping each tour to the view it runs on. */
  private static Class<? extends Component> tourView(DemoTour tour) {
    return switch (tour) {
      case HOME -> HomeView.class;
      case LANGUAGES -> LanguageCrudView.class;
      case TRANSLATIONS -> TranslationItemCrudView.class;
    };
  }

  private void addDrawerContent() {
    Image logo = new Image("/icons/icon.png", null);
    logo.setHeight("5vh");
    logo.setWidth("5vh");

    H3 appName = new H3(getTranslation("appjars.i18nmanager.demo.mainlayout.drawertitle"));

    Header header = new Header(logo, appName);
    header.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Gap.XSMALL,
        LumoUtility.AlignItems.CENTER, LumoUtility.Margin.MEDIUM);
    appName.addClassName(LumoUtility.Flex.GROW);

    Scroller scroller = new Scroller(createNavigation());


    VerticalLayout drawerContainer = new VerticalLayout(header, scroller);
    drawerContainer.getStyle().set("position", "relative");
    drawerContainer.setSizeFull();
    drawerContainer.setAlignItems(Alignment.STRETCH);
    drawerContainer.getStyle().set("overflow", "hidden");
    drawerContainer.setPadding(false);
    drawerContainer.setSpacing(false);
    drawerContainer.setFlexGrow(1, scroller);
    addToDrawer(drawerContainer);
  }

  private SideNav createNavigation() {
    SideNav nav = new SideNav();

    SideNavItem homeItem =
        new SideNavItem(
            getTranslation("appjars.i18nmanager.demo.mainlayout.home"), HomeView.class);
    homeItem.setPrefixComponent(VaadinIcon.HOME.create());

    SideNavItem i18nManagerItem =
        new SideNavItem(getTranslation("appjars.i18nmanager.demo.menuitem.i18nManagerItem"));
    i18nManagerItem.setPrefixComponent(VaadinIcon.GLOBE.create());
    i18nManagerItem.setExpanded(true);

    SideNavItem languagesItem =
        new SideNavItem(
            getTranslation("appjars.i18nmanager.demo.mainlayout.languages"),
            LanguageCrudView.class);
    languagesItem.setPrefixComponent(VaadinIcon.LIST.create());

    // The translations view takes the language as a route parameter, so it is linked by URL rather
    // than by class; refreshTranslationsTarget keeps that URL on the current default language.
    translationsItem =
        new SideNavItem(getTranslation("appjars.i18nmanager.demo.mainlayout.translations"));
    translationsItem.setPrefixComponent(VaadinIcon.EDIT.create());
    refreshTranslationsTarget();

    i18nManagerItem.addItem(languagesItem, translationsItem);

    nav.addItem(homeItem, i18nManagerItem);

    return nav;
  }

  /** Points the drawer's translations entry at the translations view of the default language. */
  private void refreshTranslationsTarget() {
    translationsItem.setPath(DemoRoutes.translationsForDefaultLanguage(languageService));
  }

  @Override
  public void afterNavigation(AfterNavigationEvent event) {
    // The engine paints on the document body and this layout survives navigation, so a tour the
    // user walked out of would keep dimming — and swallowing the clicks of — the view just opened.
    DemoTours.stop(this);
    updateTitle();
    refreshTranslationsTarget();
    thisPageTourItem.setEnabled(currentTour().isPresent());
    startPendingTour();
  }

  private void startPendingTour() {
    VaadinSession session = VaadinSession.getCurrent();
    if (session.getAttribute(DemoTours.PENDING_TOUR_ATTRIBUTE) instanceof DemoTour pending
        && currentTour().filter(pending::equals).isPresent()) {
      session.setAttribute(DemoTours.PENDING_TOUR_ATTRIBUTE, null);
      DemoTours.start(pending, this, this::getTranslation);
    }
  }

  /** Reflects the active view's dynamic title in the navbar, falling back to the app name. */
  private void updateTitle() {
    if (getContent() instanceof HasDynamicTitle hasTitle) {
      viewTitle.setText(hasTitle.getPageTitle());
    } else {
      viewTitle.setText(getTranslation("appjars.i18nmanager.demo.mainlayout.drawertitle"));
    }
  }
}
