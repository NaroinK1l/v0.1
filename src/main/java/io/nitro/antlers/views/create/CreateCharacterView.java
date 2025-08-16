package io.nitro.antlers.views.create;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import io.nitro.antlers.base.ui.view.MainLayout;

import java.security.SecureRandom;
import java.util.Random;

@Route(value = "create", layout = MainLayout.class)
@AnonymousAllowed
public class CreateCharacterView extends VerticalLayout {

    private final Random rng = new SecureRandom();

    private final Button rollBtn = new Button("Бросить 1d8");
    private final Span resultSpan = new Span("— элемент ещё не определён —");
    private final ComboBox<Element> chooseAnyElement = new ComboBox<>("Выберите элемент");
    private Element rolledElement = null;

    private final TextField nickname = new TextField("Никнейм");
    private final ComboBox<Race> race = new ComboBox<>("Раса");
    private final Div raceInfo = new Div();

    private final Checkbox wantsPin = new Checkbox("Установить PIN (по желанию)");
    private final PasswordField pin1 = new PasswordField("PIN");
    private final PasswordField pin2 = new PasswordField("Повторите PIN");

    private final Button createBtn = new Button("Создать персонажа");

    public CreateCharacterView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new H1("Создание персонажа"));

        // --- Блок броска элемента ---
        chooseAnyElement.setItems(Element.valuesWithoutSpecial());
        chooseAnyElement.setItemLabelGenerator(Element::title);
        chooseAnyElement.setVisible(false);
        chooseAnyElement.setRequired(true);

        rollBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        rollBtn.addClickListener(e -> doRollOnce());

        HorizontalLayout rollLine = new HorizontalLayout(rollBtn, resultSpan);
        rollLine.setAlignItems(Alignment.CENTER);
        rollLine.setWidthFull();
        add(rollLine, chooseAnyElement, new Paragraph(
            "Начальный элемент определяется броском 1d8. " +
            "8 — можно выбрать любой элемент, 1 — без элемента (можно выбрать позже)."
        ));

        // --- Поля ник/раса ---
        nickname.setPlaceholder("Например: Орлан");
        nickname.setMaxLength(24);
        nickname.setPattern("^[\\p{L}\\p{N}_ .-]{3,24}$"); // поддержка кириллицы и латиницы
        nickname.setHelperText("3–24 символа, буквы/цифры/пробел/._-");

        race.setItems(Race.values());
        race.setItemLabelGenerator(Race::title);
        race.addValueChangeListener(ev -> raceInfo.setText(ev.getValue() != null ? ev.getValue().desc() : ""));

        raceInfo.getStyle().set("font-size", "var(--lumo-font-size-s)");
        raceInfo.getStyle().set("color", "var(--lumo-secondary-text-color)");

        FormLayout form = new FormLayout();
        form.add(nickname, race);
        form.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("700px", 2)
        );
        add(form, raceInfo);

        // --- PIN (необязательный) ---
        pin1.setAllowedCharPattern("[0-9]");
        pin1.setMaxLength(8);
        pin1.setMinLength(4);
        pin1.setHelperText("Только цифры, 4–8 знаков");

        pin2.setAllowedCharPattern("[0-9]");
        pin2.setMaxLength(8);
        pin2.setMinLength(4);

        pin1.setVisible(false);
        pin2.setVisible(false);

        wantsPin.addValueChangeListener(ev -> {
            boolean v = Boolean.TRUE.equals(ev.getValue());
            pin1.setVisible(v);
            pin2.setVisible(v);
        });

        add(wantsPin, new HorizontalLayout(pin1, pin2));

        // --- Кнопка создания ---
        createBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        createBtn.addClickListener(e -> onCreate());
        add(createBtn);

        // Небольшой отступ
        getStyle().set("max-width", "900px");
        setAlignSelf(Alignment.START, rollLine, chooseAnyElement, form, raceInfo, wantsPin, createBtn);
    }

    // Восстанавливаем «один бросок» из сессии, если уже бросали
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        Boolean locked = (Boolean) VaadinSession.getCurrent().getAttribute("create.roll.lock");
        Element stored = (Element) VaadinSession.getCurrent().getAttribute("create.roll.element");
        if (Boolean.TRUE.equals(locked)) {
            rollBtn.setEnabled(false);
            if (stored != null) {
                rolledElement = stored;
                applyElementToUI(stored, /*rolledNumber*/ null);
            }
        }
    }

    private void doRollOnce() {
        // Бросок только один раз за сессию
        if (Boolean.TRUE.equals(VaadinSession.getCurrent().getAttribute("create.roll.lock"))) {
            Notification.show("Бросок уже сделан в этой сессии.");
            rollBtn.setEnabled(false);
            return;
        }

        int roll = rng.nextInt(8) + 1; // 1..8
        Element el = switch (roll) {
            case 1 -> Element.NONE;
            case 2 -> Element.FIRE;
            case 3 -> Element.ICE;
            case 4 -> Element.WATER;
            case 5 -> Element.WIND;
            case 6 -> Element.EARTH;
            case 7 -> Element.LIGHTNING;
            case 8 -> Element.ANY;
            default -> throw new IllegalStateException("bad roll");
        };

        rolledElement = el;
        VaadinSession.getCurrent().setAttribute("create.roll.lock", true);
        VaadinSession.getCurrent().setAttribute("create.roll.element", el);

        rollBtn.setEnabled(false);
        applyElementToUI(el, roll);
    }

    private void applyElementToUI(Element el, Integer rolledNumberOrNull) {
        if (el == Element.ANY) {
            resultSpan.setText((rolledNumberOrNull != null ? rolledNumberOrNull + " — " : "") +
                               "Вы можете выбрать любой элемент.");
            chooseAnyElement.clear();
            chooseAnyElement.setVisible(true);
            chooseAnyElement.focus();
        } else if (el == Element.NONE) {
            resultSpan.setText((rolledNumberOrNull != null ? rolledNumberOrNull + " — " : "") +
                               "Без элемента (можно выбрать позже).");
            chooseAnyElement.setVisible(false);
        } else {
            resultSpan.setText((rolledNumberOrNull != null ? rolledNumberOrNull + " — " : "") +
                               el.title());
            chooseAnyElement.setVisible(false);
        }
    }

    private void onCreate() {
        // Валидация ника
        String nick = nickname.getValue() != null ? nickname.getValue().trim() : "";
        if (nick.length() < 3 || nick.length() > 24 || !nick.matches("^[\\p{L}\\p{N}_ .-]{3,24}$")) {
            Notification.show("Введите корректный ник (3–24 символа, буквы/цифры/пробел/._-)");
            nickname.focus();
            return;
        }

        // Выбор расы обязателен
        Race r = race.getValue();
        if (r == null) {
            Notification.show("Выберите расу.");
            race.focus();
            return;
        }

        // Элемент: если выпало ANY — обязателен выбор; если NONE — можно оставить null
        Element finalElement = rolledElement;
        if (rolledElement == null) {
            Notification.show("Сначала бросьте 1d8 для элемента.");
            return;
        }
        if (rolledElement == Element.ANY) {
            finalElement = chooseAnyElement.getValue();
            if (finalElement == null) {
                Notification.show("Выберите элемент.");
                chooseAnyElement.focus();
                return;
            }
        }
        if (rolledElement == Element.NONE) {
            finalElement = null; // разрешаем «позже»
        }

        // PIN по желанию
        String pin = null;
        if (Boolean.TRUE.equals(wantsPin.getValue())) {
            String p1 = pin1.getValue();
            String p2 = pin2.getValue();
            if (p1 == null || p2 == null || !p1.matches("\\d{4,8}") || !p1.equals(p2)) {
                Notification.show("PIN должен быть 4–8 цифр и совпадать в обоих полях.");
                pin1.focus();
                return;
            }
            pin = p1;
        }

        // TODO: сохранить в БД / Firebase. Пока просто покажем сводку.
        String msg = "Создан персонаж: ник «%s», раса «%s», элемент «%s»%s"
                .formatted(nick, r.title(), finalElement != null ? finalElement.title() : "— позже —",
                        pin != null ? ", PIN установлен" : "");
        Notification.show(msg, 4000, Notification.Position.TOP_CENTER);

        // пример: перейти на главную/профиль
        // UI.getCurrent().navigate(""); // раскомментируй, если нужно
    }

    // --- Вспомогательные перечисления ---

    public enum Element {
        NONE("Без элемента"),
        FIRE("Огонь"),
        ICE("Лёд"),
        WATER("Вода"),
        WIND("Ветер"),
        EARTH("Земля"),
        LIGHTNING("Молния"),
        ANY("Любой элемент");

        private final String title;
        Element(String title) { this.title = title; }
        public String title() { return title; }

        // для выпавшего "8 — любой" показываем список без служебных значений
        public static Element[] valuesWithoutSpecial() {
            return new Element[]{FIRE, ICE, WATER, WIND, EARTH, LIGHTNING};
        }
    }

    public enum Race {
        TAMI("Тами", "+10% к реакции и уклонению"),
        NATIS("Натис", "+10% к защите и +5 к физическому урону"),
        OGVIR("Огвир", "+2 Э (энергии) и +7 HP"),
        ANER("Анер", "+3 к инициативе и +7 HP"),
        HUMAN("Человек", "+1 дополнительная черта при создании"),
        ELCHAR("Эльчар", "+10% к маг. урону и +3 ЭМ");

        private final String title;
        private final String desc;
        Race(String title, String desc) { this.title = title; this.desc = desc; }
        public String title() { return title; }
        public String desc() { return desc; }

        @Override public String toString() { return title; }
    }
}
