package io.nitro.antlers.views.profile;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.component.ClientCallable;

@PageTitle("Профиль")
@Route("profile")
@AnonymousAllowed
public class ProfileView extends VerticalLayout {

  public ProfileView() {
    setSizeFull();
    setPadding(true);
    setSpacing(true);

    // Верхняя панель с кнопкой "Выйти"
    Button logout = new Button("Выйти", e -> doLogout());
    logout.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
    HorizontalLayout topbar = new HorizontalLayout(logout);
    topbar.setWidthFull();
    topbar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
    add(topbar);

    add(new H1("Профиль"));

    // Читаем данные из сессии (их кладёт LoginView после успешного входа)
    VaadinSession s = VaadinSession.getCurrent();
    String uid     = (String) s.getAttribute("profile.uid");
    String name    = (String) s.getAttribute("profile.name");
    String race    = (String) s.getAttribute("profile.race");
    String element = (String) s.getAttribute("profile.element");

    if (uid == null && name == null) {
      // Нет сессии — отправляем на авторизацию
      UI.getCurrent().navigate("auth");
      return;
    }

    // ВАЖНО: теперь ник показываем как имя/ник персонажа (name), а не uid
    add(new Paragraph("Ник: " + (name != null ? name : "—")));
    // Строку «Персонаж» УБРАЛИ
    add(new Paragraph("Раса: " + (race != null ? race : "—")));
    add(new Paragraph("Элемент: " + (element != null ? element : "—")));
  }

  private void doLogout() {
    // Чистим сессию сразу (на всякий)
    VaadinSession s = VaadinSession.getCurrent();
    s.setAttribute("profile.uid", null);
    s.setAttribute("profile.name", null);
    s.setAttribute("profile.race", null);
    s.setAttribute("profile.element", null);

    // Попросим клиент выйти из Firebase и затем вернёмся на /auth
    getElement().executeJs(
        "try{ const el=this;"
      + "  import('/firebase-app.js').catch(()=>{}).then(()=>{"
      + "    return (window._fb && window._fb.fbSignOut) ? window._fb.fbSignOut() : null;"
      + "  }).finally(()=>{ el.$server.onLoggedOut(); });"
      + "}catch(e){ this.$server.onLoggedOut(); }"
    );
  }

  @ClientCallable
  public void onLoggedOut() {
    UI.getCurrent().navigate("auth");
  }
}
