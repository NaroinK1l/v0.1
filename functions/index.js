// Cloud Functions for Firebase (v2, Node 20)
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { getFirestore } = require("firebase-admin/firestore");
const crypto = require("crypto");

initializeApp();
const db = getFirestore();

// ===== helpers =====
const NICK_RE = /^[А-Яа-яЁёІіЇїЄєҐґA-Za-z0-9 _.\-]{3,24}$/;
const PIN_RE  = /^\d{4,8}$/;
const sha256 = (s) => crypto.createHash("sha256").update(String(s)).digest("hex");

// 1) Регистрация + создание первого персонажа + вход
//    PIN ХРАНИМ НА УРОВНЕ АККАУНТА: users/{uid}.pinHash
exports.registerAndCreateCharacter = onCall(async (req) => {
  const { nick, name, pin, noPin, race, element } = req.data || {};

  if (!nick || !NICK_RE.test(nick)) {
    throw new HttpsError("invalid-argument", "Некорректный ник (3–24, буквы/цифры/пробел/._-).");
  }
  if (!name || String(name).trim().length < 2) {
    throw new HttpsError("invalid-argument", "Имя персонажа обязательно.");
  }
  if (!noPin && pin && !PIN_RE.test(pin)) {
    throw new HttpsError("invalid-argument", "PIN: 4–8 цифр или отметьте «без PIN».");
  }

  const uid = nick.trim();

  // создать пользователя при необходимости
  try { await getAuth().getUser(uid); }
  catch { await getAuth().createUser({ uid, displayName: uid }); }

  // сохранить PIN на аккаунт
  await db.collection("users").doc(uid).set({
    pinHash: noPin ? null : sha256(pin || ""),
    createdAt: Date.now()
  }, { merge: true });

  // создать первого персонажа
  const ref = await db.collection("users").doc(uid)
    .collection("characters").add({
      uid,
      name: String(name).trim(),
      race: race || "Натис",
      element: element || null,
      createdAt: Date.now()
    });

  const token = await getAuth().createCustomToken(uid);
  return { token, characterId: ref.id };
});

// 2) Вход по ник+PIN (сверяем users/{uid}.pinHash)
exports.loginWithPin = onCall(async (req) => {
  const { nick, pin } = req.data || {};
  if (!nick || !NICK_RE.test(nick)) throw new HttpsError("invalid-argument", "Некорректный ник.");
  if (!pin || !PIN_RE.test(pin))   throw new HttpsError("invalid-argument", "Некорректный PIN.");

  const uid = nick.trim();
  const userDoc = await db.collection("users").doc(uid).get();
  if (!userDoc.exists) throw new HttpsError("not-found", "Аккаунт не найден.");

  const pinHash = userDoc.get("pinHash") || null;
  if (!pinHash) throw new HttpsError("failed-precondition", "Для этого аккаунта PIN не установлен.");
  if (pinHash !== sha256(pin)) throw new HttpsError("permission-denied", "Неверный PIN.");

  const token = await getAuth().createCustomToken(uid);
  return { token };
});

// 3) Вход без PIN (для чекбокса «без PIN»)
exports.loginUser = onCall(async (req) => {
  const { nick } = req.data || {};
  if (!nick || !NICK_RE.test(nick)) throw new HttpsError("invalid-argument", "Некорректный ник.");
  const token = await getAuth().createCustomToken(nick.trim());
  return { token };
});

// 4) Создание дополнительных персонажей (нужен вход)
exports.createCharacter = onCall(async (req) => {
  if (!req.auth) throw new HttpsError("unauthenticated", "Требуется вход.");

  const { name, race, element } = req.data || {};
  if (!name || String(name).trim().length < 2) {
    throw new HttpsError("invalid-argument", "Имя персонажа обязательно.");
  }

  const ref = await db.collection("users").doc(req.auth.uid)
    .collection("characters").add({
      uid: req.auth.uid,
      name: String(name).trim(),
      race: race || "Натис",
      element: element || null,
      createdAt: Date.now()
    });

  return { id: ref.id };
});

// 5) Сводка аккаунта (ник + последний персонаж)
exports.getAccountSummary = onCall(async (req) => {
  if (!req.auth) throw new HttpsError("unauthenticated", "Требуется вход.");
  const uid = req.auth.uid;

  const snap = await getFirestore()
    .collection("users").doc(uid)
    .collection("characters")
    .orderBy("createdAt", "desc").limit(1)
    .get();

  let character = null;
  if (!snap.empty) {
    const d = snap.docs[0].data();
    character = {
      id: snap.docs[0].id,
      name: d.name || "",
      race: d.race || "",
      element: d.element || null
    };
  }
  return { uid, character };
});
