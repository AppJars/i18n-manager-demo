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
package com.appjars.i18nmanager.demo.views.tour;

import com.appjars.i18nmanager.flow.util.TestIds;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.function.SerializableFunction;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.vaadin.addons.antlerflow.tour.EngineType;
import org.vaadin.addons.antlerflow.tour.Tour;
import org.vaadin.addons.antlerflow.tour.TourButton;
import org.vaadin.addons.antlerflow.tour.TourButtonType;
import org.vaadin.addons.antlerflow.tour.TourStep;

/**
 * Factory of the guided tours offered by the demo. Steps of the appjar's views are anchored to the
 * {@code data-testid} attributes those views expose through {@code TestIds}, and steps of the demo's
 * own landing page to its section ids. Either way the selector is resolved client-side to its first
 * <em>visible</em> match, so hidden duplicates (mobile filter dialogs, off-screen grid cells) cannot
 * steal a step.
 *
 * <p>The tour engine is always {@link EngineType#DRIVER}: Driver.js is MIT licensed, while
 * Shepherd.js is not free for commercial use and this demo is public.
 */
public final class DemoTours {

  /** Session attribute used to start a tour after navigating to its view. */
  public static final String PENDING_TOUR_ATTRIBUTE = DemoTours.class.getName() + ".pendingTour";

  static final String KEY_PREFIX = "appjars.i18nmanager.demo.tour.";

  /** Attribute the resolver below sets on the first visible match of each step's selector. */
  private static final String TARGET_ATTR = "data-antler-target";

  /**
   * Resolves every step's selector to its first <em>visible</em> match and tags it with
   * {@value #TARGET_ATTR}, keeping the tags in sync as the view re-renders. A selector that matches
   * nothing gets no tag, so Driver centers that step rather than dropping it. $0 is a JSON map of
   * {stepId: cssSelector}.
   */
  private static final String RESOLVE_TARGETS_JS =
      """
      const MAP = JSON.parse($0);
      const ATTR = '%s';
      const resolve = () => {
        Object.keys(MAP).forEach(id => {
          let pick = null;
          for (const el of document.querySelectorAll(MAP[id])) {
            const r = el.getBoundingClientRect();
            if (r.width > 4 && r.height > 4) { pick = el; break; }
          }
          document.querySelectorAll("[" + ATTR + "='" + id + "']")
              .forEach(el => { if (el !== pick) { el.removeAttribute(ATTR); } });
          if (pick && pick.getAttribute(ATTR) !== id) { pick.setAttribute(ATTR, id); }
        });
      };
      if (window.__antlerResolver) { window.__antlerResolver.stop(); }
      let scheduled = false;
      const schedule = () => { if (scheduled) { return; } scheduled = true;
        requestAnimationFrame(() => { scheduled = false; resolve(); }); };
      resolve();
      const obs = new MutationObserver(schedule);
      obs.observe(document.body, {childList: true, subtree: true, attributes: true,
          attributeFilter: ['hidden', 'style', 'class']});
      window.__antlerResolver = { stop() { obs.disconnect();
        document.querySelectorAll('[' + ATTR + ']').forEach(el => el.removeAttribute(ATTR));
        window.__antlerResolver = null; } };
      """
          .formatted(TARGET_ATTR);

  /**
   * Undoes two things that would break the tour while it runs: Driver forces
   * {@code overflow:hidden} on the highlighted element's parent (which clips its siblings, e.g. the
   * filter fields next to the highlighted button), and Vaadin's modal popover sets
   * {@code pointer-events:none} on the body (which would make the tour's own Next button
   * unclickable while the popover a step opens is showing).
   */
  private static final String TOUR_CSS_JS =
      """
      if (!document.getElementById('demo-tour-css')) {
        const style = document.createElement('style');
        style.id = 'demo-tour-css';
        style.textContent =
            'body :not(body):has(> .driver-active-element) { overflow: visible !important; }'
            + '.driver-popover { pointer-events: auto !important; }';
        document.head.appendChild(style);
      }
      """;

