// Account lockout and failed login attempt tracking
// In-memory storage for failed login attempts (for production, use Redis or database)

const failedAttempts = new Map();
const LOCKOUT_DURATION = 15 * 60 * 1000; // 15 minutes
const MAX_ATTEMPTS = 5;

function recordFailedAttempt(ip, username) {
  const key = `${ip}:${username}`;
  const now = Date.now();
  
  if (!failedAttempts.has(key)) {
    failedAttempts.set(key, { count: 1, lastAttempt: now });
  } else {
    const attempts = failedAttempts.get(key);
    
    // Reset if lockout period has passed
    if (now - attempts.lastAttempt > LOCKOUT_DURATION) {
      attempts.count = 1;
      attempts.lastAttempt = now;
    } else {
      attempts.count++;
      attempts.lastAttempt = now;
    }
    
    failedAttempts.set(key, attempts);
  }
  
  return failedAttempts.get(key);
}

function isLockedOut(ip, username) {
  const key = `${ip}:${username}`;
  const attempts = failedAttempts.get(key);
  
  if (!attempts) {
    return false;
  }
  
  const now = Date.now();
  const timeSinceLastAttempt = now - attempts.lastAttempt;
  
  // Reset if lockout period has passed
  if (timeSinceLastAttempt > LOCKOUT_DURATION) {
    failedAttempts.delete(key);
    return false;
  }
  
  // Check if locked out
  if (attempts.count >= MAX_ATTEMPTS) {
    return {
      locked: true,
      remainingTime: Math.ceil((LOCKOUT_DURATION - timeSinceLastAttempt) / 1000 / 60) // minutes
    };
  }
  
  return false;
}

function clearFailedAttempts(ip, username) {
  const key = `${ip}:${username}`;
  failedAttempts.delete(key);
}

function getRemainingAttempts(ip, username) {
  const key = `${ip}:${username}`;
  const attempts = failedAttempts.get(key);
  
  if (!attempts) {
    return MAX_ATTEMPTS;
  }
  
  const now = Date.now();
  const timeSinceLastAttempt = now - attempts.lastAttempt;
  
  // Reset if lockout period has passed
  if (timeSinceLastAttempt > LOCKOUT_DURATION) {
    failedAttempts.delete(key);
    return MAX_ATTEMPTS;
  }
  
  return MAX_ATTEMPTS - attempts.count;
}

// Cleanup old entries periodically (every hour)
setInterval(() => {
  const now = Date.now();
  for (const [key, attempts] of failedAttempts.entries()) {
    if (now - attempts.lastAttempt > LOCKOUT_DURATION) {
      failedAttempts.delete(key);
    }
  }
}, 60 * 60 * 1000);

module.exports = {
  recordFailedAttempt,
  isLockedOut,
  clearFailedAttempts,
  getRemainingAttempts,
  MAX_ATTEMPTS,
  LOCKOUT_DURATION
};
