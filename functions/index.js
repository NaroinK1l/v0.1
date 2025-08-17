const { onCall } = require("firebase-functions/v2/https");
const { initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");

initializeApp();

// registerUser: создаём/находим пользователя по нику и отдаём custom token
exports.registerUser = onCall(async (req) => {
  const { nick } = req.data || {};
  if (!nick || typeof nick !== "string") throw new Error("nick is required");

  let user;
  try {
    user = await getAuth().getUser(nick);
  } catch (e) {
    user = await getAuth().createUser({ uid: nick, displayName: nick });
  }
  const token = await getAuth().createCustomToken(user.uid);
  return { token };
});

// loginUser: выдаём custom token по нику (пин можно прикрутить позже)
exports.loginUser = onCall(async (req) => {
  const { nick } = req.data || {};
  if (!nick || typeof nick !== "string") throw new Error("nick is required");
  const token = await getAuth().createCustomToken(nick);
  return { token };
});

// createCharacter: заглушка, вернёт id
exports.createCharacter = onCall(async () => {
  const id = "char-" + Date.now();
  return { id };
});