  /**
   * In Vaadin 25 every overlay opens through the native Popover API, so it paints in the browser
   * top layer, above all z-indexed content. The Driver popover is ordinary DOM, so the steps that
   * open a menu would be hidden behind it: promote the popover to the top layer as well, and
   * re-assert it whenever another overlay opens afterwards (top-layer paint order follows the last
   * {@code showPopover()} call).
   */
  private static final String PROMOTE_TOP_LAYER_JS =
      """
      if (window.__demoTourTopLayer) { window.__demoTourTopLayer.stop(); }
      const promote = () => document.querySelectorAll('.driver-popover').forEach(el => {
        if (el.getAttribute('popover') !== 'manual') { el.setAttribute('popover', 'manual'); }
        el.style.margin = '0';
        try { if (!el.matches(':popover-open')) { el.showPopover(); } } catch (e) {}
      });
      const reassert = () => { const el = document.querySelector('.driver-popover');
        if (el && el.matches(':popover-open')) {
          try { el.hidePopover(); el.showPopover(); } catch (e) {}
        } };
      const onToggle = (e) => { const t = e.target;
        if (e.newState === 'open' && t && t.classList
            && !t.classList.contains('driver-popover')) { reassert(); } };
      document.addEventListener('toggle', onToggle, true);
      const obs = new MutationObserver(promote);
      obs.observe(document.body, {childList: true, subtree: true});
      promote();
      window.__demoTourTopLayer = { stop() { obs.disconnect();
        document.removeEventListener('toggle', onToggle, true);
        document.querySelectorAll('.driver-popover[popover]').forEach(el => {
          try { el.hidePopover(); } catch (e) {} el.removeAttribute('popover'); });
        window.__demoTourTopLayer = null; } };
      """;

  /**
   * antler-tour has no per-step hook, so the steps that describe a menu deploy it from the client
   * instead. Driver replaces the whole {@code .driver-popover} element on every step and puts no
   * step id on it, so the active step is read from the hidden marker each step embeds in its
   * content — a replacement the observer below sees as an ordinary DOM change. $0 is a JSON map of
   * {stepId: selector of the element to click when that step is shown}.
   */
  private static final String MENU_HOOK_JS =
      """
      const MAP = JSON.parse($0);
      if (window.__demoTourMenus) { window.__demoTourMenus.stop(); }
      const closeOverlays = () => document
          .querySelectorAll('vaadin-menu-bar-overlay, vaadin-popover-overlay')
          .forEach(o => { o.opened = false; });
      const clickFirstVisible = (selector) => {
        for (const el of document.querySelectorAll(selector)) {
          const r = el.getBoundingClientRect();
          if (r.width > 4 && r.height > 4) { el.click(); return; }
        }
      };
      let current = null;
      const sync = () => {
        const marker = document.querySelector('.driver-popover [data-tour-step]');
        const id = marker ? marker.getAttribute('data-tour-step') : null;
        if (id === current) { return; }
        current = id;
        closeOverlays();
        const selector = id ? MAP[id] : null;
        if (selector) { setTimeout(() => clickFirstVisible(selector), 150); }
      };
      const obs = new MutationObserver(sync);
      obs.observe(document.body, {childList: true, subtree: true});
      window.__demoTourMenus = { stop() { obs.disconnect(); closeOverlays();
        window.__demoTourMenus = null; } };
      sync();
      """;

  /**
   * antler-tour registers a fresh pair of lifecycle listeners on every start and its facade offers
   * no way to take them off again, so each tour left its own pair behind: finishing the third tour
   * called the server three times over, twice through the element of a tour already detached.
   * Only one tour runs at a time here, so keeping the latest listener of each type is enough.
   */
  private static final String SINGLE_LISTENER_JS =
      """
      const AT = window.AntlerTour;
      if (AT && !AT.__demoSingleListener) {
        const latest = new Map();
        AT.on = (type, cb) => { latest.set(type, cb); };
        AT._emit = (type, ...args) => {
          const cb = latest.get(type);
          if (cb) { try { cb(...args); } catch (e) { console.error(e); } }
        };
        AT.__demoSingleListener = true;
      }
      """;

  /** Stops every client-side helper installed for the tour. */
  private static final String STOP_JS =
      """
      ['__antlerResolver', '__demoTourTopLayer', '__demoTourMenus']
          .forEach(k => { if (window[k]) { window[k].stop(); } });
      const css = document.getElementById('demo-tour-css');
      if (css) { css.remove(); }
      """;

  public enum DemoTour {
    HOME, LANGUAGES, TRANSLATIONS
  }

  /**
   * A step of a tour. {@code selector} is the real element selector, which the resolver tags on its
   * first visible match; {@code attach} tells whether the step anchors to (and highlights) that
   * match or renders centered.
   */
  private record StepDef(String key, String selector, String position, boolean attach) {

