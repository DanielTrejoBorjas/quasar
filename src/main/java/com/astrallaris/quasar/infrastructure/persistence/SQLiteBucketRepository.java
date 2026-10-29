package com.astrallaris.quasar.infrastructure.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.astrallaris.quasar.domain.entities.Bucket;
import com.astrallaris.quasar.domain.repositories.BucketRepository;

@Repository
public class SQLiteBucketRepository implements BucketRepository {

    private static final String CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS buckets (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL UNIQUE
            )
            """;

    private final String jdbcUrl;

    public SQLiteBucketRepository(@Value("${quasar.database.url:jdbc:sqlite:quasar.db}") String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
        initializeSchema();
    }

    @Override
    public Bucket findByName(String name) {
        return queryOne("SELECT id, name FROM buckets WHERE name = ?", name);
    }

    @Override
    public Bucket findById(UUID id) {
        return queryOne("SELECT id, name FROM buckets WHERE id = ?", id.toString());
    }

    @Override
    public List<Bucket> findAll() {
        List<Bucket> buckets = new ArrayList<>();
        try (Connection connection = openConnection();
                PreparedStatement statement = connection.prepareStatement("SELECT id, name FROM buckets ORDER BY name");
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                buckets.add(toBucket(resultSet));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load buckets", e);
        }
        return buckets;
    }

    @Override
    public void save(Bucket bucket) {
        try (Connection connection = openConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO buckets (id, name) VALUES (?, ?) ON CONFLICT(id) DO UPDATE SET name = excluded.name")) {
            statement.setString(1, bucket.id().toString());
            statement.setString(2, bucket.name());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save bucket " + bucket.id(), e);
        }
    }

    @Override
    public void delete(Bucket bucket) {
        try (Connection connection = openConnection();
                PreparedStatement statement = connection.prepareStatement("DELETE FROM buckets WHERE id = ?")) {
            statement.setString(1, bucket.id().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete bucket " + bucket.id(), e);
        }
    }

    private Bucket queryOne(String sql, String parameter) {
        try (Connection connection = openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, parameter);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? toBucket(resultSet) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query bucket", e);
        }
    }

    private Bucket toBucket(ResultSet resultSet) throws SQLException {
        return new Bucket(UUID.fromString(resultSet.getString("id")), resultSet.getString("name"));
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    private void initializeSchema() {
        try (Connection connection = openConnection();
                Statement statement = connection.createStatement()) {
            statement.execute(CREATE_TABLE);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize buckets schema", e);
        }
    }
}
