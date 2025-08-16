package io.nitro.antlers.views.login;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Log in")
@Route("login")
public class LoginView extends VerticalLayout {

    private final TextField nick = new TextField("Ник (кириллица)");
    private final PasswordField pin = new PasswordField("PIN (если есть)");

    public LoginView() {
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);

        nick.setWidth("360px");
        pin.setWidth("360px");

        Button loginBtn = new Button("Log in");
        loginBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button createBtn = new Button("Создать персонажа");
        createBtn.getStyle().set("margin-left", "12px");
        createBtn.addClickListener(e ->
            UI.getCurrent().getPage().open("/create.html") // открываем статический ресурс напрямую
        );

        HorizontalLayout actions = new HorizontalLayout(loginBtn, createBtn);

        VerticalLayout card = new VerticalLayout(new H1("Вход"), nick, pin, actions);
        card.setWidth("420px");
        add(card);

        // Подключаем наш общий JS-модуль Firebase
        UI.getCurrent().getPage().addJavaScript("/firebase-app.js");

        // Логика кнопки "Log in": вызов loginUser в Firebase
        loginBtn.addClickListener(e -> {
            String n = nick.getValue() == null ? "" : nick.getValue().trim();
            String p = pin.getValue() == null ? "" : pin.getValue().trim();

            UI.getCurrent().getPage().executeJs("""
                (async () => {
                  try {
                    const { fbLogin } = window._fb || {};
                    if (!fbLogin) throw new Error("Firebase не инициализирован");
                    await fbLogin({ nick: $0, pin: $1 });
                    window.location.href = "/";
                  } catch (e) {
                    window.alert(e.message || e);
                  }
                })();
            """, n, p);
        });
    }
}
