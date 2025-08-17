// package io.nitro.antlers.views.auth;

// import com.vaadin.flow.component.Component;
// import com.vaadin.flow.router.BeforeEnterEvent;
// import com.vaadin.flow.router.BeforeEnterObserver;
// import com.vaadin.flow.router.Route;
// import com.vaadin.flow.server.auth.AnonymousAllowed;

// @Route("") // корень сайта
// @AnonymousAllowed
// public class RootRedirectView extends Component implements BeforeEnterObserver {
//     @Override public void beforeEnter(BeforeEnterEvent e) {
//         e.forwardTo(LoginView.class); // => /auth
//     }
// }