    String id() {
      return key.replace('.', '-');
    }

    /** Marker selector the step attaches to, or {@code null} when it renders centered. */
    String attachTo() {
      return attach && selector != null ? resolved(key) : null;
    }
  }

  private DemoTours() {}

  public static Tour create(DemoTour tour, SerializableFunction<String, String> translator) {
    List<StepDef> defs = steps(tour);
    List<TourStep> steps = new ArrayList<>();
    for (int i = 0; i < defs.size(); i++) {
      steps.add(step(defs.get(i), i == 0, i == defs.size() - 1, translator));
    }
    return Tour.builder().engineType(EngineType.DRIVER).steps(steps).showCancelButton(true)
        .allowClose(true).options(driverOptions(translator)).build();
  }

  /**
   * Options handed straight to Driver. The step counter is drawn by Driver itself, so it would stay
   * English unless its template is passed in translated. The translated value carries {@code {0}}
   * and {@code {1}} rather than Driver's own {@code {{current}}} tokens, because the appjar formats
   * every translation through {@code MessageFormat}, which rejects those braces.
   */
  private static Map<Object, Object> driverOptions(SerializableFunction<String, String> t) {
    String progress = t.apply(KEY_PREFIX + "progress").replace("{0}", "{{current}}")
        .replace("{1}", "{{total}}");
    return Map.of("progressText", progress);
  }

  /**
   * Creates the tour, attaches it to {@code host} and starts it, detaching it again once it is
   * completed or canceled. Any tour still running on {@code host} is stopped first.
   */
  public static void start(DemoTour tour, Component host,
      SerializableFunction<String, String> translator) {
    stop(host);
    Tour t = create(tour, translator);
    ComponentUtil.setData(host, Tour.class, t);
    host.getElement().appendChild(t.getElement());
    host.getElement().executeJs(TOUR_CSS_JS);
    host.getElement().executeJs(RESOLVE_TARGETS_JS, toJson(targets(tour)));
    // Before start(), which is what registers the listeners this collapses.
    host.getElement().executeJs(SINGLE_LISTENER_JS);
    t.addTourCompletedListener(e -> stop(host));
    t.addTourCanceledListener(e -> stop(host));
    t.start();
    // The languages and translations tours deploy a menu, which opens as a top-layer overlay.
    host.getElement().executeJs(PROMOTE_TOP_LAYER_JS);
    host.getElement().executeJs(MENU_HOOK_JS, toJson(menuActions(tour)));
  }

  /**
   * Stops the tour running on {@code host}, if there is one: tears down the engine, the client-side
   * helpers and the injected CSS, and detaches the tour component again. Safe to call when no tour
   * is running, and safe to reach from the tour's own completion event.
   */
  public static void stop(Component host) {
    Tour tour = ComponentUtil.getData(host, Tour.class);
    if (tour == null) {
      return;
    }
    // Cleared first: tearing the engine down fires the completed event, which lands back here.
    ComponentUtil.setData(host, Tour.class, null);
    // Before STOP_JS, so the helpers are still in place while the engine unwinds — the menu a step
    // deployed is closed by the hook as the popover goes away.
    tour.cancel();
    host.getElement().executeJs(STOP_JS);
    tour.getElement().removeFromParent();
  }

  private static List<StepDef> steps(DemoTour tour) {
    return switch (tour) {
      case HOME -> homeSteps();
      case LANGUAGES -> languagesSteps();
      case TRANSLATIONS -> translationsSteps();
    };
  }

  /**
   * Tour of the demo's own landing page: what the appjar does, and where to start. Its steps let
   * Driver place the popover, since the sections are far taller than a step box.
   */
  private static List<StepDef> homeSteps() {
    return List.of(
        centered("home.intro"),
        anchored("home.features", "#home-features"),
        anchored("home.tryit", "#home-tryit"),
        anchored("home.actions", ".home-actions"),
        anchored("home.license", "#home-license"));
  }

  /** Every tour opens with a centered step, so the view is laid out before anything is anchored. */
  private static List<StepDef> languagesSteps() {
    return List.of(
        centered("languages.intro"),
        anchored("languages.grid", testId(TestIds.LANGUAGES_GRID), "top"),
        anchored("languages.filters", testId(TestIds.LANGUAGES_FILTERS), "bottom"),
        anchored("languages.new", testId(TestIds.NEW_LANGUAGE_BUTTON), "bottom"),
        deployingMenu("languages.actions", testId(TestIds.LANGUAGE_ACTIONS_MENU)),
        // Closes the tour centered: it is about the browser, not about anything on this screen.
        centered("languages.locale"));
  }

