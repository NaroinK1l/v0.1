package io.nitro.antlers.views.auth;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;

@PageTitle("Вход")
@Route("auth")
@AnonymousAllowed
public class LoginView extends VerticalLayout {

    private final TextField nick = new TextField("Ник");
    private final PasswordField pin = new PasswordField("PIN");
    private final Checkbox noPin = new Checkbox("Регистрация/вход без PIN");

    private final Button registerBtn = new Button("Зарегистрироваться");
    private final Button loginBtn = new Button("Войти");

    public LoginView() {
        setSizeFull();
        setAlignItems(Alignment.START);
        setPadding(true);
        setSpacing(true);

        // Firebase helpers (META-INF/resources/firebase-app.js)
        UI.getCurrent().getPage().addJsModule("/firebase-app.js");

        H1 title = new H1("Вход/Регистрация");

        nick.setPlaceholder("Например: Орлан");
        nick.setMaxLength(24);
        nick.setHelperText("3–24 символа, буквы/цифры/пробел/._-");

        pin.setRevealButtonVisible(true);

        noPin.addValueChangeListener(e -> {
            boolean disabled = e.getValue();
            pin.setEnabled(!disabled);
            if (disabled) pin.clear();
        });

        registerBtn.addClickListener(e -> doRegister());
        loginBtn.addClickListener(e -> doLogin());

        // ENTER = Войти
        loginBtn.addClickShortcut(Key.ENTER);

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.add(nick, pin, noPin);

        add(title, form, registerBtn, loginBtn);
    }

    private void doRegister() {
        String n = nick.getValue() == null ? "" : nick.getValue().trim();
        String p = pin.isEnabled() ? (pin.getValue() == null ? "" : pin.getValue().trim()) : "";
        boolean withoutPin = Boolean.TRUE.equals(noPin.getValue());

        if (n.length() < 3) {
            Notification.show("Введите ник (минимум 3 символа)");
            return;
        }

        // Регистрируем аккаунт, создаём первого персонажа (name = nick, race=NATIS)
        UI.getCurrent().getPage().executeJs("""
            (async () => {
              try {
                if (!window._fb?.fbRegisterAndCreate || !window._fb?.fbGetAccountSummary) {
                  throw new Error('Firebase helpers not loaded');
                }
                await window._fb.fbRegisterAndCreate({ nick: $0, name: $0, pin: $1, noPin: $2, race: 'NATIS', element: null });
                const sum = await window._fb.fbGetAccountSummary();
                return { ok: true, sum };
              } catch (e) {
                return { ok: false, err: String(e && e.message || e) };
              }
            })()
        """, n, p, withoutPin
        ).then((JsonValue json) -> {
            JsonObject obj = (JsonObject) json;
            boolean ok = obj.getBoolean("ok");
            if (!ok) {
                String err = obj.getString("err");
                Notification.show("Ошибка регистрации: " + err, 5000, Notification.Position.MIDDLE);
                return;
            }
            persistSummaryToSession(obj.getObject("sum"));
            Notification.show("Успешная регистрация", 2000, Notification.Position.MIDDLE);
            UI.getCurrent().navigate("profile");
        });
    }

    private void doLogin() {
        String n = nick.getValue() == null ? "" : nick.getValue().trim();
        String p = pin.isEnabled() ? (pin.getValue() == null ? "" : pin.getValue().trim()) : "";
        boolean withoutPin = Boolean.TRUE.equals(noPin.getValue());

        if (n.length() < 3) {
            Notification.show("Введите ник (минимум 3 символа)");
            return;
        }

        UI.getCurrent().getPage().executeJs("""
            (async () => {
              try {
                if (!window._fb) throw new Error('Firebase helpers not loaded');
                if ($2) {
                  await window._fb.fbLogin({ nick: $0 });
                } else {
                  await window._fb.fbLoginWithPin({ nick: $0, pin: $1 });
                }
                const sum = await window._fb.fbGetAccountSummary();
                return { ok: true, sum };
              } catch (e) {
                return { ok: false, err: String(e && e.message || e) };
              }
            })()
        """, n, p, withoutPin
        ).then((JsonValue json) -> {
            JsonObject obj = (JsonObject) json;
            boolean ok = obj.getBoolean("ok");
            if (!ok) {
                String err = obj.getString("err");
                Notification.show("Ошибка входа: " + err, 5000, Notification.Position.MIDDLE);
                return;
            }
            persistSummaryToSession(obj.getObject("sum"));
            Notification.show("Добро пожаловать!", 1500, Notification.Position.MIDDLE);
            UI.getCurrent().navigate("profile");
        });
    }

    private void persistSummaryToSession(JsonObject sum) {
        VaadinSession s = VaadinSession.getCurrent();
        s.setAttribute("profile.uid", sum.getString("uid"));

        JsonValue chVal = sum.get("character");
        boolean hasCharacter = (chVal != null) && (chVal.getType() == JsonType.OBJECT);

        if (hasCharacter) {
            JsonObject ch = (JsonObject) chVal;
            s.setAttribute("profile.charId",  ch.getString("id"));
            s.setAttribute("profile.name",    ch.getString("name"));
            s.setAttribute("profile.race",    ch.getString("race"));

            JsonValue elVal = ch.get("element");
            String element = (elVal != null && elVal.getType() == JsonType.STRING) ? ch.getString("element") : null;
            s.setAttribute("profile.element", element);
        } else {
            s.setAttribute("profile.charId",  null);
            s.setAttribute("profile.name",    null);
            s.setAttribute("profile.race",    null);
            s.setAttribute("profile.element", null);
        }
    }
}
