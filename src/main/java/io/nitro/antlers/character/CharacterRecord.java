package io.nitro.antlers.character;

import java.time.Instant;

public class CharacterRecord {
    public String id;                   // UUID
    public String nickname_display;     // как ввёл игрок (кириллица)
    public String nickname_normalized;  // нормализовано (для поиска/уникальности)
    public String race;                 // enum/строка
    public String element;              // enum/строка или null
    public Boolean has_pin;             // true/false
    public Instant createdAt;
    public Instant lastLoginAt;

    public CharacterRecord() {}
}