  private static List<StepDef> translationsSteps() {
    return List.of(
        centered("translations.intro"),
        anchored("translations.grid", testId(TestIds.TRANSLATIONS_GRID), "top"),
        anchored("translations.filters", testId(TestIds.TRANSLATIONS_FILTERS), "bottom"),
        deployingMenu("translations.more", testId(TestIds.KEYS_OPTIONS_BUTTON)));
  }

  /** Step rendered centered on the screen, highlighting nothing. */
  private static StepDef centered(String key) {
    return new StepDef(key, null, null, false);
  }

  /** Step anchored to — and highlighting — the first visible match of {@code selector}. */
  private static StepDef anchored(String key, String selector, String position) {
    return new StepDef(key, selector, position, true);
  }

  /** Anchored step whose side Driver picks, for targets too large to have an obvious one. */
  private static StepDef anchored(String key, String selector) {
    return anchored(key, selector, null);
  }

  /**
   * Step that deploys a menu while it is shown. It stays centered on purpose: Driver anchors the
   * popover to the <em>closed</em> control, so whichever side is chosen, the menu unfolds right over
   * it. The selector is still resolved, so the hook clicks the element the step talks about.
   */
  private static StepDef deployingMenu(String key, String selector) {
    return new StepDef(key, selector, null, false);
  }

  /** {stepId: selector} map consumed by {@link #RESOLVE_TARGETS_JS}. */
  private static Map<String, String> targets(DemoTour tour) {
    Map<String, String> targets = new LinkedHashMap<>();
    steps(tour).stream().filter(def -> def.selector() != null)
        .forEach(def -> targets.put(def.id(), def.selector()));
    return targets;
  }

  /**
   * {stepId: selector to click} map consumed by {@link #MENU_HOOK_JS}. The selectors go through the
   * resolved marker, so the menu that gets deployed is exactly the one the step points at.
   */
  private static Map<String, String> menuActions(DemoTour tour) {
    return switch (tour) {
      case HOME -> Map.of();
      case LANGUAGES -> Map.of(stepId("languages.actions"),
          resolved("languages.actions") + " vaadin-menu-bar-button");
      case TRANSLATIONS -> Map.of(stepId("translations.more"), resolved("translations.more"));
    };
  }

  private static TourStep step(StepDef def, boolean first, boolean last,
      SerializableFunction<String, String> t) {
    List<TourButton> buttons = new ArrayList<>();
    if (!first) {
      buttons.add(TourButton.builder().label(t.apply(KEY_PREFIX + "btn.back")).secondary(true)
          .type(TourButtonType.PREVIOUS).build());
    }
    buttons.add(
        TourButton.builder().label(t.apply(KEY_PREFIX + (last ? "btn.done" : "btn.next")))
            .type(TourButtonType.NEXT).build());
    // Driver renders the content as HTML, so the hidden marker survives and lets the client-side
    // hooks tell which step is currently shown.
    String content = t.apply(KEY_PREFIX + def.key() + ".desc")
        + "<span hidden data-tour-step='" + def.id() + "'></span>";
    return TourStep.builder().id(def.id()).attachTo(def.attachTo()).position(def.position())
        .title(t.apply(KEY_PREFIX + def.key() + ".title")).content(content).buttons(buttons).build();
  }

  private static String stepId(String key) {
    return key.replace('.', '-');
  }

  private static String resolved(String key) {
    return "[" + TARGET_ATTR + "='" + stepId(key) + "']";
  }

  private static String testId(String id) {
    return "[data-testid='" + id + "']";
  }

  /**
   * Minimal JSON object writer: every key and value here is a step id or a CSS selector, and the
   * result travels as an {@code executeJs} parameter, so no escaping beyond this is needed.
   */
  private static String toJson(Map<String, String> map) {
    StringBuilder json = new StringBuilder("{");
    map.forEach((key, value) -> {
      if (json.length() > 1) {
        json.append(',');
      }
      json.append('"').append(key).append("\":\"").append(value).append('"');
    });
    return json.append('}').toString();
  }
}
