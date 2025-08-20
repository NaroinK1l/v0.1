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
import com.vaadin.flow.component.ClientCallable;
import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;

@PageTitle("Вход")
@Route("auth")
@AnonymousAllowed
public class LoginView extends VerticalLayout {

  private final TextField nick = new TextField("Ник");
  private final PasswordField pin = new PasswordField("PIN (4–8 цифр)");
  private final Checkbox withoutPin = new Checkbox("Войти без PIN");
  private final Button createBtn = new Button("Создать персонажа");
  private final Button loginBtn  = new Button("Войти");

  public LoginView() {
    setSizeFull();
    setPadding(true);
    setSpacing(true);
    setDefaultHorizontalComponentAlignment(Alignment.CENTER);

    // Подгружаем модуль — ошибка загрузки видна в консоли
    getElement().executeJs(
      "console.log('[LoginView] import /firebase-app.js');" +
      "import('/firebase-app.js').catch(e=>console.error('firebase-app import error', e));"
    );

    H1 title = new H1("Вход");
    title.getStyle().set("margin", "var(--lumo-space-l) 0");

    nick.setClearButtonVisible(true);
    nick.setMaxLength(40);
    nick.setMinLength(1);

    pin.setClearButtonVisible(true);
    pin.setRevealButtonVisible(true);
    pin.setAllowedCharPattern("[0-9]");
    pin.setMaxLength(8);
    pin.setMinLength(4);

    withoutPin.addValueChangeListener(e -> pin.setEnabled(!Boolean.TRUE.equals(e.getValue())));

    createBtn.addClickListener(e -> UI.getCurrent().navigate("create"));
    loginBtn.addClickShortcut(Key.ENTER);
    loginBtn.addClickListener(e -> doLogin());

    FormLayout form = new FormLayout();
    form.add(nick, pin, withoutPin);
    form.setResponsiveSteps(
        new FormLayout.ResponsiveStep("0", 1),
        new FormLayout.ResponsiveStep("700px", 2)
    );

    add(title, form, loginBtn, createBtn);
    setMaxWidth("720px");
  }

  private void doLogin() {
    String n = nick.getValue() != null ? nick.getValue().trim() : "";
    String p = pin.getValue() != null ? pin.getValue().trim() : "";
    boolean noPin = Boolean.TRUE.equals(withoutPin.getValue());

    if (n.isEmpty()) {
      Notification.show("Введите ник.", 2500, Notification.Position.MIDDLE);
      return;
    }
    if (!noPin && (p.length() < 4 || p.length() > 8)) {
      Notification.show("Введите PIN (4–8 цифр) или включите «Войти без PIN».",
          3500, Notification.Position.MIDDLE);
      return;
    }

    Notification.show("Выполняю вход…", 1500, Notification.Position.MIDDLE);

    // КЛЮЧ: используем плейсхолдеры $0/$1/$2 и this === элемент.
    // Никаких стрелочных функций и аргументов-функций, только прямой вызов.
    getElement().executeJs(
      // Значения из Java будут доступны как $0 (nick), $1 (pin), $2 (noPin)
      "try {"
    + "  const el = this;"
    + "  const nick = $0;"
    + "  const pin  = $1;"
    + "  const noPin = $2;"
    + "  let responded = false;"
    + "  const respond = (payload) => {"
    + "    if (responded) return; responded = true;"
    + "    try { el.$server.onLoginResult(payload); } catch(e) { console.error('[LoginView] $server error', e); }"
    + "  };"
    + "  (async () => {"
    + "    try {"
    + "      if (window._fbReady) { await window._fbReady; console.log('[LoginView] _fbReady ok'); }"
    + "      if (!window._fb || typeof window._fb.fbLoginAndFetchSummary !== 'function') {"
    + "        return respond(JSON.stringify({ ok:false, err:'firebase-app.js не загружен' }));"
    + "      }"
    + "      console.log('[LoginView] call fbLoginAndFetchSummary', {nick, noPin});"
    + "      const json = await window._fb.fbLoginAndFetchSummary({ nick, pin, noPin });"
    + "      console.log('[LoginView] got:', json);"
    + "      respond(json);"
    + "    } catch (e) {"
    + "      const msg = (e && e.message) ? e.message : (''+e);"
    + "      console.error('[LoginView] error', msg);"
    + "      respond(JSON.stringify({ ok:false, err: msg }));"
    + "    }"
    + "  })();"
    + "  setTimeout(() => { if (!responded) respond(JSON.stringify({ ok:false, err:'timeout:12s' })); }, 12000);"
    + "} catch (e) {"
    + "  const msg = (e && e.message) ? e.message : (''+e);"
    + "  console.error('[LoginView] fatal', msg);"
    + "  this.$server.onLoginResult(JSON.stringify({ ok:false, err: msg }));"
    + "}",
      n, p, noPin
    );
  }

  /** Клиент всегда шлёт сюда JSON-строку (успех/ошибка). */
  @ClientCallable
  public void onLoginResult(String json) {
    if (json == null || json.isBlank()) {
      Notification.show("Не удалось получить ответ клиента (формат не распознан).",
          4000, Notification.Position.MIDDLE);
      return;
    }

    JsonObject obj = null;
    try {
      JsonValue parsed = Json.parse(json);
      if (parsed != null && parsed.getType() == JsonType.OBJECT) obj = (JsonObject) parsed;
    } catch (Exception ignore) {}

    if (obj == null) {
      Notification.show("Не удалось получить ответ клиента (формат не распознан).",
          4000, Notification.Position.MIDDLE);
      return;
    }

    boolean ok = obj.hasKey("ok") && obj.getBoolean("ok");
    if (!ok) {
      String err = obj.hasKey("err") ? obj.getString("err") : null;
      String msg;
      if (err != null && err.contains("not-found")) {
        msg = "Аккаунт не найден. Создайте персонажа.";
      } else if (err != null && (err.contains("permission-denied") || err.toLowerCase().contains("pin"))) {
        msg = "PIN не подошёл.";
      } else if (err != null && err.contains("failed-precondition")) {
        msg = "Для этого аккаунта установлен PIN. Войдите с PIN либо создайте нового персонажа.";
      } else if (err != null && err.toLowerCase().contains("timeout")) {
        msg = "Истекло время ожидания ответа. Проверь подключение к эмуляторам/сети и перезагрузи страницу.";
      } else {
        msg = "Не удалось войти: " + (err == null ? "неизвестная ошибка" : err);
      }
      Notification.show(msg, 5500, Notification.Position.MIDDLE);
      return;
    }

    JsonValue sumVal = obj.get("sum");
    if (sumVal == null || sumVal.getType() != JsonType.OBJECT) {
      Notification.show("Ошибка: некорректные данные профиля.",
          4000, Notification.Position.MIDDLE);
      return;
    }
    JsonObject sum = (JsonObject) sumVal;

    VaadinSession s = VaadinSession.getCurrent();
    s.setAttribute("profile.uid",  sum.hasKey("uid")  ? sum.getString("uid")  : null);
    s.setAttribute("profile.name", sum.hasKey("name") ? sum.getString("name") : null);
    s.setAttribute("profile.race", sum.hasKey("race") ? sum.getString("race") : null);
    String element = (sum.hasKey("element") && sum.get("element").getType()==JsonType.STRING)
        ? sum.getString("element") : null;
    s.setAttribute("profile.element", element);

    Notification.show("Добро пожаловать!", 1200, Notification.Position.MIDDLE);
    UI ui = UI.getCurrent();
    if (ui != null) ui.navigate("profile");
  }
}
