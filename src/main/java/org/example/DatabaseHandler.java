package org.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler {
    private Connection conn;

    public DatabaseHandler() {
        try {
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection(
                    "jdbc:postgresql://dpg-d3g9cpvfte5s73bu8hgg-a.oregon-postgres.render.com:5432/kurs_nmvh",
                    "kurs_nmvh_user",
                    "6HhKdcXAWDUcOAgwGtyqoLS52X1LJnvv"
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public User getUser(Long chatId) throws SQLException {
        String sql = "SELECT * FROM users WHERE chat_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, chatId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getLong("chat_id"),
                        rs.getString("full_name"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        rs.getString("state"),
                        rs.getString("course"),
                        rs.getString("course_time")
                );
            }
        }
        return null;
    }

    public void saveUser(User user) throws SQLException {
        String sql = """
            INSERT INTO users (chat_id, full_name, phone, address, state, course, course_time)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (chat_id) DO UPDATE SET
                full_name = EXCLUDED.full_name,
                phone = EXCLUDED.phone,
                address = EXCLUDED.address,
                state = EXCLUDED.state,
                course = EXCLUDED.course,
                course_time = EXCLUDED.course_time""";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, user.getChatId());
            stmt.setString(2, user.getFullName());
            stmt.setString(3, user.getPhone());
            stmt.setString(4, user.getAddress());
            stmt.setString(5, user.getState());
            stmt.setString(6, user.getCourse());
            stmt.setString(7, user.getCourseTime());
            stmt.executeUpdate();
        }
    }

    public List<Course> getAllCourses() throws SQLException {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT * FROM courses";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                courses.add(new Course(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("duration"),
                        rs.getString("price"),
                        rs.getString("description"),
                        rs.getString("details_url")
                ));
            }
        }
        return courses;
    }

    public Course getCourseById(int id) throws SQLException {
        String sql = "SELECT * FROM courses WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Course(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("duration"),
                        rs.getString("price"),
                        rs.getString("description"),
                        rs.getString("details_url")
                );
            }
        }
        return null;
    }

    public List<String> getCourseTimes(int courseId) throws SQLException {
        List<String> times = new ArrayList<>();
        times.add("Ertalab: 8-12");
        times.add("Tushdan keyin: 12-20");
        return times;
    }

    public List<String> getAllBranches() throws SQLException {
        List<String> branches = new ArrayList<>();
        String sql = "SELECT name FROM branches";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                branches.add(rs.getString("name"));
            }
        }
        return branches;
    }

    public void saveRegistration(User user, int courseId) throws SQLException {
        String sql = """
            INSERT INTO registrations (user_id, course_id, full_name, phone, address, course_time, reg_date)
            VALUES (?, ?, ?, ?, ?, ?, NOW())""";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, user.getChatId());
            stmt.setInt(2, courseId);
            stmt.setString(3, user.getFullName());
            stmt.setString(4, user.getPhone());
            stmt.setString(5, user.getAddress());
            stmt.setString(6, user.getCourseTime());
            stmt.executeUpdate();
        }
    }



    public void close() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}