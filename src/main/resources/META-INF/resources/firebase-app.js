// firebase-app.js — модуль с обёртками для функций + таймауты

import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.4/firebase-app.js";
import {
  getAuth, onAuthStateChanged, signInWithCustomToken, signOut, connectAuthEmulator
} from "https://www.gstatic.com/firebasejs/10.12.4/firebase-auth.js";
import {
  getFunctions, httpsCallable, connectFunctionsEmulator
} from "https://www.gstatic.com/firebasejs/10.12.4/firebase-functions.js";

export const USE_EMULATORS = true;            // локально true; в проде false
export const PROJECT_ID    = "demo-rpvo1";

export const firebaseConfig = USE_EMULATORS ? {
  apiKey: "demo",
  authDomain: "demo.firebaseapp.com",
  projectId: PROJECT_ID,
  appId: "demo"
} : {
  apiKey: "REPLACE_ME",
  authDomain: "REPLACE_ME",
  projectId: PROJECT_ID,
  appId: "REPLACE_ME"
};

let app, auth, functions;
window._fbReady = (async () => {
  app = initializeApp(firebaseConfig);
  auth = getAuth(app);
  functions = getFunctions(app, "us-central1");
  if (USE_EMULATORS) {
    connectAuthEmulator(auth, "http://127.0.0.1:9099", { disableWarnings: true });
    connectFunctionsEmulator(functions, "127.0.0.1", 5001);
  }
  console.debug("[fb] SDK ready");
  return true;
})();

const TIMEOUT_MS = 12000;
function withTimeout(promise, label) {
  return new Promise((resolve, reject) => {
    const id = setTimeout(() => reject(new Error(`timeout: ${label} > ${TIMEOUT_MS}ms`)), TIMEOUT_MS);
    promise.then(v => { clearTimeout(id); resolve(v); })
           .catch(e => { clearTimeout(id); reject(e); });
  });
}
async function callFn(name, data) {
  console.debug("[fb] call", name, data);
  const callable = httpsCallable(functions, name);
  const res = await withTimeout(callable(data), name);
  console.debug("[fb] result", name, res?.data);
  return res.data;
}

export async function fbRegisterAndCreate({ nick, name, pin, noPin, race, element }) {
  await window._fbReady;
  const data = await callFn("registerAndCreateCharacter", { nick, name, pin, noPin, race, element });
  await withTimeout(signInWithCustomToken(auth, data.token), "signInAfterRegister");
  return data;
}
export async function fbLogin({ nick }) {
  await window._fbReady;
  const data = await callFn("loginUser", { nick });
  await withTimeout(signInWithCustomToken(auth, data.token), "signInLogin");
  return data;
}
export async function fbLoginWithPin({ nick, pin }) {
  await window._fbReady;
  const data = await callFn("loginWithPin", { nick, pin });
  await withTimeout(signInWithCustomToken(auth, data.token), "signInLoginPin");
  return data;
}
export async function fbGetAccountSummary() {
  await window._fbReady;
  return await callFn("getAccountSummary", {});
}
export async function fbLoginAndFetchSummary({ nick, pin, noPin }) {
  try {
    await window._fbReady;
    if (noPin) await fbLogin({ nick }); else await fbLoginWithPin({ nick, pin });
    const sum = await withTimeout(fbGetAccountSummary(), "getAccountSummary");
    return JSON.stringify({ ok: true, sum });
  } catch (e) {
    console.error("[fb] error:", e);
    return JSON.stringify({ ok: false, err: String((e && e.message) || e) });
  }
}

// НОВОЕ: выход из Firebase (используем в кнопке «Выйти»)
export async function fbSignOut() {
  try {
    await window._fbReady;
    await withTimeout(signOut(auth), "signOut");
  } catch (e) {
    console.error("[fb] signOut error:", e);
  }
  return true;
}

export function fbOnAuth(cb) { return onAuthStateChanged(auth, cb); }

window._fb = {
  fbRegisterAndCreate,
  fbLogin,
  fbLoginWithPin,
  fbGetAccountSummary,
  fbLoginAndFetchSummary,
  fbSignOut,               // ← добавили
  fbOnAuth
};
