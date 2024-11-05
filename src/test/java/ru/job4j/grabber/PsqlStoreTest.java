package ru.job4j.grabber;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.*;

class PsqlStoreTest {

    private static Connection connection;

    private final String link = "https://career.habr.com/vacancies?page=1&q=Java+developer&type=all";

    @BeforeAll
    public static void initConnection() {
        try (InputStream in = PsqlStore.class.getClassLoader().getResourceAsStream("db/liquibase_test.properties")) {
            Properties config = new Properties();
            config.load(in);
            Class.forName(config.getProperty("driver-class-name"));
            connection = DriverManager.getConnection(
                    config.getProperty("url"),
                    config.getProperty("username"),
                    config.getProperty("password")

            );
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @AfterAll
    public static void closeConnection() throws SQLException {
        connection.close();
    }

    @AfterEach
    public void wipeTable() throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("delete from grabber.post")) {
            statement.execute();
        }
    }

    @Test
    public void whenSavePost() throws IOException {
        HabrCareerParse habrCareerParse = new HabrCareerParse(new HabrCareerDateTimeParser());
        List<Post> posts = habrCareerParse.list(link);
        PsqlStore psqlStore = new PsqlStore(connection);
        psqlStore.save(posts.get(1));
        String expected = posts.get(1).getLink();
        try (Statement statement =
                     connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("SELECT link FROM grabber.post WHERE id = 1");
            while (resultSet.next()) {
                String result = resultSet.getString(1);
                assertThat(result).isEqualTo(expected);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void whenGetAllPosts() throws IOException {
        HabrCareerParse habrCareerParse = new HabrCareerParse(new HabrCareerDateTimeParser());
        List<Post> posts = habrCareerParse.list(link);
        PsqlStore psqlStore = new PsqlStore(connection);
        posts.forEach(psqlStore::save);
        int result = posts.size();
        try (Statement statement =
                     connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM grabber.post");
            while (resultSet.next()) {
                int expected = resultSet.getInt(1);
                assertThat(result).isEqualTo(expected);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void whenFindByIdPost() throws IOException {
        HabrCareerParse habrCareerParse = new HabrCareerParse(new HabrCareerDateTimeParser());
        List<Post> posts = habrCareerParse.list(link);
        PsqlStore psqlStore = new PsqlStore(connection);
        posts.forEach(psqlStore::save);
        Post result = psqlStore.findById(10);
        try (Statement statement =
                     connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("SELECT * FROM grabber.post where id = 10");
            while (resultSet.next()) {
                Post expected = new Post();
                expected.setId(resultSet.getInt(1));
                expected.setTitle(resultSet.getString(2));
                expected.setDescription(resultSet.getString(3));
                expected.setLink(resultSet.getString(4));
                expected.setCreated(resultSet.getTimestamp(4).toLocalDateTime());
                assertThat(result).isEqualTo(expected);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}