# BorrowIT

BorrowIT is a JavaFX and MySQL Equipment System.

It includes two interfaces:

- User Application for borrower login, equipment viewing, reservation requests, current borrowed items, status tracking, cancellation, and history.
- Admin/Staff Application for login, dashboard, equipment management, reservation approval/decline, return processing, overdue tracking, and user/equipment search.

## What's New

- Added separate admin and borrower workflows in a single JavaFX application.
- Improved reservation tracking with request approval, decline, and return processing.
- Added overdue item monitoring and status updates for both borrowers and staff.
- Added equipment search, user management, and admin dashboard tools for staff.
- Improved authentication flow with distinct login paths for admin and user roles.

## Running the Application

This project uses JavaFX and requires the JavaFX runtime at launch time. Use the Maven JavaFX plugin to run the app:

```bash
mvn javafx:run
```

### Run as Admin or User

The same application launch command starts both interfaces. After startup, choose the appropriate login type:

- `Admin`: Use the admin/staff login screen to access the dashboard, equipment management, reservation approvals, returns, overdue tracking, and search tools.
- `User`: Use the borrower login screen to view available equipment, request reservations, track current loans, cancel requests, and view history.

The role is determined by the credentials used at login. Make sure the database has the corresponding admin and user accounts seeded before login.

If your Maven installation does not recognize the `javafx` prefix, run the plugin by full coordinate instead:

```bash
mvn org.openjfx:javafx-maven-plugin:0.0.8:run
```

If you want to use `exec:java`, the correct main class is `com.borrowit.Main`:

```bash
mvn -Dexec.mainClass=com.borrowit.Main exec:java
```

If you want to run from a packaged JAR, use the platform-specific JavaFX module path instead of `java -jar`:

```powershell
java --module-path "C:\path\to\javafx-sdk-20.0.2\lib" --add-modules javafx.controls,javafx.fxml -jar target/borrowit-1.0.0.jar
```

Running `java -jar` directly without JavaFX modules will produce the error: `JavaFX runtime components are missing, and are required to run this application`.
