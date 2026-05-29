# BorrowIT

BorrowIT is a JavaFX and MySQL Equipment System.

It includes two interfaces:

- User Application for borrower login, equipment viewing, reservation requests, current borrowed items, status tracking, cancellation, and history.
- Admin/Staff Application for login, dashboard, equipment management, reservation approval/decline, return processing, overdue tracking, and user/equipment search.

## Running the Application

This project uses JavaFX and requires the JavaFX runtime at launch time. Use the Maven JavaFX plugin to run the app:

```bash
mvn javafx:run
```

### Run as Admin or User

The same application launch command starts both interfaces. After startup, choose the appropriate login type:

- `Admin`: Use the admin/staff login screen to access the dashboard, equipment management, reservation approvals, returns, overdue tracking, and search tools.
- `User`: Use the borrower login screen to view available equipment, request reservations, track current loans, cancel requests, and view history.
 - `User`: Use the borrower login screen to view available equipment, request reservations, track current borrowings, cancel requests, and view history.

The role is determined by the credentials used at login. Make sure the database has the corresponding admin and user accounts seeded before login.

If your Maven installation does not recognize the `javafx` prefix, run the plugin by full coordinate instead:

```bash
mvn org.openjfx:javafx-maven-plugin:0.0.8:run
```

If you want to use `exec:java`, the correct main class is `com.borrowit.Main`:

```bash
mvn -Dexec.mainClass=com.borrowit.Main exec:java
```

## Web Portal

If you want to run the web portal instead of the desktop app, use the portal folder:

```bash
cd web-portal
npm install
npm start
```

### Accessing the Web Portal

Once the server is running (default: `http://localhost:3000`), you can access both portals:

**User Portal (Borrowers)**
- URL: `http://localhost:3000/login.html` or `http://localhost:3000`
- Login with user/student credentials
- Features: Browse equipment, request reservations, track loans, view history

**Admin Portal**
- URL: `http://localhost:3000/admin-login.html`
- Login with admin credentials
- Features: Equipment management, reservation approvals, user management, reports

### Default Test Credentials

- **Admin Account**: Username: `admin` | Password: `Admin@123`
- **Student Account**: Username: `202511319` | Password: `GCbalan`

**Note**: Users are automatically redirected based on their role. If an admin tries to use the user login page, they will be prompted to use the admin login page instead.

If you want to run from a packaged JAR, the correct platform-specific JavaFX module path is:

```bash
mvn -Dexec.mainClass=com.borrowit.Main exec:java
```

If you want to run from a packaged JAR, use the platform-specific JavaFX module path instead of `java -jar`:

```powershell
java --module-path "C:\path\to\javafx-sdk-20.0.2\lib" --add-modules javafx.controls,javafx.fxml -jar target/borrowit-1.0.0.jar
```

Running `java -jar` directly without JavaFX modules will produce the error: `JavaFX runtime components are missing, and are required to run this application`.
