const path = require('path');
const express = require('express');
const helmet = require('helmet');
const cookieParser = require('cookie-parser');
const session = require('express-session');
const dotenv = require('dotenv');
const userRoutes = require('./src/userRoutes');

dotenv.config();

const app = express();
const PORT = process.env.PORT || 3000;

app.use(helmet());
app.use(express.json());
app.use(express.urlencoded({ extended: false }));
app.use(cookieParser());
app.use(
  session({
    secret: process.env.SESSION_SECRET || 'borrowit-default-secret',
    resave: false,
    saveUninitialized: false,
    cookie: {
      httpOnly: true,
      sameSite: 'lax',
      secure: false
    }
  })
);

app.use('/api', userRoutes);

app.get('/admin.html', (req, res, next) => {
  if (!req.session?.user) {
    return res.redirect('/login.html');
  }
  if (req.session.user.role !== 'ADMIN') {
    return res.redirect('/login.html');
  }
  return next();
});

app.use(express.static(path.join(__dirname, 'public')));

app.get('/register.html', (req, res) => {
  return res.redirect(301, '/login.html');
});

app.get('*', (req, res) => {
  if (path.extname(req.path)) {
    return res.status(404).send('Not Found');
  }
  return res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

app.listen(PORT, () => {
  console.log(`BorrowIT user portal listening on http://localhost:${PORT}`);
});
