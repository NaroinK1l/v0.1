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
import { getAuth, signInWithCustomToken, onAuthStateChanged, connectAuthEmulator } from "https://www.gstatic.com/firebasejs/10.12.0/firebase-auth.js";
import { getFunctions, httpsCallable, connectFunctionsEmulator } from "https://www.gstatic.com/firebasejs/10.12.0/firebase-functions.js";

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const functions = getFunctions(app);

if (USE_EMULATORS) {
  try { connectAuthEmulator(auth, "http://127.0.0.1:9099"); } catch (_) {}
  connectFunctionsEmulator(functions, "127.0.0.1", 5001);
}

// ========== HELPERS ==========
const NICK_RE = /^[А-Яа-яЁёІіЇїЄєҐґ]{2,30}$/;

export async function fbRegister({ nick, pin, noPin }) {
  if (!NICK_RE.test(nick)) throw new Error("Ник: только кириллица (2–30).");
  if (!noPin && !/^\d{4,6}$/.test(pin)) throw new Error("PIN: 4–6 цифр или отметьте «без PIN».");

  const call = httpsCallable(functions, "registerUser");
  const res = await call({ nick, pin, noPin });
  await signInWithCustomToken(auth, res.data.token);
  return res.data;
}

export async function fbLogin({ nick, pin }) {
  if (!NICK_RE.test(nick)) throw new Error("Ник: только кириллица (2–30).");
  const call = httpsCallable(functions, "loginUser");
  const res = await call({ nick, pin });
  await signInWithCustomToken(auth, res.data.token);
  return res.data;
}

export function fbOnAuth(cb) {
  return onAuthStateChanged(auth, cb);
}

// Делаем функции доступными глобально для вызова из Vaadin Java:
window._fb = { fbLogin, fbRegister, fbOnAuth };
