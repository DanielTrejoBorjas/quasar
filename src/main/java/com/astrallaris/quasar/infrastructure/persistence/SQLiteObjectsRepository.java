package com.astrallaris.quasar.infrastructure.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.astrallaris.quasar.domain.entities.StoredObject;
import com.astrallaris.quasar.domain.repositories.ObjectsRepository;

@Repository
public class SQLiteObjectsRepository implements ObjectsRepository {

    private static final String CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS objects (
                id TEXT PRIMARY KEY,
                bucket_id TEXT NOT NULL,
                key TEXT NOT NULL,
                hash TEXT NOT NULL,
                size INTEGER NOT NULL,
                version INTEGER NOT NULL,
                create_at TEXT NOT NULL,
                update_at TEXT NOT NULL,
                UNIQUE (bucket_id, key)
            )
            """;

    private final String jdbcUrl;

    public SQLiteObjectsRepository(@Value("${quasar.database.url:jdbc:sqlite:quasar.db}") String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
        initializeSchema();
    }

    @Override
    public void save(StoredObject object) {
        String sql = """
                INSERT INTO objects (id, bucket_id, key, hash, size, version, create_at, update_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    bucket_id = excluded.bucket_id,
                    key = excluded.key,
                    hash = excluded.hash,
                    size = excluded.size,
                    version = excluded.version,
                    update_at = excluded.update_at
                """;
        try (Connection connection = openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, object.id().toString());
            statement.setString(2, object.bucket_id().toString());
            statement.setString(3, object.key());
            statement.setString(4, object.hash());
            statement.setLong(5, object.size());
            statement.setInt(6, object.version());
            statement.setString(7, object.createAt().toString());
            statement.setString(8, object.updateAt().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to save object " + object.bucket_id() + "/" + object.key(), e);
        }
    }

    @Override
    public StoredObject findById(UUID id) {
        return queryOne("SELECT * FROM objects WHERE id = ?", id.toString());
    }

    @Override
    public List<StoredObject> findByBucket(UUID bucketId) {
        return query("SELECT * FROM objects WHERE bucket_id = ? ORDER BY key", bucketId.toString());
    }

    @Override
    public StoredObject findByBucketAndKey(UUID bucketId, String key) {
        return queryOne("SELECT * FROM objects WHERE bucket_id = ? AND key = ?", bucketId.toString(), key);
    }

    @Override
    public void deleteById(UUID id) {
        try (Connection connection = openConnection();
                PreparedStatement statement = connection.prepareStatement("DELETE FROM objects WHERE id = ?")) {
            statement.setString(1, id.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete object " + id, e);
        }
    }

    private StoredObject queryOne(String sql, String... parameters) {
        List<StoredObject> objects = query(sql, parameters);
        return objects.isEmpty() ? null : objects.get(0);
    }

    private List<StoredObject> query(String sql, String... parameters) {
        List<StoredObject> objects = new ArrayList<>();
        try (Connection connection = openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setString(i + 1, parameters[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    objects.add(toStoredObject(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query objects", e);
        }
        return objects;
    }

    private StoredObject toStoredObject(ResultSet resultSet) throws SQLException {
        return new StoredObject(
                UUID.fromString(resultSet.getString("id")),
                UUID.fromString(resultSet.getString("bucket_id")),
                resultSet.getString("key"),
                resultSet.getString("hash"),
                resultSet.getLong("size"),
                resultSet.getInt("version"),
                Instant.parse(resultSet.getString("create_at")),
                Instant.parse(resultSet.getString("update_at")));
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    private void initializeSchema() {
        try (Connection connection = openConnection();
                Statement statement = connection.createStatement()) {
            statement.execute(CREATE_TABLE);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize objects schema", e);
        }
    }
}
