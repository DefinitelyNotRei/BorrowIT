const crypto = require('crypto');

const ITERATIONS = 65536;
const KEY_BYTES = 32;
const SALT_BYTES = 16;
const DIGEST = 'sha256';

function hashPassword(password) {
  const salt = crypto.randomBytes(SALT_BYTES);
  const hash = crypto.pbkdf2Sync(password, salt, ITERATIONS, KEY_BYTES, DIGEST);
  return `${ITERATIONS}:${salt.toString('base64')}:${hash.toString('base64')}`;
}

function verifyPassword(password, storedHash) {
  if (!storedHash || typeof storedHash !== 'string') {
    return false;
  }

  const parts = storedHash.split(':');
  if (parts.length !== 3) {
    return false;
  }

  const iterations = parseInt(parts[0], 10);
  const salt = Buffer.from(parts[1], 'base64');
  const expectedHash = Buffer.from(parts[2], 'base64');
  const actualHash = crypto.pbkdf2Sync(password, salt, iterations, expectedHash.length, DIGEST);

  return crypto.timingSafeEqual(expectedHash, actualHash);
}

module.exports = {
  hashPassword,
  verifyPassword
};
