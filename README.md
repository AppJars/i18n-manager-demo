# I18N Manager Demo

A runnable demo of **I18N Manager**, the AppJar that lets an application manage its own languages
and translations *while it is running*. Translations live in the database and are served through a
Vaadin `I18NProvider`, so editing a value takes effect immediately — no properties file to change
and no redeploy.

The demo needs no login and no external services. It starts on a landing page that presents the
features and offers a guided tour of every view.

## What comes preloaded

On the first run the demo creates its database and seeds it with:

- **English**, set as the default language.
- The **texts of the landing page itself** as English translations. That way the translation editor
  is not empty on a fresh database — and editing one of those rows changes the landing page in
  front of you.

Every other key the application uses is deliberately left out: importing them is what the
**Scan missing keys** action demonstrates.

Two sample translation files are bundled under `src/main/resources/sample-translations/` and are
meant to be loaded through **Upload Values**:

| File | Language |
| --- | --- |
| `sample-translations/messages_es.properties` | Spanish |
| `sample-translations/messages_fr.properties` | French |

They translate both the demo's own texts and the AppJar's views. Note that in free mode the demo is
capped at 2 languages, so English plus **one** of the two fits; delete a language to try the other.

## Prerequisites

- **JDK 21** or newer
- **Maven 3.9** or newer

Dependencies resolve from public repositories only — Maven Central, the Vaadin repositories, and
`https://maven.appjars.com` for the AppJars artifacts. Nothing has to be built or installed locally.

## Running it

1. Clone the repository and enter it.
2. Start the application:

   ```bash
   mvn
   ```

   `spring-boot:run` is the default goal. The first start downloads the Vaadin frontend toolchain
   and can take a few minutes; later starts are quick.
3. Wait for `Started Application in N seconds` in the log.
4. Open <http://localhost:8080>.

Stop it with `Ctrl+C`. To start over from an empty database, stop the application and delete the
`data/` directory — the seed runs again on the next start.

## Using the demo

The landing page links to everything; the drawer on the left has **Home** and, under
**I18N Manager**, one entry per view.

### Languages

Lists the languages the application supports, with the key, name, optional region, and which one is
the default. **New Language** registers another one. The `⋮` menu on each row — also available by
right-clicking it — offers:

| Action | What it does |
| --- | --- |
| Set as default | Makes this language the fallback for missing translations |
| Edit language | Changes its key, name, or region |
| Translations items | Opens the translation editor for this language |
| Scan missing keys | Finds keys the application uses that are not in the database yet |
| Upload Values | Imports a standard `.properties` file into this language |
| Download Values | Exports this language as a `.properties` file |
| Delete language | Removes the language and its translations |

### Translations items

The editor for one language: each row is a translation key with its value. Double-click a row to
edit it inline. Filter by key or by translation text, or narrow the list to **Untranslated only**.
The **More** menu holds **New item**, **Scan missing keys**, and **Delete selected**.

### Guided tours

The **Guided tour** button in the top-right corner of the navigation bar is available from every
view. It offers a tour of the landing page, of the languages view, and of the translations view,
plus **This page** for whichever view is currently open.

### Seeing the demo translated

The demo renders in whichever of *its own* languages the browser asks for. To see it in Spanish or
French: create the language, load its sample file through **Upload Values**, put that language first
in your browser's language preferences, and open the demo in a new private window. The language is
negotiated once per session, so reloading the current tab keeps the language you are on.

The theme selector next to the tour button switches between light and dark.

## Configuration

All settings live in `src/main/resources/application.properties`. Every AppJar property has a
default, so none of them has to be set; the file lists them all with the value this demo runs on.

### View routes

| Property | Default | Description |
| --- | --- | --- |
| `com.appjars.i18nmanager.url.views.translationitems` | `i18n/items` | Base route of the translation items view |
| `com.appjars.i18nmanager.url.views.translationitemsparams` | `i18n/items/:lang` | Route of the translation items view for one language |
| `com.appjars.i18nmanager.url.views.languages` | `i18n/languages` | Route of the languages view |

### Resource bundle loading

| Property | Default | Description |
| --- | --- | --- |
| `com.appjars.i18nmanager.properties.encoding` | `UTF-8` | Charset used to decode `messages_<locale>.properties` on scan and upload. Set it to `ISO-8859-1` for bundles that rely on the legacy JDK decoding |

### Translation keys

| Property | Default | Description |
| --- | --- | --- |
| `com.appjars.i18nmanager.key.max-length` | `255` | Maximum length accepted for a translation key |

### Locale resolution

| Property | Default | Description |
| --- | --- | --- |
| `com.appjars.i18nmanager.default-locale` | `en` | Locale used when no better match is available |
| `com.appjars.i18nmanager.auto-resolve-locale` | `false` | Re-resolves the session locale from `Accept-Language` on every UI init, so a reload picks up a language just created. **The demo sets this to `true`** |
| `com.appjars.utils.i8n.supportedLocales` | `en` | Locales whose bundles are preloaded. Declared by `appjars-utils` and shared across AppJars — the `i8n` typo is part of the real name. **The demo sets this to `en,es,fr`** |

### Demo-only settings

| Property | Value | Description |
| --- | --- | --- |
| `spring.datasource.url` | `jdbc:h2:./data/i18nmanagerdb` | File-based H2, so your edits survive a restart |
| `spring.h2.console.enabled` | `true` | Browse the tables at <http://localhost:8080/h2-console> (user `sa`, no password). Set to `false` to turn it off |

## Free vs. full license

The demo runs unlicensed. Every feature is fully functional, capped at **2 languages** and **100
translations per language** — keys belonging to the AppJars views themselves do not count toward
that. A full license removes the caps and changes nothing else.

## Learn more

- [I18N Manager documentation](https://docs.appjars.com/i18n-manager/overview/)
- [AppJars on GitHub](https://github.com/AppJars)
- [appjars.com](https://www.appjars.com)
