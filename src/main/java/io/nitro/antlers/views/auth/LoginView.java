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
import com.vaadin.flow.server.auth.AnonymousAllowed;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

@PageTitle("Вход/Регистрация")
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

        // Грузим модульный firebase-скрипт, лежащий в META-INF/resources/firebase-app.js
        UI.getCurrent().getPage().addJsModule("/firebase-app.js");

        H1 title = new H1("Вход/Регистрация");

        nick.setPlaceholder("Например: Орлан");
        nick.setMaxLength(24);
        nick.setHelperText("3–24 символа, буквы/цифры/пробел/._-");
        nick.setWidth("320px");

        pin.setWidth("320px");
        pin.setRevealButtonVisible(true);

        noPin.addValueChangeListener(e -> {
            boolean disabled = e.getValue();
            pin.setEnabled(!disabled);
            if (disabled) pin.clear();
        });

        registerBtn.addClickListener(e -> doRegister());
        loginBtn.addClickListener(e -> doLogin());

        // ENTER = Войти (shortcut вместо addKeyPressListener)
        loginBtn.addClickShortcut(Key.ENTER);

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.add(nick, pin, noPin);

        add(title, form, registerBtn, loginBtn);
    }

    private void doRegister() {
        String n = nick.getValue() == null ? "" : nick.getValue().trim();
        String p = pin.isEnabled() ? (pin.getValue() == null ? "" : pin.getValue().trim()) : "";

        if (n.length() < 3) {
            Notification.show("Введите ник (минимум 3 символа)");
            return;
        }

        UI.getCurrent().getPage().executeJs("""
            (async () => {
              try {
                if (!window._fb || !window._fb.fbRegister) {
                  throw new Error("Firebase: не найден _fb.fbRegister. Проверь /firebase-app.js");
                }
                const res = await window._fb.fbRegister({nick: $0, pin: $1, noPin: $2});
                return { ok: true, res };
              } catch (e) {
                return { ok: false, err: String(e && e.message || e) };
              }
            })()
            """, n, p, noPin.getValue()
        ).then((JsonValue json) -> {
            JsonObject obj = (JsonObject) json;
            boolean ok = obj.getBoolean("ok");
            if (!ok) {
                String err = obj.getString("err");
                Notification.show("Ошибка регистрации: " + err, 5000, Notification.Position.MIDDLE);
                return;
            }
            Notification.show("Успешная регистрация", 2000, Notification.Position.MIDDLE);
            UI.getCurrent().navigate("create");
        });
    }

    private void doLogin() {
        String n = nick.getValue() == null ? "" : nick.getValue().trim();
        String p = pin.isEnabled() ? (pin.getValue() == null ? "" : pin.getValue().trim()) : "";

        if (n.length() < 3) {
            Notification.show("Введите ник (минимум 3 символа)");
            return;
        }

        UI.getCurrent().getPage().executeJs("""
            (async () => {
              try {
                if (!window._fb || !window._fb.fbLogin) {
                  throw new Error("Firebase: не найден _fb.fbLogin. Проверь /firebase-app.js");
                }
                const res = await window._fb.fbLogin({nick: $0, pin: $1, noPin: $2});
                return { ok: true, res };
              } catch (e) {
                return { ok: false, err: String(e && e.message || e) };
              }
            })()
            """, n, p, noPin.getValue()
        ).then((JsonValue json) -> {
            JsonObject obj = (JsonObject) json;
            boolean ok = obj.getBoolean("ok");
            if (!ok) {
                String err = obj.getString("err");
                Notification.show("Ошибка входа: " + err, 5000, Notification.Position.MIDDLE);
                return;
            }
            Notification.show("Добро пожаловать!", 1500, Notification.Position.MIDDLE);
            UI.getCurrent().navigate("create");
        });
    }
}
