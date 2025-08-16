"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.loginUser = exports.registerUser = void 0;
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const bcrypt = require("bcryptjs");
admin.initializeApp();
const db = admin.firestore();
const NICK_RE = /^[А-Яа-яЁёІіЇїЄєҐґ]{2,30}$/;
const PIN_RE = /^\d{4,6}$/;
const sanitize = (s) => (s || "").trim();
exports.registerUser = functions.https.onCall(async (data) => {
    const nick = sanitize(String(data?.nick ?? ""));
    const noPin = Boolean(data?.noPin ?? false);
    const pin = String(data?.pin ?? "");
    if (!NICK_RE.test(nick))
        throw new functions.https.HttpsError("invalid-argument", "Некорректный ник (только кириллица, 2–30).");
    if (!noPin && !PIN_RE.test(pin))
        throw new functions.https.HttpsError("invalid-argument", "Некорректный PIN (4–6 цифр) или отметьте «без PIN».");
    const userRef = db.collection("users").doc(nick);
    if ((await userRef.get()).exists)
        throw new functions.https.HttpsError("already-exists", "Ник занят.");
    const hasPin = !noPin;
    const pinHash = hasPin ? await bcrypt.hash(pin, 10) : null;
    await userRef.set({
        nick, hasPin, pinHash,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    try {
        await admin.auth().createUser({ uid: nick, displayName: nick });
    }
    catch (e) {
        await userRef.delete().catch(() => { });
        throw new functions.https.HttpsError("internal", "Ошибка создания пользователя в Auth: " + (e?.message || e));
    }
    const token = await admin.auth().createCustomToken(nick);
    return { token, nick, hasPin };
});
exports.loginUser = functions.https.onCall(async (data) => {
    const nick = sanitize(String(data?.nick ?? ""));
    const pin = String(data?.pin ?? "");
    if (!NICK_RE.test(nick))
        throw new functions.https.HttpsError("invalid-argument", "Некорректный ник.");
    const snap = await db.collection("users").doc(nick).get();
    if (!snap.exists)
        throw new functions.https.HttpsError("not-found", "Пользователь не найден.");
    const u = snap.data();
    if (u.hasPin) {
        if (!PIN_RE.test(pin))
            throw new functions.https.HttpsError("invalid-argument", "Нужен корректный PIN.");
        const ok = await bcrypt.compare(pin, u.pinHash || "");
        if (!ok)
            throw new functions.https.HttpsError("permission-denied", "Неверный PIN.");
    }
    try {
        await admin.auth().getUser(nick);
    }
    catch {
        await admin.auth().createUser({ uid: nick, displayName: nick });
    }
    const token = await admin.auth().createCustomToken(nick);
    return { token, nick, hasPin: !!u.hasPin };
});
