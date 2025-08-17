// functions/index.js
const { onCall } = require("firebase-functions/v2/https");
const { initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");

initializeApp();

// Создать/найти юзера по нику и выдать custom token
exports.registerUser = onCall(async (req) => {
  const { nick } = req.data || {};
  if (!nick || typeof nick !== "string") {
    throw new Error("nick is required");
  }

  try {
    await getAuth().getUser(nick);
  } catch (e) {
    // если пользователя нет — создаём
    await getAuth().createUser({ uid: nick, displayName: nick });
  }

  const token = await getAuth().createCustomToken(nick);
  return { token };
});

// Выдать custom token по нику
exports.loginUser = onCall(async (req) => {
  const { nick } = req.data || {};
  if (!nick || typeof nick !== "string") {
    throw new Error("nick is required");
  }
  const token = await getAuth().createCustomToken(nick);
  return { token };
});

// Заглушка создания персонажа
exports.createCharacter = onCall(async (req) => {
  // можешь валидировать req.auth и req.data при желании
  const id = "char-" + Date.now();
  return { id };
});
