package io.nitro.antlers.views.profile;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import io.nitro.antlers.base.ui.view.MainLayout;

@PageTitle("Профиль")
@Route(value = "profile", layout = MainLayout.class)
@AnonymousAllowed
public class ProfileView extends VerticalLayout {
  public ProfileView() {
    setWidthFull();
    setMaxWidth("900px");
    String uid     = (String) VaadinSession.getCurrent().getAttribute("profile.uid");
    String name    = (String) VaadinSession.getCurrent().getAttribute("profile.name");
    String race    = (String) VaadinSession.getCurrent().getAttribute("profile.race");
    String element = (String) VaadinSession.getCurrent().getAttribute("profile.element");

    add(new H1("Профиль"),
        new Paragraph("Ник: " + (uid != null ? uid : "—")),
        new Paragraph("Персонаж: " + (name != null ? name : "—")),
        new Paragraph("Раса: " + (race != null ? race : "—")),
        new Paragraph("Элемент: " + (element != null ? element : "—"))
    );
  }
}
