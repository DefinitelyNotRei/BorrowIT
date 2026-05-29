const path = require('path');
const express = require('express');
const helmet = require('helmet');
const cookieParser = require('cookie-parser');
const session = require('express-session');
const dotenv = require('dotenv');
const userRoutes = require('./src/userRoutes');
const adminRoutes = require('./src/adminRoutes');

dotenv.config();

const PORT = process.env.PORT || 3000;
const staticRoot = path.join(__dirname, 'public');

const app = express();
app.set('allowAdminLogin', true);
app.use(helmet());
app.use(express.json());
app.use(express.urlencoded({ extended: false }));
app.use(cookieParser());
app.use(session({
  name: 'borrowit_sid',
  secret: process.env.SESSION_SECRET || 'borrowit-default-secret',
  resave: false,
  saveUninitialized: false,
  cookie: {
    httpOnly: true,
    sameSite: 'lax',
    secure: false
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
