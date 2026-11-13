package de.jsc3r.pass2sec.migration.boundary;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.*;

import de.jsc3r.pass2sec.migration.control.port.Website;
import de.jsc3r.pass2sec.migration.entity.Rotation;
import de.jsc3r.pass2sec.profile.entity.Action;
import de.jsc3r.pass2sec.profile.entity.Profile;
import de.jsc3r.pass2sec.shared.entity.Failure;

public class BrowserWebsite implements Website {
    @Override
    public void change(Profile profile, Rotation rotation, boolean headless) {
        execute(profile, rotation, headless, false);
    }

    @Override
    public void verify(Profile profile, Rotation rotation, boolean headless) {
        execute(profile, rotation, headless, true);
    }

    private void execute(
            Profile profile, Rotation rotation, boolean headless, boolean verification) {
        profile.checkMode(headless);
        // Each run uses a new browser/context, so verification cannot reuse an authenticated
        // session.
        try (var playwright = Playwright.create();
                var browser =
                        playwright
                                .chromium()
                                .launch(new BrowserType.LaunchOptions().setHeadless(headless));
                var context =
                        browser.newContext(
                                new Browser.NewContextOptions()
                                        .setAcceptDownloads(false)
                                        .setServiceWorkers(
                                                com.microsoft.playwright.options.ServiceWorkerPolicy
                                                        .BLOCK))) {
            context.setDefaultTimeout(profile.timeoutSeconds() * 1000.0);
            context.route(
                    "**/*",
                    route -> {
                        try {
                            profile.checkUrl(route.request().url());
                            route.resume();
                        } catch (Failure e) {
                            route.abort();
                        }
                    });
            var page = context.newPage();
            for (var step : verification ? profile.verify() : profile.change()) {
                if (step.action() != Action.NAVIGATE && !page.url().equals("about:blank"))
                    profile.checkUrl(page.url());
                String value = TemplateValues.resolve(step.value(), rotation);
                switch (step.action()) {
                    case NAVIGATE -> page.navigate(step.target());
                    case FILL -> page.locator(step.target()).fill(value);
                    case CLICK -> page.locator(step.target()).click();
                    case WAIT_VISIBLE -> page.locator(step.target()).waitFor();
                    case ASSERT_TEXT -> assertThat(page.locator(step.target())).hasText(value);
                    case PAUSE -> {
                        // Playwright Inspector resumes the exact same automation; no credentials
                        // are printed.
                        System.out.println(
                                "Manueller Schritt: "
                                        + step.target()
                                        + " — danach im Playwright Inspector fortsetzen.");
                        page.pause();
                    }
                }
            }
        } catch (Failure e) {
            throw e;
        } catch (Throwable e) {
            if (e instanceof VirtualMachineError fatal) throw fatal;
            // Playwright exceptions contain filled values and DOM snippets, so never forward them.
            throw new Failure(
                    verification
                            ? "Website-Verifikation fehlgeschlagen; Tresor bleibt unverändert."
                            : "Website-Ablauf abgebrochen; mögliche Teiländerungen mit recover"
                                    + " prüfen.");
        }
    }
}
