# BorrowIT

BorrowIT is being modernized into a hybrid equipment borrowing system:

- **BorrowIT Admin**: standalone JavaFX desktop application for staff operations.
- **BorrowIT Borrower Web Portal**: responsive student portal in `web-portal`.
- **Shared MySQL Database**: one source of truth for users, equipment, reservations, overdues, notifications, and audit data.

The web portal is borrower/student only. Admin and staff workflows remain exclusively in the JavaFX desktop application.

## Architecture

```text
Student Web Portal
  -> REST API
  -> Shared MySQL Database
  <- JavaFX Admin Desktop Application
```

The JavaFX admin application does not depend on the web portal.

## Run The JavaFX Admin Application

```bash
mvn javafx:run
```

If Maven does not recognize the `javafx` prefix:

```bash
mvn org.openjfx:javafx-maven-plugin:0.0.8:run
```

Or:

```bash
mvn -Dexec.mainClass=com.borrowit.Main exec:java
```

## Run The Borrower Web Portal

```bash
cd web-portal
npm install
npm start
```

Open `http://localhost:3000`.

## Database

Default local configuration:

- Host: `localhost`
- Port: `3306`
- Database: `borrowit`
- User: `root`
- Password: empty

JavaFX configuration is in `src/main/resources/borrowit-db.properties`.

Fresh setup:

```sql
SOURCE database/borrowit_schema.sql;
SOURCE database/borrowit_seed.sql;
SOURCE database/borrowit_sample_data.sql;
```

Existing database modernization:

```sql
SOURCE database/borrowit_modernization_migration.sql;
```

## Documentation

- Modernization blueprint: `MODERNIZATION_BLUEPRINT.md`
- Web API contracts: `web-portal/API-CONTRACTS.md`
- Web portal notes: `web-portal/README.md`

## Default Test Credentials

- Admin account: username `admin`, password `Admin@123`
- Student account: username `202511319`, password `GCbalan`

## Packaged JAR Note

If running from a packaged JAR, use the platform-specific JavaFX module path:

```powershell
java --module-path "C:\path\to\javafx-sdk-20.0.2\lib" --add-modules javafx.controls,javafx.fxml -jar target/borrowit-1.0.0.jar
```
