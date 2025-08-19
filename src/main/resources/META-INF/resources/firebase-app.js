// firebase-app.js — общий модуль для LoginView и create.html

// ========== НАСТРОЙКА ==========
export const USE_EMULATORS = true;         // локально true; в проде поставишь false
export const PROJECT_ID = "demo-rpvo1";    // в проде — реальный projectId

export const firebaseConfig = USE_EMULATORS ? {
  apiKey: "demo", authDomain: "demo.firebaseapp.com",
  projectId: PROJECT_ID, appId: "demo"
} : {
  // === ВСТАВЬ РЕАЛЬНЫЕ КЛЮЧИ ДЛЯ ПРОД ===
  apiKey: "…",
  authDomain: "…",
  projectId: "…",
  appId: "…"
};

// ========== SDK ==========
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.0/firebase-app.js";
import {
  getAuth, signInWithCustomToken, onAuthStateChanged, connectAuthEmulator
} from "https://www.gstatic.com/firebasejs/10.12.0/firebase-auth.js";
import {
  getFunctions, httpsCallable, connectFunctionsEmulator
} from "https://www.gstatic.com/firebasejs/10.12.0/firebase-functions.js";

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const functions = getFunctions(app, "us-central1");

if (USE_EMULATORS) {
  try {
    connectAuthEmulator(auth, "http://127.0.0.1:9099", { disableWarnings: true });
  } catch (_) {}
  connectFunctionsEmulator(functions, "127.0.0.1", 5001);
}

// ========== HELPERS ==========

// Регистрация + создание первого персонажа + моментальный вход
export async function fbRegisterAndCreate({ nick, name, pin, noPin, race, element }) {
  const call = httpsCallable(functions, "registerAndCreateCharacter");
  const { data } = await call({ nick, name, pin, noPin, race, element });
  await signInWithCustomToken(auth, data.token);
  return data; // { token, characterId }
}

// Вход БЕЗ PIN (для чекбокса «без PIN»)
export async function fbLogin({ nick }) {
  const call = httpsCallable(functions, "loginUser");
  const { data } = await call({ nick });
  await signInWithCustomToken(auth, data.token);
  return data;
}

// Вход по ник+PIN
export async function fbLoginWithPin({ nick, pin }) {
  const call = httpsCallable(functions, "loginWithPin");
  const { data } = await call({ nick, pin });
  await signInWithCustomToken(auth, data.token);
  return data;
}

// Создание ДОПОЛНИТЕЛЬНЫХ персонажей (после входа)
export async function createCharacter(payload) {
  const call = httpsCallable(functions, "createCharacter");
  const { data } = await call(payload);
  return data?.id || null;
}

// Сводка аккаунта (ник + последний персонаж)
export async function fbGetAccountSummary() {
  const call = httpsCallable(functions, "getAccountSummary");
  const { data } = await call();
  return data; // { uid, character: { id, name, race, element } | null }
}

export function fbOnAuth(cb) {
  return onAuthStateChanged(auth, cb);
}

// Глобально для Vaadin Java:
window._fb = {
  fbRegisterAndCreate, fbLogin, fbLoginWithPin, fbGetAccountSummary,
  fbOnAuth, createCharacter
};
