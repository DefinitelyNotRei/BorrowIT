const path = require('path');
const express = require('express');
const helmet = require('helmet');
const cookieParser = require('cookie-parser');
const session = require('express-session');
const rateLimit = require('express-rate-limit');
const cors = require('cors');
const dotenv = require('dotenv');
const { sanitizeMiddleware } = require('./src/sanitizer');
const userRoutes = require('./src/userRoutes');
const adminRoutes = require('./src/adminRoutes');

dotenv.config();

const PORT = process.env.PORT || 3000;
const staticRoot = path.join(__dirname, 'public');

const app = express();
app.set('allowAdminLogin', true);

// Enhanced Content Security Policy
app.use(helmet.contentSecurityPolicy({
  directives: {
    defaultSrc: ["'self'"],
    scriptSrc: ["'self'", "'unsafe-inline'"],
    styleSrc: ["'self'", "'unsafe-inline'"],
    imgSrc: ["'self'", "data:", "https:"],
    connectSrc: ["'self'"],
    fontSrc: ["'self'"],
    objectSrc: ["'none'"],
    mediaSrc: ["'self'"],
    frameSrc: ["'none'"],
  },
}));

app.use(cors({
  origin: process.env.CORS_ORIGIN || '*',
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'DELETE'],
  allowedHeaders: ['Content-Type', 'Authorization']
}));

app.use(express.json());
app.use(express.urlencoded({ extended: false }));
app.use(cookieParser());
app.use(sanitizeMiddleware);

// Rate limiting configuration
const limiter = rateLimit({
  windowMs: parseInt(process.env.RATE_LIMIT_WINDOW_MS) || 15 * 60 * 1000, // 15 minutes
  max: parseInt(process.env.RATE_LIMIT_MAX_REQUESTS) || 100,
  message: { message: 'Too many requests from this IP, please try again later.' },
  standardHeaders: true,
  legacyHeaders: false,
});

const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: parseInt(process.env.AUTH_RATE_LIMIT_MAX_REQUESTS) || 5,
  message: { message: 'Too many login attempts from this IP, please try again later.' },
  standardHeaders: true,
  legacyHeaders: false,
});

app.use('/api/login', authLimiter);
app.use('/api', limiter);

app.use(session({
  name: 'borrowit_sid',
  secret: process.env.SESSION_SECRET || 'borrowit-default-secret',
  resave: false,
  saveUninitialized: false,
  cookie: {
    httpOnly: true,
    sameSite: 'lax',
    secure: process.env.NODE_ENV === 'production',
    maxAge: parseInt(process.env.SESSION_MAX_AGE) || 30 * 60 * 1000 // 30 minutes
  }
}));

app.use('/api/admin', adminRoutes);
app.use('/api', userRoutes);
app.use(express.static(staticRoot));

app.get(['/login.html', '/admin-login.html', '/admin.html'], (req, res) => {
  return res.sendFile(path.join(staticRoot, path.basename(req.path)));
});

app.get('/register.html', (req, res) => {
  return res.redirect(301, '/login.html');
});

app.get('*', (req, res) => {
  if (path.extname(req.path)) {
    return res.status(404).send('Not Found');
  }
  return res.sendFile(path.join(staticRoot, 'index.html'));
});

app.listen(PORT, () => {
  console.log(`BorrowIT portal listening on http://localhost:${PORT}`);
});
