/**
 * Firebase Functions v2 (JS)
 * Логика регистрации, логина и выдачи краткой сводки профиля.
 * Айди пользователя генерируется в формате 21052019NNNN по счётчику.
 */

const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { logger } = require("firebase-functions");
const admin = require("firebase-admin");

// Инициализация Admin SDK (разрешит работать с Firestore/Auth из функций)
try {
  admin.app();
} catch (e) {
  admin.initializeApp();
}

const db = admin.firestore();
const auth = admin.auth();

/** Имя коллекции с профилями */
const USERS = "users";
/** Документ со счётчиком регистраций */
const COUNTER_DOC = "meta/counters";     // коллекция meta, документ counters
const COUNTER_FIELD = "usersNext";        // поле со следующим номером
/** Префикс айди */
const ID_PREFIX = "21052019";
/** Сколько цифр в порядковом номере */
const SEQ_WIDTH = 4;

/** Простейшее хэширование PIN (sha256). Можно заменить на bcrypt, если нужно. */
const crypto = require("crypto");
const hashPin = (pin) => crypto.createHash("sha256").update(String(pin)).digest("hex");

/** Форматируем номер в "21052019NNNN" */
function buildUserId(seq) {
  const tail = String(seq).padStart(SEQ_WIDTH, "0");
  return `${ID_PREFIX}${tail}`;
}

/**
 * Получить следующий порядковый номер регистрации атомарно.
 * Создаёт meta/counters при первом запуске.
 */
async function allocateNextSequence() {
  const ref = db.doc(COUNTER_DOC);
  let newSeq;

  await db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    let data = snap.exists ? snap.data() : {};
    let current = Number.isInteger(data[COUNTER_FIELD]) ? data[COUNTER_FIELD] : 0;
    newSeq = current + 1;
    tx.set(ref, { [COUNTER_FIELD]: newSeq }, { merge: true });
  });

  return newSeq;
}

/**
 * REGISTER + CREATE CHARACTER
 * Вход: { nick (name), pin?, noPin?, race, element }
 * Результат: { token } — custom token c uid = "21052019NNNN"
 */
exports.registerAndCreateCharacter = onCall({ region: "us-central1" }, async (req) => {
  const { nick, pin, noPin, race, element } = req.data || {};
  const name = (nick || "").trim();

  if (!name) {
    throw new HttpsError("invalid-argument", "name is required");
  }

  // Проверяем уникальность ника по ПОЛЮ name
  const exists = await db.collection(USERS).where("name", "==", name).limit(1).get();
  if (!exists.empty) {
    throw new HttpsError("already-exists", "nickname already taken");
  }

  // Выделяем следующий номер и формируем id
  const seq = await allocateNextSequence();              // 1,2,3,...
  const uid = buildUserId(seq);                          // 210520190001 ...

  // Пишем профиль
  const profile = {
    uid,
    name,                      // ник для логина
    race: race || null,
    element: element || null,
    createdAt: Date.now(),
    pinHash: noPin ? null : (pin ? hashPin(pin) : null), // если noPin=true — хэш = null
  };

  await db.collection(USERS).doc(uid).set(profile, { merge: false });

  // Создаём (или обновляем) запись в Firebase Auth под тем же uid — чтобы выдать токен
  try {
    await auth.getUser(uid);
  } catch (e) {
    await auth.createUser({ uid });
  }

  // Возвращаем custom token
  const token = await auth.createCustomToken(uid);
  logger.info("registered", { uid, name });
  return { token };
});

/**
 * LOGIN без PIN
 * Вход: { nick }
 * Ищем по полю name, убеждаемся что pinHash == null.
 * Выдаём токен с uid документа.
 */
exports.loginUser = onCall({ region: "us-central1" }, async (req) => {
  const { nick } = req.data || {};
  const name = (nick || "").trim();
  if (!name) throw new HttpsError("invalid-argument", "name is required");

  const snap = await db.collection(USERS).where("name", "==", name).limit(1).get();
  if (snap.empty) throw new HttpsError("not-found", "user not found");
  const doc = snap.docs[0];
  const user = doc.data();

  if (user.pinHash) {
    throw new HttpsError("failed-precondition", "pin is set; login without pin not allowed");
  }

  const uid = user.uid || doc.id;

  try {
    await auth.getUser(uid);
  } catch (e) {
    await auth.createUser({ uid });
  }

  const token = await auth.createCustomToken(uid);
  return { token };
});

/**
 * LOGIN c PIN
 * Вход: { nick, pin }
 * Ищем по полю name, сравниваем PIN.
 * Выдаём токен с uid документа.
 */
exports.loginWithPin = onCall({ region: "us-central1" }, async (req) => {
  const { nick, pin } = req.data || {};
  const name = (nick || "").trim();
  const pinStr = String(pin || "");

  if (!name || !pinStr) {
    throw new HttpsError("invalid-argument", "name and pin are required");
  }

  const snap = await db.collection(USERS).where("name", "==", name).limit(1).get();
  if (snap.empty) throw new HttpsError("not-found", "user not found");
  const doc = snap.docs[0];
  const user = doc.data();

  if (!user.pinHash) {
    throw new HttpsError("failed-precondition", "pin is not set for this user");
  }

  const ok = user.pinHash === hashPin(pinStr);
  if (!ok) throw new HttpsError("permission-denied", "bad pin");

  const uid = user.uid || doc.id;

  try {
    await auth.getUser(uid);
  } catch (e) {
    await auth.createUser({ uid });
  }

  const token = await auth.createCustomToken(uid);
  return { token };
});

/**
 * GET ACCOUNT SUMMARY
 * Читает профиль по uid текущего авторизованного пользователя.
 * Возвращает { uid, name, race, element }.
 */
exports.getAccountSummary = onCall({ region: "us-central1" }, async (req) => {
  const uid = req.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "not signed in");

  const doc = await db.collection(USERS).doc(uid).get();
  if (!doc.exists) throw new HttpsError("not-found", "profile not found");

  const u = doc.data() || {};
  const sum = {
    uid,
    name: u.name || null,
    race: u.race || null,
    element: u.element || null,
  };
  return sum;
});
