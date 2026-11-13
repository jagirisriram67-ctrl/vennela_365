---
name: room-database-migrations
description: Production Room database migration strategies, schema export, automated/manual migrations, and test validation using MigrationTestHelper. Prevents data loss and startup crashes across app updates.
---

# Room Database Migrations & Schema Safety

This skill guides the safe evolution of Room database schemas, zero-data-loss migrations, and automated migration tests.

---

## 1. Schema Export Configuration in Gradle

Always export schema JSON files to enable Room's compile-time migration validation:

```kotlin
// app/build.gradle.kts
android {
    defaultConfig {
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }
    sourceSets {
        getByName("androidTest").assets.srcDirs("$projectDir/schemas")
    }
}
```

---

## 2. Automated Migrations (AutoMigration)

For simple schema additions (adding non-null columns with default values, adding new tables):

```kotlin
@Database(
    entities = [FileEntity::class, CategoryEntity::class],
    version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ],
    exportSchema = true
)
abstract class VennelaDatabase : RoomDatabase() {
    abstract fun fileDao(): FileDao
    abstract fun categoryDao(): CategoryDao
}
```

If renaming or deleting columns/tables, provide an `@DeleteColumn` or `@RenameColumn` spec:

```kotlin
@RenameColumn(tableName = "files", fromColumnName = "path", toColumnName = "uri_string")
class Migration1To2Spec : AutoMigrationSpec
```

---

## 3. Manual SQL Migrations

For complex transformations (data backfilling, type alterations, table splits):

```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Step 1: Create new table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `tags` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `name` TEXT NOT NULL,
                `colorHex` TEXT NOT NULL
            )
        """.trimIndent())

        // Step 2: Add foreign key reference or index
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_files_tag_id` ON `files` (`tagId`)")
    }
}
```

Attach to Room database builder:
```kotlin
Room.databaseBuilder(context, VennelaDatabase::class.java, "vennela.db")
    .addMigrations(MIGRATION_2_3)
    .fallbackToDestructiveMigrationOnDowngrade() // Only permit wipe on downgrades (debug scenarios), NEVER in production upgrades
    .build()
```

---

## 4. Automated Migration Verification Test

Use `MigrationTestHelper` in instrumented tests to verify migration integrity against exported schema JSON:

```kotlin
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        VennelaDatabase::class.java
    )

    @Test
    fun migrate1To2_containsAllData() {
        // 1. Create Database at version 1 and seed data
        var db = helper.createDatabase(TEST_DB, 1).apply {
            execSQL("INSERT INTO files (id, name, size) VALUES ('f1', 'test.pdf', 1024)")
            close()
        }

        // 2. Run migration to version 2
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true)

        // 3. Verify data persisted
        val cursor = db.query("SELECT * FROM files WHERE id = 'f1'")
        assertThat(cursor.moveToFirst()).isTrue()
        assertThat(cursor.getString(cursor.getColumnIndexOrThrow("name"))).isEqualTo("test.pdf")
        cursor.close()
    }
}
```

