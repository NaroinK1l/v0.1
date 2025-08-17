package io.nitro.antlers.character;

import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;

import static io.nitro.antlers.util.UsernameUtil.*;

@Service
public class CharacterServiceFirebase {

    private Firestore db() {
        return FirestoreClient.getFirestore();
    }

    public static class RegisterResult {
        public final String characterId;
        public final String nicknameDisplay;
        public final String nicknameNormalized;
        public RegisterResult(String id, String disp, String norm) {
            this.characterId = id; this.nicknameDisplay = disp; this.nicknameNormalized = norm;
        }
    }

    public RegisterResult registerCharacter(String rawNickname,
                                            String raceOrNull,
                                            String elementOrNull,
                                            String pinOrNull)
            throws ExecutionException, InterruptedException {

        if (!isValidDisplayName(rawNickname)) {
            throw new IllegalArgumentException("Ім’я/Имя некоректне: тільки кирилиця та пробіли, мінімум 2 літери.");
        }

        final String nicknameDisplay = normalizeCyrillicOnly(rawNickname); // уже аккуратно
        final String nicknameNormalized = nicknameDisplay;                 // наши правила = уже нормализовано

        final boolean hasPin = pinOrNull != null && !pinOrNull.isBlank();
        final String pinHash = hasPin
                ? BCrypt.hashpw(pinOrNull.trim(), BCrypt.gensalt())
                : null;

        final String charId = java.util.UUID.randomUUID().toString();
        final Firestore db = db();

        db.runTransaction((Transaction.Function<Void>) tx -> {
            // 1) Бронируем имя
            final DocumentReference unameRef = db.collection("usernames").document(nicknameNormalized);
            final DocumentSnapshot unameSnap = tx.get(unameRef).get();
            if (unameSnap.exists()) {
                throw new RuntimeException("Таке ім’я вже використовується / Имя уже занято.");
            }
            final Map<String, Object> uname = new HashMap<>();
            uname.put("charId", charId);
            uname.put("createdAt", FieldValue.serverTimestamp());
            tx.set(unameRef, uname);

            // 2) Публичные данные персонажа
            final DocumentReference charRef = db.collection("characters").document(charId);
            final Map<String, Object> character = new HashMap<>();
            character.put("id", charId);
            character.put("nickname_display", nicknameDisplay);
            character.put("nickname_normalized", nicknameNormalized);
            character.put("race", raceOrNull);
            character.put("element", elementOrNull);
            character.put("has_pin", hasPin);
            character.put("createdAt", FieldValue.serverTimestamp());
            character.put("lastLoginAt", FieldValue.serverTimestamp());
            tx.set(charRef, character);

            // 3) Секреты (опционально)
            if (hasPin) {
                final DocumentReference secRef = db.collection("player_secrets").document(charId);
                final Map<String, Object> secret = new HashMap<>();
                secret.put("pin_hash", pinHash);
                secret.put("updatedAt", FieldValue.serverTimestamp());
                tx.set(secRef, secret);
            }
            return null;
        }).get();

        return new RegisterResult(charId, nicknameDisplay, nicknameNormalized);
    }

    public String loginByUsernamePin(String rawNickname, String pinOrNull)
            throws ExecutionException, InterruptedException {

        final String normalized = normalizeCyrillicOnly(rawNickname);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Вкажіть/Укажи коректне ім’я (лише кирилиця).");
        }

        final Firestore db = db();

        final DocumentSnapshot unameDoc = db.collection("usernames").document(normalized).get().get();
        if (!unameDoc.exists()) throw new IllegalArgumentException("Персонажа з таким ім’ям не знайдено / Персонаж не найден.");

        final String charId = unameDoc.getString("charId");
        if (charId == null || charId.isBlank()) throw new IllegalStateException("Пошкоджено реєстр імен / Повреждён реестр имён.");

        final DocumentReference charRef = db.collection("characters").document(charId);
        final DocumentSnapshot charSnap = charRef.get().get();
        if (!charSnap.exists()) throw new IllegalStateException("Персонаж відсутній / не найден.");

        final Boolean hasPin = charSnap.getBoolean("has_pin");
        if (Boolean.TRUE.equals(hasPin)) {
            if (pinOrNull == null || pinOrNull.isBlank()) {
                throw new IllegalArgumentException("Потрібен PIN / Требуется PIN.");
            }
            final DocumentSnapshot secSnap = db.collection("player_secrets").document(charId).get().get();
            final String storedHash = secSnap.getString("pin_hash");
            if (storedHash == null || !BCrypt.checkpw(pinOrNull.trim(), storedHash)) {
                throw new IllegalArgumentException("Невірний PIN / Неверный PIN.");
            }
        }

        charRef.update("lastLoginAt", FieldValue.serverTimestamp());
        return charId;
    }
}
