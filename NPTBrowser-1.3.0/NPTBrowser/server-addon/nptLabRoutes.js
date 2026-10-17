const express = require("express");

const router = express.Router();

router.get("/time", (req, res) => {
  res.status(200).json({ time: new Date().toLocaleTimeString("vi-VN") });
});

router.post("/message", (req, res) => {
  const message = req.body?.message;
  if (typeof message !== "string" || message.trim().length === 0) {
    return res.status(400).json({ error: "message must be a non-empty string" });
  }

  return res.status(200).json({
    message: message.trim(),
    length: message.trim().length,
    receivedAt: new Date().toISOString(),
  });
});

router.get("/students", (req, res) => {
  res.status(200).json([
    { id: 1, name: "Nguyen Van A" },
    { id: 2, name: "Tran Van B" },
  ]);
});

for (const path of ["/time", "/message", "/students"]) {
  router.all(path, (req, res) => {
    res.status(405).json({ error: "Method Not Allowed" });
  });
}

function malformedJsonHandler(err, req, res, next) {
  if (err instanceof SyntaxError && err.status === 400 && "body" in err) {
    return res.status(400).json({ error: "Invalid JSON body" });
  }
  return next(err);
}

module.exports = { router, malformedJsonHandler };
